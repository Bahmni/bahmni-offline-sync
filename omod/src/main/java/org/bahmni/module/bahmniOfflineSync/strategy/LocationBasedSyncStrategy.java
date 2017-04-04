package org.bahmni.module.bahmniOfflineSync.strategy;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.*;

public class LocationBasedSyncStrategy extends AbstractOfflineSyncStrategy {
    private static final String ATTRIBUTE_TYPE_NAME = "addressCode";


    public LocationBasedSyncStrategy() throws SQLException {
        super();

    }

     protected String evaluateFilterForPatient(String uuid) {
        String patientFilter = null;
        Patient patient = patientService.getPatientByUuid(uuid);

        if (patient != null && patient.getAttribute(ATTRIBUTE_TYPE_NAME) != null) {
            patientFilter = patient.getAttribute(ATTRIBUTE_TYPE_NAME).getValue();
        }

        return patientFilter;
    }

    private String evaluateFilterForEncounter(String uuid) {
        String filter = null;
        Encounter encounter = encounterService.getEncounterByUuid(uuid);
        if (encounter != null)
            filter = evaluateFilterForPatient(encounter.getPatient().getUuid());
        return filter;
    }

    private String evaluateFilterForAddressHierarchy(String uuid) {
        String addressHierarchyFilter = null;
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(uuid);
        if (addressHierarchyEntry != null && addressHierarchyEntry.getLevel() != null && addressHierarchyEntry.getLevel().getId() > 3) {
            addressHierarchyFilter = addressHierarchyEntry.getUserGeneratedId();
        }
        return addressHierarchyFilter;
    }


    public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
        Map<String, List<String>> categoryFilterMap = new HashMap();
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(addressUuid);
        List transactionalDataFilters = getTransactionalDataFilters(loginLocationUuid, addressHierarchyService, addressHierarchyEntry);
        categoryFilterMap.put("patient", transactionalDataFilters);
        categoryFilterMap.put("encounter", transactionalDataFilters);
        categoryFilterMap.put("addressHierarchy", getFiltersForAddressHierarchy(addressHierarchyEntry));
        categoryFilterMap.put("parentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        return categoryFilterMap;
    }

    private List getTransactionalDataFilters(String loginLocationUuid, AddressHierarchyService addressHierarchyService, AddressHierarchyEntry addressHierarchyEntry) {
        List transactionalDataFilters = new ArrayList();
        if (addressHierarchyEntry != null) {
            LocationAttributeType catchmentFiltersAttribute = locationService.getLocationAttributeTypeByName("catchmentFilters");
            String userGeneratedId = addressHierarchyEntry.getUserGeneratedId();
            LocationAttribute catchmentFilters = getCatchmentFilters(loginLocationUuid, catchmentFiltersAttribute);
            List<AddressHierarchyEntry> childAddressHierarchyEntries = addressHierarchyService.getChildAddressHierarchyEntries(addressHierarchyEntry);
            List<String> transactionalFilters = getCatchmentIds(catchmentFilters, childAddressHierarchyEntries, addressHierarchyService, addressHierarchyEntry);
            transactionalDataFilters.add(userGeneratedId);
            transactionalDataFilters.addAll(transactionalFilters);
        }
        return transactionalDataFilters;
    }

    private LocationAttribute getCatchmentFilters(String loginLocationUuid, LocationAttributeType catchmentFiltersAttribute) {
        Location location = locationService.getLocationByUuid(loginLocationUuid);
        List<LocationAttribute> attributes = (List<LocationAttribute>) location.getActiveAttributes();
        for (LocationAttribute attribute : attributes) {
            if (attribute.getAttributeType().equals(catchmentFiltersAttribute)) {
                return attribute;
            }
        }
        return null;
    }

