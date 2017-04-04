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
    protected static int JUMP_SIZE = 1000;

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

    private PatientProfileWriter getWriter(String name, String initSyncDirectory, String category) throws IOException {
        File patientDirectory = new File(String.format("%s/%s", initSyncDirectory, category));
        if (!patientDirectory.exists()) {
            patientDirectory.mkdir();
        }
        String fileName = String.format("%s/%s/%s.json.gz", initSyncDirectory, category, name);
        GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(fileName)));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));
        return (new PatientProfileWriter(bufferedWriter));
    }

    private List<String> getAllFilters() throws SQLException {
        String queryString = "SELECT DISTINCT filter FROM event_log WHERE category='patient' AND filter != '' and filter IS NOT NULL ";
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
            log.info("InitialSyncArtifactsPublisher job started");
            createInitSyncDirectory(initSyncDirectory);
            SimpleObject lastEvent = getLastEvent();
            Integer lastEventId = new Integer(lastEvent.get("id"));
            List<String> filters = getAllFilters();
            String preTextTemplate = "{\"lastReadEventUuid\":\"%s\", \"patients\":[";
            String postText = "]}";
            for (String filter : filters) {
                log.info(String.format("Creating zip files for %s is started", filter));
                Connection connection = atomFeedSpringTransactionManager.getConnection();
                sql = getSql(lastEventId, filter);

                EventLogProcessor eventLogProcessor = new EventLogProcessor(sql, connection, new PatientProfileTransformer());
                List<SimpleObject> urls = eventLogProcessor.getUrlObjects();

                for (int index = 0; index < urls.size(); index += JUMP_SIZE) {
                    String fileName = getFileName(filter, index);
                    log.info(String.format("Creating zip file for %s is started", fileName));
                    List<SimpleObject> subUrls = urls.subList(index, getUpperLimit(index, urls.size()));
                    PatientProfileWriter patientProfileWriter = getWriter(fileName, initSyncDirectory, "patient");
                    String lastEventUuid = (index + JUMP_SIZE < urls.size()) ?
                            subUrls.get(subUrls.size() - 1).get("uuid").toString() : lastEvent.get("uuid").toString();
                    String preText = String.format(preTextTemplate, lastEventUuid);
                    patientProfileWriter.write(preText);
                    eventLogProcessor.process(subUrls, patientProfileWriter);
                    patientProfileWriter.write(postText);
                    patientProfileWriter.close();
                    Thread.sleep(1000);
                    log.info(String.format("Creating zip file for %s is successfully completed", fileName));
                }

                log.info(String.format("Creating zip files for %s is successfully completed", filter));
            }
            log.info("InitialSyncArtifactsPublisher job completed");
        } catch (SQLException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getFileName(String filter, int index) {
        return String.format("%s-%s", filter, (String.valueOf((index / JUMP_SIZE) + 1)));
    }

    private int getUpperLimit(int index, int size) {
        int limit = index + JUMP_SIZE;
        return size < limit ? size : limit;
    }

    private void createInitSyncDirectory(String initSyncDirectory) {
        File directory = new File(initSyncDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private String getSql(Integer lastEventId, String filter) {
        String template = "SELECT object, uuid FROM event_log WHERE filter = '%s' and id <= %d and category = 'patient' GROUP BY object";
        return String.format(template, filter, lastEventId);
    }

}
