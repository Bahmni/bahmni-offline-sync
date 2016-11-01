package org.bahmni.module.bahmniOfflineSync.strategy;


import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.bahmni.module.bahmniOfflineSync.utils.EventRecordServiceHelper;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractOfflineSyncStrategy implements OfflineSyncStrategy {
    protected LocationService locationService;

    protected PatientService patientService;

    protected EncounterService encounterService;

    protected ConceptService conceptService;

    protected EventRecordServiceHelper eventRecordServiceHelper;

    protected String encounterURL = "/openmrs/ws/rest/v1/bahmnicore/bahmniencounter/%s?includeAll=true";

    protected static final String labOrderResultsURL = "/openmrs/ws/rest/v1/bahmnicore/labOrderResults?patientUuid=%s";

    public AbstractOfflineSyncStrategy() {
        this.patientService = Context.getPatientService();
        this.encounterService = Context.getEncounterService();
        this.locationService = Context.getLocationService();
        this.conceptService = Context.getConceptService();
        eventRecordServiceHelper = new EventRecordServiceHelper();
    }

    @Override
    public List<EventLog> getEventsWithNewFilterFor(List<String> eventRecordUuids) throws SQLException {
        List<EventLog> eventLogs = new ArrayList<EventLog>();

        for (String uuid : eventRecordUuids){
            EventRecord eventRecord = eventRecordServiceHelper.findEventRecordByUuid(uuid);
            String patientUuid = getUuidFromURL(eventRecord.getContents());
            Patient patient = patientService.getPatientByUuid(patientUuid);
            List<Encounter> encountersList = encounterService.getEncountersByPatient(patient);
            for (Encounter encounter : encountersList){
                if(!encounter.getEncounterType().getName().equals("LAB_RESULT"))
                    eventLogs.add(new EventLog(UUID.randomUUID().toString(),"Encounter", new Date(),String.format(encounterURL,encounter.getUuid()), null, uuid) );
                else
                    eventLogs.add(new EventLog(UUID.randomUUID().toString(),"LabOrderResults", new Date(),String.format(labOrderResultsURL,patient.getUuid()), null, uuid));
            }
        }
        return eventLogs;
    }

    protected abstract String evaluateFilterForPatient(String patientUuid);


    protected String getUuidFromURL(String url){
        String uuid = "";
        Pattern uuidPattern = Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        Matcher matcher = uuidPattern.matcher(url);
        if (matcher.find())
            uuid = matcher.group(0);
        return uuid;
    }
}