    private List<String> getCatchmentIds(LocationAttribute catchmentFilters, List<AddressHierarchyEntry> childAddressHierarchyEntries, AddressHierarchyService addressHierarchyService, AddressHierarchyEntry addressHierarchyEntry) {
        List<String> wardIDs = new ArrayList();
        if (catchmentFilters != null) {
            String wardsName = trim(catchmentFilters.getValue().toString());
            Set<String> wardsNameList = StringUtils.commaDelimitedListToSet(wardsName);
            for (String wardName : wardsNameList) {
                AddressHierarchyEntry childAddressHierarchyEntry = addressHierarchyService.getChildAddressHierarchyEntryByName(addressHierarchyEntry, wardName);
                if (childAddressHierarchyEntry == null) {
                    throw new RuntimeException("Please check your catchmentFilters configuration in openmrs!!");
                } else {
                    getAllWardIds(childAddressHierarchyEntry, addressHierarchyService, wardIDs);
                }
            }
        } else {
            updateWardIds(addressHierarchyService, wardIDs, childAddressHierarchyEntries);
        }
        return wardIDs;
    }

    private void getAllWardIds(AddressHierarchyEntry addressHierarchyEntry, AddressHierarchyService addressHierarchyService, List<String> wardIDs) {
        if (addressHierarchyEntry == null) {
            return;
        }
        wardIDs.add(addressHierarchyEntry.getUserGeneratedId());
        List<AddressHierarchyEntry> childAddressHierarchyEntries = addressHierarchyService.getChildAddressHierarchyEntries(addressHierarchyEntry);
        updateWardIds(addressHierarchyService, wardIDs, childAddressHierarchyEntries);
    }

    private void updateWardIds(AddressHierarchyService addressHierarchyService, List<String> wardIDs, List<AddressHierarchyEntry> childAddressHierarchyEntries) {
        for (AddressHierarchyEntry childAddressHierarchyEntry : childAddressHierarchyEntries) {
            getAllWardIds(childAddressHierarchyEntry, addressHierarchyService, wardIDs);
        }
    }

    private String trim(String content) {
        content = content.trim();
        return content.replaceAll("(\\s*,\\s*)", ",");
    }

    @Override
    public List<String> getEventCategoriesList() {
        List<String> eventCategoryList = new ArrayList();

        eventCategoryList.add("patient");
        eventCategoryList.add("encounter");
        eventCategoryList.add("addressHierarchy");
        eventCategoryList.add("parentAddressHierarchy");
        eventCategoryList.add("offline-concepts");

        return eventCategoryList;
    }



    private List<String> getFiltersForAddressHierarchy(AddressHierarchyEntry addressHierarchyEntry) {
        List addressHierarchyFilters = new ArrayList();
        while (addressHierarchyEntry.getParent() != null) {
            if (addressHierarchyEntry.getUserGeneratedId().length() == 6) {
                addressHierarchyFilters.add(addressHierarchyEntry.getUserGeneratedId());
                break;
            }
            addressHierarchyEntry = addressHierarchyEntry.getParent();
        }
        return addressHierarchyFilters;
    }

    @Override
    public List<EventLog> getEventLogsFromEventRecords(List<EventRecord> eventRecords) {
        List<EventLog> eventLogs = new ArrayList<EventLog>();

        for (EventRecord er : eventRecords) {
            EventLog eventLog = new EventLog(er.getUuid(),er.getCategory(),er.getTimeStamp(),er.getContents(), er.getUuid(), er.getUuid());
            String category = er.getCategory();
            String uuid = getUuidFromURL(er.getContents());
            String filter = null;

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
                else if (category.equalsIgnoreCase("Encounter") || category.equalsIgnoreCase("SHREncounter"))
                    filter = evaluateFilterForEncounter(uuid);
                else if (category.equalsIgnoreCase("addressHierarchy"))
                    filter = evaluateFilterForAddressHierarchy(uuid);
            }
            eventLog.setFilter(filter);

            eventLogs.add(eventLog);
        }

        return eventLogs;
    }



    private boolean isOfflineConceptEvent(String eventUuid) {
        final Concept concept = conceptService.getConceptByUuid(eventUuid);
        final Concept offlineConcept = conceptService.getConceptByName("Offline Concepts");
        return offlineConcept != null && offlineConcept.getSetMembers().contains(concept);
    }
}
