package org.bahmni.module.bahmniOfflineSync.utils;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.openmrs.Patient;

public class SelectiveSyncStrategyHelper {

    public static EventLog setAddressHierarchy(Patient patient, EventLog eventLog, String lowestLevelUserGeneratedID)
    {
        if(patient.getPerson() != null && patient.getPerson().getPersonAddress() != null) {
            concatenateFilter1ToN(eventLog,lowestLevelUserGeneratedID );
        }
        return eventLog;
    }

    private static void concatenateFilter1ToN(EventLog eventLog, String lowestLevelUserGeneratedID) {
        String filter = eventLog.getFilter();
          filter += getFilterValuePostNullCheck(lowestLevelUserGeneratedID);
        eventLog.setFilter(filter);
    }

    private static String getFilterValuePostNullCheck(String filter)
    {
        return null == filter ? "" : "-" + filter ;
    }
}
