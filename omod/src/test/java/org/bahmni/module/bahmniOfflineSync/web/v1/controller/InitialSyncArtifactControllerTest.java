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
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@PrepareForTest({Context.class, IOUtils.class})
@RunWith(PowerMockRunner.class)
public class InitialSyncArtifactControllerTest {
    @Mock
    AdministrationService administrationService;

    @Mock
    HttpServletResponse httpServletResponse;

    @Mock
    ResourceLoader resourceLoader;

    private File resultFile = null;
    private File resultOfflineConceptFile = null;
    private File resultAddressHierarchyFile = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        when(Context.getAdministrationService()).thenReturn(administrationService);

        resultFile = new File("./patient/ABC.json.gz");
        resultOfflineConceptFile = new File("./offline-concepts/offline-concepts.json.gz");
        resultAddressHierarchyFile = new File("./addressHierarchy/addressHierarchy.json.gz");
        FileUtils.writeStringToFile(resultFile, "blah..blah..blah..");
        FileUtils.writeStringToFile(resultOfflineConceptFile, "blah..blah..blah..");
        FileUtils.writeStringToFile(resultAddressHierarchyFile, "blah..blah..blah..");
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File("./patient"));
        FileUtils.deleteQuietly(new File("./offline-concepts"));
        FileUtils.deleteQuietly(new File("./addressHierarchy"));
    }

    @Test
    public void shouldRetrieveCompressedPatientFileFromPredefinedLocation() throws Exception {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./patient/ABC.json.gz")).thenReturn(new FileSystemResource(resultFile));

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        controller.setResourceLoader(resourceLoader);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.getPatientsByFilter(response, "ABC.json.gz");

        assertEquals("blah..blah..blah..", response.getContentAsString());
        assertEquals("application/json", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("gzip", response.getHeader("Content-Encoding"));
    }

    @Test
    public void shouldGiveAllTheFileNamesStartedWithGivenFilterSortByLastModifiedTime() throws Exception {
        FileUtils.cleanDirectory(new File("./patient"));
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        (new File("./patient/ABC-1.json.gz")).createNewFile();
        (new File("./patient/CDE-1.json.gz")).createNewFile();
        (new File("./patient/ABC-2.json.gz")).createNewFile();

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getFileNames("ABC");
        assertEquals(2, fileNames.size());
        assertTrue(fileNames.contains("ABC-1.json.gz"));
        assertTrue(fileNames.contains("ABC-2.json.gz"));
        assertFalse(fileNames.contains("CDE-1.json.gz"));
    }

    @Test
    public void shouldGiveEmptyListWhenFilesAreNotAvailableForFilter() throws Exception {
        FileUtils.cleanDirectory(new File("./patient"));
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getFileNames("ABC");
        assertEquals(0, fileNames.size());
    }

    @Test
    public void shouldNotThrowExceptionIfBaseDirectoryIsNotPresent() throws Exception {
        FileUtils.cleanDirectory(new File("./patient"));
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn("./patient/not/present");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getFileNames("ABC");
        assertEquals(0, fileNames.size());
    }

    @Test
    public void shouldThrowAPIExceptionWhenPatientFileIsNotAvailable() {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        thrown.expect(APIException.class);
        thrown.expectMessage("File [ABCD.json.gz] is not available at [./patient]");

        controller.getPatientsByFilter(httpServletResponse, "ABCD.json.gz");
    }

    @Test
    public void shouldThrowAPIExceptionWhenFileIsPresentButUnableToParseTheContent() throws Exception {
        PowerMockito.mockStatic(IOUtils.class);
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./patient/ABC.json.gz")).thenReturn(new FileSystemResource(resultFile));
        when(IOUtils.copy(any(InputStream.class), any(OutputStream.class))).thenThrow(new IOException());

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        controller.setResourceLoader(resourceLoader);

        thrown.expect(APIException.class);
        thrown.expectMessage("Cannot parse the patient file at location [./patient/ABC.json.gz]");

        controller.getPatientsByFilter(httpServletResponse, "ABC.json.gz");

    }

    @Test
    public void shouldRetrieveCompressedOfflineConceptFileFromPredefinedLocation() throws Exception{
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./offline-concepts/offline-concepts.json.gz")).thenReturn(new FileSystemResource(resultOfflineConceptFile));

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        controller.setResourceLoader(resourceLoader);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.getOfflineConceptsByFilter(response, "offline-concepts.json.gz");

        assertEquals("blah..blah..blah..", response.getContentAsString());
        assertEquals("application/json", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("gzip", response.getHeader("Content-Encoding"));
    }

    @Test
    public void shouldGiveAllOfflineFileNamesStartedWithGivenFilterSortByLastModifiedTime() throws Exception {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        (new File("./offline-concepts/ABC-1.json.gz")).createNewFile();
        (new File("./offline-concepts/CDE-1.json.gz")).createNewFile();
        (new File("./offline-concepts/ABC-2.json.gz")).createNewFile();

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getOfflineConceptFileNames("ABC");
        assertEquals(2, fileNames.size());
        assertTrue(fileNames.contains("ABC-1.json.gz"));
        assertTrue(fileNames.contains("ABC-2.json.gz"));
        assertFalse(fileNames.contains("CDE-1.json.gz"));
    }

    @Test
    public void shouldGiveEmptyListWhenOfflineConceptsFilesAreNotAvailableForFilter() throws Exception {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getOfflineConceptFileNames("offlineconcepts");
        assertEquals(0, fileNames.size());
    }

    @Test
    public void shouldNotThrowExceptionIfBaseDirectoryIsNotPresentForOfflineConcepts() throws Exception {
        FileUtils.cleanDirectory(new File("./offline-concepts"));
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn("./patient/not/present");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getOfflineConceptFileNames("ABC");
        assertEquals(0, fileNames.size());
    }

    @Test
    public void shouldThrowAPIExceptionWhenOfflineConceptsFileIsNotAvailable() {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        thrown.expect(APIException.class);
        thrown.expectMessage("File [ABCD.json.gz] is not available at [./offline-concepts]");

        controller.getOfflineConceptsByFilter(httpServletResponse, "ABCD.json.gz");
    }

    @Test
    public void shouldThrowAPIExceptionWhenFileIsPresentButUnableToParseTheContentForOfflineConcepts() throws Exception {
        PowerMockito.mockStatic(IOUtils.class);
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./offline-concepts/offline-concepts.json.gz")).thenReturn(new FileSystemResource(resultOfflineConceptFile));
        when(IOUtils.copy(any(InputStream.class), any(OutputStream.class))).thenThrow(new IOException());

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        controller.setResourceLoader(resourceLoader);

        thrown.expect(APIException.class);
        thrown.expectMessage("Cannot parse the offline-concepts file at location [./offline-concepts/offline-concepts.json.gz]");

        controller.getOfflineConceptsByFilter(httpServletResponse, "offline-concepts.json.gz");

    }

    @Test
    public void shouldRetrieveCompressedAddressHierarchyFileFromPredefinedLocation() throws Exception{
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./addressHierarchy/addressHierarchy.json.gz")).thenReturn(new FileSystemResource(resultAddressHierarchyFile));

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        controller.setResourceLoader(resourceLoader);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.getAddressHierarchyByFilter(response, "addressHierarchy.json.gz");

        assertEquals("blah..blah..blah..", response.getContentAsString());
        assertEquals("application/json", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("gzip", response.getHeader("Content-Encoding"));
    }

    @Test
    public void shouldGiveAllAddressHierarchyFileNamesStartedWithGivenFilterSortByLastModifiedTime() throws Exception {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        (new File("./addressHierarchy/ABC-1.json.gz")).createNewFile();
        (new File("./addressHierarchy/CDE-1.json.gz")).createNewFile();
        (new File("./addressHierarchy/ABC-2.json.gz")).createNewFile();

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getAddressHierarchyFileNames("ABC");
        assertEquals(2, fileNames.size());
        assertTrue(fileNames.contains("ABC-1.json.gz"));
        assertTrue(fileNames.contains("ABC-2.json.gz"));
        assertFalse(fileNames.contains("CDE-1.json.gz"));
    }

    @Test
    public void shouldGiveEmptyListWhenAddressHierarchyFilesAreNotAvailableForFilter() throws Exception {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getAddressHierarchyFileNames("addressHierarchy");
        assertEquals(0, fileNames.size());
    }

    @Test
    public void shouldNotThrowExceptionIfBaseDirectoryIsNotPresentForAddressHierarchy() throws Exception {
        FileUtils.cleanDirectory(new File("./addressHierarchy"));
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn("./patient/not/present");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        ArrayList<String> fileNames = controller.getAddressHierarchyFileNames("ABC");
        assertEquals(0, fileNames.size());
    }

    @Test
    public void shouldThrowAPIExceptionWhenAddressHierarchyFileIsNotAvailable() {
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        thrown.expect(APIException.class);
        thrown.expectMessage("File [ABCD.json.gz] is not available at [./addressHierarchy]");

        controller.getAddressHierarchyByFilter(httpServletResponse, "ABCD.json.gz");
    }

    @Test
    public void shouldThrowAPIExceptionWhenFileIsPresentButUnableToParseTheContentForAddressHierarchy() throws Exception {
        PowerMockito.mockStatic(IOUtils.class);
        when(administrationService.getGlobalProperty(InitialSyncArtifactController.GP_BAHMNICONNECT_INIT_SYNC_PATH, InitialSyncArtifactController.DEFAULT_INIT_SYNC_PATH)).thenReturn(".");
        when(resourceLoader.getResource("file:./offline-concepts/offline-concepts.json.gz")).thenReturn(new FileSystemResource(resultOfflineConceptFile));
        when(IOUtils.copy(any(InputStream.class), any(OutputStream.class))).thenThrow(new IOException());

        InitialSyncArtifactController controller = new InitialSyncArtifactController();
        controller.setResourceLoader(resourceLoader);

        thrown.expect(APIException.class);
        thrown.expectMessage("Cannot parse the offline-concepts file at location [./offline-concepts/offline-concepts.json.gz]");

        controller.getOfflineConceptsByFilter(httpServletResponse, "offline-concepts.json.gz");

    }

}