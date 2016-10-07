package org.bahmni.module.bahmniOfflineSync.strategy;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleLocationBasedOfflineSyncStrategy implements OfflineSyncStrategy {

    private static final String ATTRIBUTE_TYPE_NAME = "addressCode";

    private PatientService patientService;

    private LocationService locationService;

    private EncounterService encounterService;

    private ConceptService conceptService;

    public SimpleLocationBasedOfflineSyncStrategy() {
        this.patientService = Context.getPatientService();
        this.encounterService = Context.getEncounterService();
        this.locationService = Context.getLocationService();
        this.conceptService = Context.getConceptService();
    }

    private String evaluateFilterForPatient(String uuid) {
        String patientFilter = "";
        Patient patient = patientService.getPatientByUuid(uuid);

        if (patient != null && patient.getAttribute(ATTRIBUTE_TYPE_NAME) != null) {
            patientFilter = patient.getAttribute(ATTRIBUTE_TYPE_NAME).getValue();
        }

        return patientFilter;
    }

    private String evaluateFilterForEncounter(String uuid) {
        String filter = "";
        Encounter encounter = encounterService.getEncounterByUuid(uuid);
        if (encounter != null)
            filter = evaluateFilterForPatient(encounter.getPatient().getUuid());
        return filter;
    }

    private String evaluateFilterForAddressHierarchy(String uuid) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(uuid);
        return addressHierarchyEntry.getUserGeneratedId();
    }


    public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
        Map<String, List<String>> categoryFilterMap = new HashMap();
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(addressUuid);
        List transactionalDataFilters = getTransactionalDataFilters(addressHierarchyService, addressHierarchyEntry);
        categoryFilterMap.put("TransactionalData", transactionalDataFilters);
        categoryFilterMap.put("AddressHierarchy", getFilters(addressHierarchyEntry));
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        return categoryFilterMap;
    }

    private List getTransactionalDataFilters(AddressHierarchyService addressHierarchyService, AddressHierarchyEntry addressHierarchyEntry) {
        List transactionalDataFilters = new ArrayList();
        if (addressHierarchyEntry != null) {
            String userGeneratedId = addressHierarchyEntry.getUserGeneratedId();
            List<AddressHierarchyEntry> childAddressHierarchyEntries = addressHierarchyService.getChildAddressHierarchyEntries(addressHierarchyEntry);
            List<String> transactionalFilters = getCatchmentIds(childAddressHierarchyEntries, addressHierarchyService, addressHierarchyEntry);
            transactionalDataFilters.add(userGeneratedId);
            transactionalDataFilters.addAll(transactionalFilters);
        }
        return transactionalDataFilters;
    }

    private List<String> getCatchmentIds(List<AddressHierarchyEntry> childAddressHierarchyEntries, AddressHierarchyService addressHierarchyService, AddressHierarchyEntry addressHierarchyEntry) {
        List<String> wardIDs = new ArrayList();
            updateWardIds(addressHierarchyService, wardIDs, childAddressHierarchyEntries);
        return wardIDs;
    }


    private void updateWardIds(AddressHierarchyService addressHierarchyService, List<String> wardIDs, List<AddressHierarchyEntry> childAddressHierarchyEntries) {
        for (AddressHierarchyEntry childAddressHierarchyEntry : childAddressHierarchyEntries) {
            getAllWardIds(childAddressHierarchyEntry, addressHierarchyService, wardIDs);
        }
    }

    private void getAllWardIds(AddressHierarchyEntry addressHierarchyEntry, AddressHierarchyService addressHierarchyService, List<String> wardIDs) {
        if (addressHierarchyEntry == null) {
            return;
        }
        wardIDs.add(addressHierarchyEntry.getUserGeneratedId());
        List<AddressHierarchyEntry> childAddressHierarchyEntries = addressHierarchyService.getChildAddressHierarchyEntries(addressHierarchyEntry);
        updateWardIds(addressHierarchyService, wardIDs, childAddressHierarchyEntries);
    }

    private List getFilters(AddressHierarchyEntry addressHierarchyEntry) {
        List transactionalDataFilters = new ArrayList();
        if (addressHierarchyEntry != null) {
            String userGeneratedId = addressHierarchyEntry.getUserGeneratedId();
            transactionalDataFilters.add(userGeneratedId);
        }
        return transactionalDataFilters;
    }

    @Override
    public List<String> getEventCategoriesList() {
        List<String> eventCategoryList = new ArrayList();
        eventCategoryList.add("TransactionalData");
        eventCategoryList.add("AddressHierarchy");
        eventCategoryList.add("offline-concepts");
        return eventCategoryList;
    }

    @Override
    public List<EventLog> getEventLogsFromEventRecords(List<EventRecord> eventRecords) {
        List<EventLog> eventLogs = new ArrayList<EventLog>();

        for (EventRecord er : eventRecords) {
            EventLog eventLog = new EventLog(er.getUuid(), er.getCategory(), er.getTimeStamp(), er.getContents(), null);
            String category = er.getCategory();
            String uuid = getUuidFromURL(er.getContents());
            String filter = "";

            if (!uuid.isEmpty()) {
                if (category.equalsIgnoreCase("all-concepts")) {
                    if (isOfflineConceptEvent(uuid)) {
                        eventLog.setCategory("offline-concepts");
                    } else {
                        eventLog.setCategory("concepts");
                    }
                }

                if (category.equalsIgnoreCase("Patient")|| category.equalsIgnoreCase("LabOrderResults"))
                    filter = evaluateFilterForPatient(uuid);
                else if (category.equals("Encounter"))
                    filter = evaluateFilterForEncounter(uuid);
                else if (category.equalsIgnoreCase("AddressHierarchy"))
                    filter = evaluateFilterForAddressHierarchy(uuid);
            }
            eventLog.setFilter(filter);

            eventLogs.add(eventLog);
        }

        return eventLogs;
    }

    private String getUuidFromURL(String url) {
        String uuid = "";
        Pattern uuidPattern = Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        Matcher matcher = uuidPattern.matcher(url);
        if (matcher.find())
            uuid = matcher.group(0);
        return uuid;
    }

    private boolean isOfflineConceptEvent(String eventUuid) {
        final Concept concept = conceptService.getConceptByUuid(eventUuid);
        final Concept offlineConcept = conceptService.getConceptByName("Offline Concepts");
        return offlineConcept != null && offlineConcept.getSetMembers().contains(concept);
    }

}
