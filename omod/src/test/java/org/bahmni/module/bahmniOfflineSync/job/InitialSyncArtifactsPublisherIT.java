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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InitialSyncArtifactsPublisherIT extends BaseModuleWebContextSensitiveTest {

    private String directory = "src/test/resources/patient/";
    private String offlineConceptsDirectory = "src/test/resources/offline-concepts";

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
        FileUtils.deleteQuietly(new File(offlineConceptsDirectory));
    }

    @Test
    public void shouldCreateZipFileForEachFilter() throws Exception {
        InitialSyncArtifactsPublisher initialSyncArtifactsPublisher = new InitialSyncArtifactsPublisher();
        InitialSyncArtifactsPublisher.JUMP_SIZE = 2;
        initialSyncArtifactsPublisher.execute();

        File ganFile_1 = new File(directory + "GAN-1.json.gz");
        assertTrue(ganFile_1.exists());
        File ganFile_2 = new File(directory + "GAN-2.json.gz");
        assertTrue(ganFile_2.exists());
        File semFile = new File(directory + "SEM-1.json.gz");
        assertTrue(semFile.exists());
        File nullFile = new File(directory + "null-1.json.gz");
        assertFalse(nullFile.exists());
        File emptyFile = new File(directory + "-1.json.gz");
        assertFalse(emptyFile.exists());

        File offline_concepts = new File(offlineConceptsDirectory + "offline-concepts-1.json.gz");
        assertTrue(offline_concepts.exists());

        JSONObject offlineConcepts_1 = unzip(ganFile_1);
        JSONArray offlineConcepts_1_Array = (JSONArray) offlineConcepts_1.get("offlineconcepts");
        assertEquals(1, (offlineConcepts_1_Array).length());
        assertEquals("6f0762d6-3b38-11ea-b6b4-6c2b598065d8", offlineConcepts_1.get("lastReadEventUuid"));
        assertEquals("47f8952e-3b38-11ea-a342-6c2b598065d8", ((JSONObject) offlineConcepts_1_Array.get(0)).getString("uuid"));

        JSONObject ganList_1 = unzip(ganFile_1);
        JSONArray ganPatients_1 = (JSONArray) ganList_1.get("patients");
        assertEquals(2, (ganPatients_1).length());
        assertEquals("ac46eca0-51d5-11e3-8f96-0800200c9a66", ganList_1.get("lastReadEventUuid"));
        assertEquals("ca17fcc5-ec96-487f-b9ea-42973c8973e3", ((JSONObject) ganPatients_1.get(0)).getString("uuid"));
        assertEquals("61b38324-e2fd-4feb-95b7-9e9a2a4400df", ((JSONObject) ganPatients_1.get(1)).getString("uuid"));

        JSONObject ganList_2 = unzip(ganFile_2);
        JSONArray gan_2Patients = (JSONArray) ganList_2.get("patients");
        assertEquals(1, (gan_2Patients).length());
        assertEquals("ac46eca0-51d5-11e3-8f96-0800200c4a71", ganList_2.get("lastReadEventUuid"));
        assertEquals("8d703ff2-c3e2-4070-9737-73e713d5a50d", ((JSONObject) gan_2Patients.get(0)).getString("uuid"));


        JSONObject semList = unzip(semFile);
        JSONArray semPatients = (JSONArray) semList.get("patients");
        assertEquals(2, semPatients.length());
        assertEquals("ac46eca0-51d5-11e3-8f96-0800200c4a71", semList.get("lastReadEventUuid"));
        assertEquals("5c521595-4e12-46b0-8248-b8f2d3697766", ((JSONObject) semPatients.get(0)).getString("uuid"));
        assertEquals("30e2aa2a-4ed1-415d-84c5-ba29016c14b7", ((JSONObject) semPatients.get(1)).getString("uuid"));
    }

    private JSONObject unzip(File file) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(file));
        return new JSONObject(org.apache.commons.io.IOUtils.toString(gzipInputStream));
    }
}
