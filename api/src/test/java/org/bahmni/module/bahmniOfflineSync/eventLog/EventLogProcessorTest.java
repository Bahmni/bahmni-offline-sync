package org.bahmni.module.bahmniOfflineSync.eventLog;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bahmni.module.bahmniOfflineSync.utils.PatientProfileWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({Context.class})
@RunWith(PowerMockRunner.class)
public class EventLogProcessorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    ResultSet resultSet;

    @Mock
    PersonNameTransformer personNameTransformer;

    private File resultFile;
    private File directory;
    private String sql;
    private PatientProfileWriter writer;
    private EventLogProcessor eventLogProcessor;

    @Before
    public void setUp() throws Exception {
        directory = new File("./patient");
        directory.mkdir();
        initMocks(this);

        SimpleObject person1 = new SimpleObject();
        person1.put("given_name", "Super");
        person1.put("uuid", "ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562");

        SimpleObject person2 = new SimpleObject();
        person2.put("given_name", "Horatio");
        person2.put("uuid", "da7f524f-27ce-4bb2-86d6-6d1d05312bd5");

        sql = "select uuid from person";

        when(connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn("ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562")
                .thenReturn("da7f524f-27ce-4bb2-86d6-6d1d05312bd5").thenReturn("da7f524f-27ce-4bb2-86d6-6d1d05312bdx");
        when(personNameTransformer.transform("ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562")).thenReturn(person1);
        when(personNameTransformer.transform("da7f524f-27ce-4bb2-86d6-6d1d05312bd5")).thenReturn(person2);
        when(personNameTransformer.transform("da7f524f-27ce-4bb2-86d6-6d1d05312bdx")).thenReturn(null);

        writer = getWriter("GAN", ".");
        eventLogProcessor = new EventLogProcessor(sql, connection, personNameTransformer);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(directory);
    }

    @Test
    public void shouldIterateThroughAllPatient() throws Exception {
        eventLogProcessor.process(eventLogProcessor.getUrlObjects(), writer);
        String expected = "{\"given_name\":\"Super\",\"uuid\":\"ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562\"}," +
                "{\"given_name\":\"Horatio\",\"uuid\":\"da7f524f-27ce-4bb2-86d6-6d1d05312bd5\"}";
        assertEquals(expected, IOUtils.toString(new FileInputStream(resultFile)));
    }

    @Test
    public void shouldThrowEventLogIteratorExceptionWhenSqlQueryHasAnyError() throws Exception {
        when(connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
                .thenThrow(new SQLException());

        thrown.expect(EventLogIteratorException.class);
        thrown.expectMessage("Error in setting up of SQL query");
        eventLogProcessor.process(eventLogProcessor.getUrlObjects(), writer);
    }

    @Test
    public void shouldThrowEventLogIteratorExceptionWhenUnableToWriteUsingGivenWriter() throws Exception {
        writer.close();

        thrown.expect(EventLogIteratorException.class);
        thrown.expectMessage("Error while writing with provided writer");
        eventLogProcessor.process(eventLogProcessor.getUrlObjects(), writer);

    }

    class PersonNameTransformer implements RowTransformer {
        @Override
        public SimpleObject transform(String uuid) {
            return null;
        }
    }

    private PatientProfileWriter getWriter(String filter, String initSyncDirectory) throws IOException {
        String fileName = String.format("%s/patient/%s.json.gz", initSyncDirectory, filter);
        resultFile = new File(fileName);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(resultFile), "UTF-8"));
        return (new PatientProfileWriter(bufferedWriter));
    }
}
