package org.bahmni.module.bahmniOfflineSync.strategy;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.service.IdentifierSourceService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDBasedSyncStrategy implements OfflineSyncStrategy {
    private LocationService locationService;

    private PatientService patientService;

    private EncounterService encounterService;

    private ConceptService conceptService;

    private IdentifierSourceService identifierSourceService;

    public IDBasedSyncStrategy() {
        this.patientService = Context.getPatientService();
        this.encounterService = Context.getEncounterService();
        this.locationService = Context.getLocationService();
        this.conceptService = Context.getConceptService();
        this.identifierSourceService = Context.getService(IdentifierSourceService.class);
    }

    private String evaluateFilterForPatient(String uuid) {
        final Patient patient = patientService.getPatientByUuid(uuid);

        if (patient != null) {
            final PatientIdentifier identifier = patient.getPatientIdentifier("Patient Identifier");
            final List<IdentifierSource> identifierSources = identifierSourceService.getAllIdentifierSources(false);

            for (IdentifierSource src : identifierSources) {
                if (identifier.getIdentifier().startsWith(src.getName())) {
                    return src.getName();
                }
            }
        }

        return null;
    }

    private String evaluateFilterForEncounter(String uuid) {
        String filter = "";
        Encounter encounter = encounterService.getEncounterByUuid(uuid);

        if (encounter != null)
            filter = evaluateFilterForPatient(encounter.getPatient().getUuid());

        return filter;
    }

    public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
        Location location = locationService.getLocationByUuid(loginLocationUuid);
        Map<String, List<String>> categoryFilterMap = new HashMap();
        final Collection<LocationAttribute> activeAttributes = location.getActiveAttributes();
        ArrayList<String> filters = new ArrayList<String>();

        for (LocationAttribute attr : activeAttributes) {
            if (attr.getAttributeType().getName().equals("IdentifierSourceName")) {
                filters.add(attr.getValue().toString());
            }
        }

        categoryFilterMap.put("TransactionalData", filters);
        categoryFilterMap.put("AddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());

        return categoryFilterMap;
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
        Pattern uuidPattern = Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");

        for (EventRecord er : eventRecords) {
            EventLog eventLog = new EventLog(er.getUuid(), er.getCategory(), er.getTimeStamp(), er.getContents(), null);
            String category = er.getCategory();
            Matcher matcher = uuidPattern.matcher(er.getContents());

            if (matcher.find()) {
                String uuid = matcher.group(0);
                if ((er.getCategory().equalsIgnoreCase("all-concepts"))) {
                    if (isOfflineConceptEvent(uuid)) {
                        eventLog.setCategory("offline-concepts");
                    } else {
                        eventLog.setCategory("concepts");
                    }
                }

                String filter = "";
                if (category.equalsIgnoreCase("Patient"))
                    filter = evaluateFilterForPatient(uuid);
                else if (category.equalsIgnoreCase("Encounter"))
                    filter = evaluateFilterForEncounter(uuid);
                eventLog.setFilter(filter);
            }

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
