package org.bahmni.module.bahmniOfflineSync.job;

import org.bahmni.module.bahmniOfflineSync.eventLog.BulkEventLogProcessor;
import org.bahmni.module.bahmniOfflineSync.utils.PatientProfileWriter;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Component("BulkPatientByFilterPublisher")
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/zip")
public class BulkPatientByFilterPublisher extends AbstractTask {

    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;

    private static final String GP_BAHMNICONNECT_INIT_SYNC_PATH = "bahmniconnect.initsync.directory";
    private static final String DEFAULT_INIT_SYNC_PATH = "/home/bahmni/init_sync";

    public BulkPatientByFilterPublisher() {
        atomFeedSpringTransactionManager = createTransactionManager();

    }

    private AtomFeedSpringTransactionManager createTransactionManager() {
        PlatformTransactionManager platformTransactionManager = getSpringPlatformTransactionManager();
        return new AtomFeedSpringTransactionManager(platformTransactionManager);
    }

    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }

    private PatientProfileWriter getWriter(String filter, String initSyncDirectory) throws IOException {
        String fileName = String.format("%s/patient/%s.json.gz", initSyncDirectory, filter);
        GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(fileName)));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));
        return (new PatientProfileWriter(bufferedWriter));
    }

    private List<String> getAllFilters() throws SQLException {
        String queryString = "SELECT DISTINCT filter FROM event_log WHERE category='patient'";
        Connection connection = atomFeedSpringTransactionManager.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement(queryString);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<String> filters = new ArrayList<>();
        while (resultSet.next()) {
            filters.add(resultSet.getString(1));
        }
        return filters;
    }

    private SimpleObject getLastEvent() throws SQLException {
        Connection connection = atomFeedSpringTransactionManager.getConnection();
        SimpleObject event = new SimpleObject();
        String queryString = "SELECT id, uuid FROM event_log ORDER BY id DESC LIMIT 1";
        PreparedStatement preparedStatement = connection.prepareStatement(queryString);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        event.put("id", resultSet.getString(1));
        event.put("uuid", resultSet.getString(2));
        return event;
    }

    @Override
    @RequestMapping(method = RequestMethod.GET, value = "/test")
    @ResponseBody
    public void execute() {
        String sql;
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        try {
            SimpleObject lastEvent = getLastEvent();
            Integer lastEventId = new Integer(lastEvent.get("id"));
            List<String> filters = getAllFilters();
            String preText = String.format("{\"lastReadEventUuid\":\"%s\", \"patients\":[", lastEvent.get("uuid").toString());
            String postText = "]}";
            for (String filter : filters) {
                Connection connection = atomFeedSpringTransactionManager.getConnection();
                sql = String.format("SELECT DISTINCT object FROM event_log WHERE filter='%s' and id <= %d and category = 'patient'", filter, lastEventId);
                PatientProfileWriter patientProfileWriter = getWriter(filter, initSyncDirectory);
                patientProfileWriter.write(preText);
                BulkEventLogProcessor bulkEventLogProcessor = new BulkEventLogProcessor(sql,
                        connection, new PatientProfileTransformer(), patientProfileWriter);
                bulkEventLogProcessor.process();
                patientProfileWriter.write(postText);
                patientProfileWriter.close();
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}
