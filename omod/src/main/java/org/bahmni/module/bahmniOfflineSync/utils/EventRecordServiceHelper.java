package org.bahmni.module.bahmniOfflineSync.utils;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.Configuration;
import org.ict4h.atomfeed.jdbc.JdbcResultSetMapper;
import org.ict4h.atomfeed.jdbc.JdbcUtils;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.ict4h.atomfeed.server.exceptions.AtomFeedRuntimeException;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventRecordServiceHelper {
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;


    public EventRecordServiceHelper() {
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

    public List<EventRecord> getEventRecordsAfterUuid(String lastReadUuid) {
        ArrayList<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord lastReadEventRecord = findEventRecordByUuid(lastReadUuid);
        if(lastReadEventRecord!=null) {
            List<EventRecord> events  = findEventRecordsAfterId(lastReadEventRecord.getId());
            eventRecords.addAll(events);
        }
        return eventRecords;
    }

    public EventRecord findEventRecordByUuid(String uuid) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<EventRecord> events = new ArrayList<EventRecord>();
        Connection connection = null;
        try {
            connection = atomFeedSpringTransactionManager.getConnection();
            String queryString = String.format("select * from %s where uuid = ?", new Object[]{JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records")});
            stmt = connection.prepareStatement(queryString);
            stmt.setString(1, uuid);
            rs = stmt.executeQuery();
            events = this.mapEventRecords(rs);
        } catch (SQLException var11) {
            throw new AtomFeedRuntimeException(var11);
        } finally {
            this.closeAll(stmt, rs);
        }
        return !events.isEmpty() ? events.get(0) : null;

    }

    private List<EventRecord> findEventRecordsAfterId( Integer id) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection connection = null;
        List<EventRecord> events = new ArrayList<EventRecord>();
        try {
            connection = atomFeedSpringTransactionManager.getConnection();
            String queryString = String.format("select * from %s where id > ? order by id limit 1000 ", new Object[]{JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records")});
            stmt = connection.prepareStatement(queryString);
            stmt.setString(1, id.toString());
            rs = stmt.executeQuery();
            events = this.mapEventRecords(rs);
        } catch (SQLException var11) {
            throw new AtomFeedRuntimeException(var11);
        } finally {
            this.closeAll(stmt, rs);
        }
        return events;
    }

    private void closeAll(PreparedStatement stmt, ResultSet rs) {
        this.close(rs);
        this.close(stmt);
    }

    private void close(AutoCloseable rs) {
        try {
            if (rs != null) {
                rs.close();
            }

        } catch (Exception var3) {
            throw new AtomFeedRuntimeException(var3);
        }
    }

    private List<EventRecord> mapEventRecords(ResultSet results) {
        return (new JdbcResultSetMapper()).mapResultSetToObject(results, EventRecord.class);
    }

    public List<EventRecord> getTopEventRecordsOrderByIdAsc() {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<EventRecord> events = new ArrayList<EventRecord>();
        Connection connection = null;
        try {
            connection = atomFeedSpringTransactionManager.getConnection();
            String queryString = String.format("select * from %s order by id limit 1000 ", new Object[]{JdbcUtils.getTableName(Configuration.getInstance().getSchema(), "event_records")});
            stmt = connection.prepareStatement(queryString);
            rs = stmt.executeQuery();
            events = this.mapEventRecords(rs);
        } catch (SQLException var11) {
            throw new AtomFeedRuntimeException(var11);
        } finally {
            this.closeAll(stmt, rs);
        }
        return events;
    }

    public List<EventLog> getDistinctEventLogsByCategory(String category, String object) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<EventLog> events = new ArrayList<EventLog>();
        Connection connection = null;
        try {
            connection = atomFeedSpringTransactionManager.getConnection();
            String queryString = String.format("select * from event_log where category = '%s' group by %s", category, object);
            stmt = connection.prepareStatement(queryString);
            rs = stmt.executeQuery();
            events =  (new JdbcResultSetMapper()).mapResultSetToObject(rs, EventLog.class);
        } catch (SQLException var11) {
            throw new AtomFeedRuntimeException(var11);
        } finally {
            this.closeAll(stmt, rs);
        }
        return events;
    }

}
