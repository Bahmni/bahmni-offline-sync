package org.bahmni.module.bahmniOfflineSync.eventLog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.utils.PatientProfileWriter;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.springframework.jdbc.support.JdbcUtils;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventLogProcessor {

    protected Log log = LogFactory.getLog(getClass());

    private String sql;

    private Connection connection;

    private ResultSet resultSet;

    private PreparedStatement preparedStatement;

    private RowTransformer rowTransformer;

    private PatientProfileWriter writer;

    public EventLogProcessor(String sql, Connection connection, RowTransformer rowTransformer, PatientProfileWriter writer) {
        this.sql = sql;
        this.connection = connection;
        this.rowTransformer = rowTransformer;
        this.writer = writer;
    }

    public void process() {
        try {
            preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setFetchDirection(ResultSet.FETCH_FORWARD);
            ResultSet resultSet = preparedStatement.executeQuery();
            handleWarnings(preparedStatement);

            List<String> urls = new ArrayList<>();

            while (resultSet.next()) {
                urls.add((resultSet.getString(1)));
            }
            for (int index = 0; index < urls.size(); index++) {
                String url = urls.get(index);
                SimpleObject simpleObject = rowTransformer.transform(url);
                if (index != 0) {
                    writer.append(",");
                }
                writer.write(simpleObject);
            }
        } catch (SQLException e) {
            clean();
            throw new EventLogIteratorException("Error in setting up of SQL query", e);
        } catch (IOException e) {
            throw new EventLogIteratorException("Error while writing with provided writer [" + writer.toString() + "]", e);
        } finally {
            clean();
        }
    }

    private void handleWarnings(PreparedStatement preparedStatement) throws SQLException {
        SQLWarning warningToLog = preparedStatement.getWarnings();
        while (warningToLog != null) {
            log.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '"
                    + warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
            warningToLog = warningToLog.getNextWarning();
        }
    }

    private void clean() {
        JdbcUtils.closeResultSet(this.resultSet);
        this.resultSet = null;
        JdbcUtils.closeStatement(this.preparedStatement);
    }
}
