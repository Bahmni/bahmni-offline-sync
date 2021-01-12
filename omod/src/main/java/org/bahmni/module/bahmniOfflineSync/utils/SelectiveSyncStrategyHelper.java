package org.bahmni.module.bahmniOfflineSync.utils;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.openmrs.Patient;

public class SelectiveSyncStrategyHelper {

    public static EventLog setAddressHierarchy(Patient patient, EventLog eventLog, String lowestLevelUserGeneratedID)
    {
        if(patient.getPerson() != null && patient.getPerson().getPersonAddress() != null) {
//            eventLog.setFilter1(patient.getPerson().getPersonAddress().getStateProvince());
//            eventLog.setFilter2(patient.getPerson().getPersonAddress().getCityVillage());
//            eventLog.setFilter3(patient.getPerson().getPersonAddress().getAddress2());
//            eventLog.setFilter4(patient.getPerson().getPersonAddress().getAddress3());
//            eventLog.setFilter5(patient.getPerson().getPersonAddress().getAddress4());
//            eventLog.setFilter6(patient.getPerson().getPersonAddress().getAddress5());
//            eventLog.setFilter7(patient.getPerson().getPersonAddress().getAddress6());
//            eventLog.setFilter8(patient.getPerson().getPersonAddress().getAddress7());
//            eventLog.setFilter9(patient.getPerson().getPersonAddress().getAddress8());
//            eventLog.setFilter10(patient.getPerson().getPersonAddress().getAddress9());
            concatenateFilter1ToN(eventLog,lowestLevelUserGeneratedID );
        }
        return eventLog;
    }

    private static void concatenateFilter1ToN(EventLog eventLog, String lowestLevelUserGeneratedID) {
        String filter = eventLog.getFilter();
//        filter += getFilterValuePostNullCheck(eventLog.getFilter1());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter2());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter3());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter4());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter5());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter6());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter7());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter8());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter9());
//        filter += getFilterValuePostNullCheck(eventLog.getFilter10());
          filter += getFilterValuePostNullCheck(lowestLevelUserGeneratedID);
        eventLog.setFilter(filter);
    }

    private static String getFilterValuePostNullCheck(String filter)
    {
        return null == filter ? "" : "-" + filter ;
    }
}
