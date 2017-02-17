package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@PrepareForTest({Context.class})
@RunWith(PowerMockRunner.class)
public class BulkLoadControllerTest {
    @Mock
    AdministrationService administrationService;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        when(Context.getAdministrationService()).thenReturn(administrationService);

        SimpleObject patientProfile1 = new SimpleObject();
        patientProfile1.add("uuid","uuid1")
                        .add("patient", "blah...blah..blah");

        SimpleObject patientProfile2 = new SimpleObject();
        patientProfile2.add("uuid","uuid2")
                .add("patient", "blah2...blah2..blah2");

        ObjectMapper objectMapper = new ObjectMapper();
        File resultFile = new File("./patient/ABC.json");
        FileUtils.writeStringToFile(resultFile,"");
        objectMapper.writeValue(resultFile,Arrays.asList(patientProfile1, patientProfile2));
    }

    @After
    public void tearDown(){
        FileUtils.deleteQuietly(new File("./patient"));
    }

    @Test
    public void shouldRetrievePatientListFromPredefinedLocation() throws Exception {
        when(administrationService.getGlobalProperty(BulkLoadController.GP_BAHMNICONNECT_INIT_SYNC_PATH, BulkLoadController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");

        BulkLoadController controller = new BulkLoadController();
        List<SimpleObject> patientProfileList = controller.getPatientsInBulk("ABC");
        assertNotNull(patientProfileList);
        assertEquals(2,patientProfileList.size());
        assertEquals("uuid1", patientProfileList.get(0).get("uuid"));
        assertEquals("blah...blah..blah", patientProfileList.get(0).get("patient"));
        assertEquals("uuid2", patientProfileList.get(1).get("uuid"));
        assertEquals("blah2...blah2..blah2", patientProfileList.get(1).get("patient"));
    }

    @Test
    public void shouldThrowAPIExceptionWhenFileIsNotAvailableForFilter() {
        when(administrationService.getGlobalProperty(BulkLoadController.GP_BAHMNICONNECT_INIT_SYNC_PATH, BulkLoadController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        BulkLoadController controller = new BulkLoadController();
        thrown.expect(APIException.class);
        thrown.expectMessage("Bulk patient file is not available at [./patient] for [ABCD]");

        controller.getPatientsInBulk("ABCD");
    }

    @Test
    public void shouldThrowApiExceptionWhenThereIsAParsingError() throws Exception {
        when(administrationService.getGlobalProperty(BulkLoadController.GP_BAHMNICONNECT_INIT_SYNC_PATH, BulkLoadController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        BulkLoadController controller = new BulkLoadController();
        FileUtils.writeStringToFile(new File("./patient/ABC.json"),"");
        thrown.expect(APIException.class);
        thrown.expectMessage("Cannot parse the patient file at location [./patient/ABC.json]");

        controller.getPatientsInBulk("ABC");
    }
}