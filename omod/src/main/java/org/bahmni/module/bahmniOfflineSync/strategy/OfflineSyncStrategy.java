/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at https://www.bahmni.org/license/mplv2hd.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

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
