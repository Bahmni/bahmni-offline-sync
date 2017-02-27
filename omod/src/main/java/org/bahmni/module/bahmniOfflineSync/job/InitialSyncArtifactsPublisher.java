package org.bahmni.module.bahmniOfflineSync.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.eventLog.EventLogProcessor;
import org.bahmni.module.bahmniOfflineSync.utils.PatientProfileWriter;
import org.bahmni.module.bahmniOfflineSync.web.v1.controller.InitialSyncArtifactController;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Component("InitialSyncArtifactsPublisher")
public class InitialSyncArtifactsPublisher extends AbstractTask {
    protected Log log = LogFactory.getLog(getClass());

    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;

    public InitialSyncArtifactsPublisher() {
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
        File patientDirectory = new File(String.format("%s/patient", initSyncDirectory));
        if (!patientDirectory.exists()) {
            patientDirectory.mkdir();
        }
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
    public void execute() {
        String sql;
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH);
        try {
            log.debug("InitialSyncArtifactsPublisher job started");
            createInitSyncDirectory(initSyncDirectory);
            SimpleObject lastEvent = getLastEvent();
            Integer lastEventId = new Integer(lastEvent.get("id"));
            List<String> filters = getAllFilters();
            String preText = String.format("{\"lastReadEventUuid\":\"%s\", \"patients\":[", lastEvent.get("uuid").toString());
            String postText = "]}";
            for (String filter : filters) {
                log.debug(String.format("Creating zip file for %s is started", filter));
                Connection connection = atomFeedSpringTransactionManager.getConnection();
                sql = getSql(lastEventId, filter);
                PatientProfileWriter patientProfileWriter = getWriter(filter, initSyncDirectory);
                patientProfileWriter.write(preText);
                EventLogProcessor eventLogProcessor = new EventLogProcessor(sql,
                        connection, new PatientProfileTransformer(), patientProfileWriter);
                eventLogProcessor.process();
                patientProfileWriter.write(postText);
                patientProfileWriter.close();
                log.debug(String.format("Creating zip file for %s is successfully completed", filter));
            }
            log.debug("InitialSyncArtifactsPublisher job completed");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void createInitSyncDirectory(String initSyncDirectory) {
        File directory = new File(initSyncDirectory);
        if(!directory.exists()){
            directory.mkdirs();
        }
    }

    private String getSql(Integer lastEventId, String filter) {
        String template = "SELECT DISTINCT object FROM event_log WHERE %s and id <= %d and category = 'patient'";
        String filterCondition = (filter == null) ? "filter is null" : String.format("filter = '%s'", filter);
        return String.format(template, filterCondition, lastEventId);
    }

}
