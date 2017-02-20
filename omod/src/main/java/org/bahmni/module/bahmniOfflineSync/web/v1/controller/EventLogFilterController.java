package org.bahmni.module.bahmniOfflineSync.web.v1.controller;


import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.bahmni.module.bahmniOfflineSync.strategy.OfflineSyncStrategy;
import org.bahmni.module.bahmniOfflineSync.strategy.SyncStrategyLoader;
import org.bahmni.module.bahmniOfflineSync.utils.EventRecordServiceHelper;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/eventlog/filter")
public class EventLogFilterController extends BaseRestController {
    @Autowired
    SyncStrategyLoader syncStrategyLoader;

    @Autowired
    private EventRecordServiceHelper eventRecordServiceHelper;

    private OfflineSyncStrategy offlineSyncStrategy;


    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<EventLog> getEventLogs() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        offlineSyncStrategy = syncStrategyLoader.getFilterEvaluatorFromGlobalProperties();
        List<EventRecord> eventRecords = eventRecordServiceHelper.getTopEventRecordsOrderByIdAsc();
        if (offlineSyncStrategy != null) {
            return offlineSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        } else {
            throw new RuntimeException("Global Property bahmniOfflineSync.strategy is not configured in OpenMRS. Configure classpath for OfflineSyncStrategy");
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{uuid}")
    @ResponseBody
    public List<EventLog> getEventLogsAfter(@PathVariable("uuid") String lastReadUuid) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        offlineSyncStrategy = syncStrategyLoader.getFilterEvaluatorFromGlobalProperties();
        List<EventRecord> eventRecords = eventRecordServiceHelper.getEventRecordsAfterUuid(lastReadUuid);
        if (offlineSyncStrategy != null) {
            return offlineSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        } else {
            throw new RuntimeException("Global Property bahmniOfflineSync.strategy is not configured in OpenMRS. Configure classpath for OfflineSyncStrategy");
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "getEventsWithNewFilterForPatients",  params = {"eventRecordUuid"})
    @ResponseBody
    public List<EventLog> getEventsWithNewFilterForPatients(@RequestParam(value = "eventRecordUuid")List<String> eventRecordUuids) throws IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException {
        offlineSyncStrategy = syncStrategyLoader.getFilterEvaluatorFromGlobalProperties();
        if (offlineSyncStrategy != null) {
            return offlineSyncStrategy.getEventsWithNewFilterFor(eventRecordUuids);
        } else {
            throw new RuntimeException("Global Property bahmniOfflineSync.strategy is not configured in OpenMRS. Configure classpath for OfflineSyncStrategy");
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/markers/{providerUuid}/{addressUuid}/{loginLocationUuid}", produces = {"application/json"})
    @ResponseBody
    public Map<String, List<String>> getFilterForDevice(@PathVariable("providerUuid") String providerUuid, @PathVariable("addressUuid") String addressUuid, @PathVariable("loginLocationUuid") String loginLocationUuid) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        offlineSyncStrategy = syncStrategyLoader.getFilterEvaluatorFromGlobalProperties();

        if (offlineSyncStrategy != null) {
            return offlineSyncStrategy.getFilterForDevice(providerUuid, addressUuid, loginLocationUuid);
        } else
            throw new RuntimeException("Global Property bahmniOfflineSync.strategy is not configured in OpenMRS. Configure classpath for OfflineSyncStrategy");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/category")
    @ResponseBody
    public List<String> getCategoryList() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        offlineSyncStrategy = syncStrategyLoader.getFilterEvaluatorFromGlobalProperties();

        if (offlineSyncStrategy != null) {
            return offlineSyncStrategy.getEventCategoriesList();
        } else
            throw new RuntimeException("Global Property bahmniOfflineSync.strategy is not configured in OpenMRS. Configure classpath for OfflineSyncStrategy");
    }
}
