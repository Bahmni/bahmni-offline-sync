package org.bahmni.module.bahmniOfflineSync.web.v1.controller;


import com.google.gson.Gson;
import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.bahmni.module.bahmniOfflineSync.strategy.OfflineSyncStrategy;
import org.bahmni.module.bahmniOfflineSync.strategy.SyncStrategyLoader;
import org.bahmni.module.bahmniOfflineSync.utils.EventRecordServiceHelper;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

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


    @RequestMapping(method = RequestMethod.GET, value = "getEventsWithNewFilterForPatients", params = {"eventRecordUuid"})
    @ResponseBody
    public List<EventLog> getEventsWithNewFilterForPatients(@RequestParam(value = "eventRecordUuid") List<String> eventRecordUuids) throws IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException {
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

    @RequestMapping(method = RequestMethod.GET, value = "/suman")
    @ResponseBody
    public List<EventLog> getPatientsList() throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
        RequestContext requestContext = new RequestContext();
        List<EventLog> eventLogList = eventRecordServiceHelper.getDistinctEventLogsByCategory("patient", "object");
        Stream<EventLog> eventLogStream = eventLogList.stream();
        Map<String, List<String>> patientsUUid = eventLogStream.filter(p -> p.getFilter() != null).collect(Collectors.groupingBy(p -> p.getFilter(),
                Collectors.mapping(EventLog::getObject, Collectors.toList())));
        patientsUUid.forEach((k, v) -> {
            try {
                process(k, v, requestContext);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return null;
    }

    private ArrayList<Patient> process(String filter, List<String> patientUrls, RequestContext requestContext) throws IOException {
        ArrayList<Patient> patientProfiles = new ArrayList<>();
        for (String url : patientUrls) {
            patientProfiles.add(getPatientProfile(url, requestContext));
        }
        String profiles = new Gson().toJson(patientProfiles);
        writeToFile(profiles, String.format("/zipFolder/%s.zip", filter));
        return patientProfiles;
    }

    private void writeToFile(String patientProfiles, String fileName) throws IOException {
        File file = new File(fileName);
        FileOutputStream fout = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new GZIPOutputStream(fout), "UTF-8");
        outputStreamWriter.write(patientProfiles);
        outputStreamWriter.close();
//        ObjectOutputStream oos = new ObjectOutputStream(fout);
//        oos.writeObject(patientProfiles);
        fout.close();
    }

    private Patient getPatientProfile(String url, RequestContext requestContext) {
        Patient patientByUuid = Context.getPatientService().getPatientByUuid(getUuidFromUrl(url));

        return patientByUuid;
    }


    private String getUuidFromUrl(String url) {
        String uuid = "";
        Pattern uuidPattern = Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        Matcher matcher = uuidPattern.matcher(url);
        if (matcher.find())
            uuid = matcher.group(0);
        return uuid;
    }
}

