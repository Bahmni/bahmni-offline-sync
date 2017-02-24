package org.bahmni.module.bahmniOfflineSync.job;

import org.apache.commons.io.FileUtils;
import org.h2.tools.RunScript;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import static org.h2.engine.Constants.UTF8;
import static org.junit.Assert.assertEquals;

public class BulkPatientByFilterPublisherIT extends BaseModuleWebContextSensitiveTest {

    private String directory = "src/test/resources/patient/";

    @BeforeClass
    public static void createSchema() throws Exception {
        RunScript.execute("jdbc:h2:mem:openmrs;DB_CLOSE_DELAY=-1",
                "sa", "", "classpath:schema.sql", UTF8, false);
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("eventLog.xml");
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File(directory));
    }

    @Test
    public void shouldCreateZipFileForEachFilter() throws Exception {
        BulkPatientByFilterPublisher bulkPatientByFilterPublisher = new BulkPatientByFilterPublisher();
        bulkPatientByFilterPublisher.execute();

        File ganFile = new File(directory + "GAN.json.gz");
        Assert.assertTrue(ganFile.exists());
        File semFile = new File(directory + "SEM.json.gz");
        Assert.assertTrue(semFile.exists());
        File nullFile = new File(directory + "null.json.gz");
        Assert.assertTrue(nullFile.exists());

        JSONObject ganList = unzip(ganFile);
        JSONArray ganPatients = (JSONArray) ganList.get("patients");
        assertEquals(3, (ganPatients).length());
        assertEquals("ac46eca0-51d5-11e3-8f96-0800200c4a70", ganList.get("lastReadEventUuid"));
        assertEquals("ca17fcc5-ec96-487f-b9ea-42973c8973e3", ((JSONObject) ganPatients.get(0)).getString("uuid"));
        assertEquals("61b38324-e2fd-4feb-95b7-9e9a2a4400df", ((JSONObject) ganPatients.get(1)).getString("uuid"));
        assertEquals("8d703ff2-c3e2-4070-9737-73e713d5a50d", ((JSONObject) ganPatients.get(2)).getString("uuid"));


        JSONObject semList = unzip(semFile);
        JSONArray semPatients = (JSONArray) semList.get("patients");
        assertEquals(2, semPatients.length());
        assertEquals("ac46eca0-51d5-11e3-8f96-0800200c4a70", semList.get("lastReadEventUuid"));
        assertEquals("5c521595-4e12-46b0-8248-b8f2d3697766", ((JSONObject) semPatients.get(0)).getString("uuid"));
        assertEquals("30e2aa2a-4ed1-415d-84c5-ba29016c14b7", ((JSONObject) semPatients.get(1)).getString("uuid"));

        JSONObject list = unzip(nullFile);
        JSONArray patients = (JSONArray) list.get("patients");
        assertEquals(1, patients.length());
        assertEquals("ac46eca0-51d5-11e3-8f96-0800200c4a70", list.get("lastReadEventUuid"));
        assertEquals("256ccf6d-6b41-455c-9be2-51ff4386ae76", ((JSONObject) patients.get(0)).getString("uuid"));

    }


    private JSONObject unzip(File file) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(file));
        return new JSONObject(org.apache.commons.io.IOUtils.toString(gzipInputStream));
    }


}
