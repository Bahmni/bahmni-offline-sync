package org.bahmni.module.bahmniOfflineSync.strategy;


import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.server.domain.EventRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OfflineSyncStrategy {
    public List<EventLog> getEventLogsFromEventRecords(List<EventRecord> eventRecords);

    public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid);

    public List<String> getEventCategoriesList();

    List<EventLog> getEventsWithNewFilterFor(List<String> patientUuids) throws SQLException;

}
