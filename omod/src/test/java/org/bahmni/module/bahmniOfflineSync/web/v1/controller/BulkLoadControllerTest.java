package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@PrepareForTest({Context.class, IOUtils.class})
@RunWith(PowerMockRunner.class)
public class BulkLoadControllerTest {
    @Mock
    AdministrationService administrationService;

    @Mock
    HttpServletResponse httpServletResponse;

    @Mock
    ResourceLoader resourceLoader;

    private File resultFile = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        when(Context.getAdministrationService()).thenReturn(administrationService);

        resultFile = new File("./patient/ABC.json.gz");
        FileUtils.writeStringToFile(resultFile, "blah..blah..blah..");
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(resultFile);
    }

    @Test
    public void shouldRetrieveCompressedPatientFileFromPredefinedLocation() throws Exception {
        when(administrationService.getGlobalProperty(BulkLoadController.GP_BAHMNICONNECT_INIT_SYNC_PATH, BulkLoadController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./patient/ABC.json.gz")).thenReturn(new FileSystemResource(resultFile));

        BulkLoadController controller = new BulkLoadController();
        controller.setResourceLoader(resourceLoader);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.getPatientsInBulk(response, "ABC");

        assertEquals("blah..blah..blah..", response.getContentAsString());
        assertEquals("application/json", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("gzip", response.getHeader("Content-Encoding"));
    }

    @Test
    public void shouldThrowAPIExceptionWhenFileIsNotAvailableForFilter() {
        when(administrationService.getGlobalProperty(BulkLoadController.GP_BAHMNICONNECT_INIT_SYNC_PATH, BulkLoadController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        BulkLoadController controller = new BulkLoadController();
        thrown.expect(APIException.class);
        thrown.expectMessage("Bulk patient file is not available at [./patient] for [ABCD]");

        controller.getPatientsInBulk(httpServletResponse, "ABCD");
    }

    @Test
    public void shouldThrowAPIExceptionWhenFileIsPresentButUnableToParseTheContent() throws Exception {
        PowerMockito.mockStatic(IOUtils.class);
        when(administrationService.getGlobalProperty(BulkLoadController.GP_BAHMNICONNECT_INIT_SYNC_PATH, BulkLoadController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./patient/ABC.json.gz")).thenReturn(new FileSystemResource(resultFile));
        when(IOUtils.copy(any(InputStream.class), any(OutputStream.class))).thenThrow(new IOException());

        BulkLoadController controller = new BulkLoadController();
        controller.setResourceLoader(resourceLoader);

        thrown.expect(APIException.class);
        thrown.expectMessage("Cannot parse the patient file at location [./patient/ABC.json.gz]");

        controller.getPatientsInBulk(httpServletResponse, "ABC");

    }
}