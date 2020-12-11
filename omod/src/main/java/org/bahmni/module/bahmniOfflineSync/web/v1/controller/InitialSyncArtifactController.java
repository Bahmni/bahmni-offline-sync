package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/bahmniconnect")
public class InitialSyncArtifactController extends BaseRestController implements ResourceLoaderAware {

    public static final String GP_BAHMNICONNECT_INIT_SYNC_PATH = "bahmniconnect.initsync.directory";
    public static final String DEFAULT_INIT_SYNC_PATH = "/home/bahmni/init_sync";
    private ResourceLoader resourceLoader;

    @RequestMapping(method = RequestMethod.GET, value = "/patientfiles", params = {"filter"})
    @ResponseBody
    public ArrayList<String> getFileNames(@RequestParam(value = "filter") String filter) {
        Log logger = LogFactory.getLog(getClass());
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        File baseDirectory = new File(String.format("%s/patient", initSyncDirectory));

        if(!baseDirectory.exists()){
            return new ArrayList<>();
        }

        filter = filter.replace("[", "\\[").replace("]","\\]");
        String finalFilter = filter;
        File[] files = baseDirectory.listFiles((dir, name) -> name.matches(String.format("(.*)%s(.*)\\.json\\.gz", finalFilter)));
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        return Arrays.stream(files).map(File::getName).collect(Collectors.toCollection(ArrayList::new));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/offlineconceptfiles", params = {"filter"})
    @ResponseBody
    public ArrayList<String> getOfflineConceptFileNames(@RequestParam(value = "filter") String filter) {
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        File baseDirectory = new File(String.format("%s/offline-concepts", initSyncDirectory));

        if(!baseDirectory.exists()){
            return new ArrayList<>();
        }

        File[] files = baseDirectory.listFiles((dir, name) -> name.matches(String.format("%s-.*\\.json\\.gz", filter)));
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        return Arrays.stream(files).map(File::getName).collect(Collectors.toCollection(ArrayList::new));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/addresshierarchy", params = {"filter"})
    @ResponseBody
    public ArrayList<String> getAddressHierarchyFileNames(@RequestParam(value = "filter") String filter) {
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        File baseDirectory = new File(String.format("%s/addressHierarchy", initSyncDirectory));

        if(!baseDirectory.exists()){
            return new ArrayList<>();
        }

        File[] files = baseDirectory.listFiles((dir, name) -> name.matches(String.format("%s-.*\\.json\\.gz", filter)));
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        return Arrays.stream(files).map(File::getName).collect(Collectors.toCollection(ArrayList::new));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/patient", params = {"filename"})
    @ResponseBody
    public void getPatientsByFilter(HttpServletResponse response, @RequestParam(value = "filename") String filename) {
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        String filePath = String.format("%s/patient/%s", initSyncDirectory, filename);
        File initSyncFile = new File(filePath);

        if (!initSyncFile.exists()) {
            throw new APIException("File [" + filename + "] is not available at [" + initSyncDirectory + "/patient]");
        }

        try {
            Resource resource = resourceLoader.getResource("file:" + filePath);
            IOUtils.copy(new FileInputStream(resource.getFile()), response.getOutputStream());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Encoding", "gzip");
            response.flushBuffer();
        } catch (IOException e) {
            throw new APIException("Cannot parse the patient file at location [" + filePath + "]. Error [" + e.getMessage() + "]", e);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/offlineconcepts", params = {"filename"})
    @ResponseBody
    public void getOfflineConceptsByFilter(HttpServletResponse response, @RequestParam(value = "filename") String filename) {
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        String filePath = String.format("%s/offline-concepts/%s", initSyncDirectory, filename);
        File initSyncFile = new File(filePath);

        if (!initSyncFile.exists()) {
            throw new APIException("File [" + filename + "] is not available at [" + initSyncDirectory + "/offline-concepts]");
        }

        try {
            Resource resource = resourceLoader.getResource("file:" + filePath);
            IOUtils.copy(new FileInputStream(resource.getFile()), response.getOutputStream());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Encoding", "gzip");
            response.flushBuffer();
        } catch (IOException e) {
            throw new APIException("Cannot parse the offline-concepts file at location [" + filePath + "]. Error [" + e.getMessage() + "]", e);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/addresshierarchy", params = {"filename"})
    @ResponseBody
    public void getAddressHierarchyByFilter(HttpServletResponse response, @RequestParam(value = "filename") String filename) {
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        String filePath = String.format("%s/addressHierarchy/%s", initSyncDirectory, filename);
        File initSyncFile = new File(filePath);

        if (!initSyncFile.exists()) {
            throw new APIException("File [" + filename + "] is not available at [" + initSyncDirectory + "/addressHierarchy]");
        }

        try {
            Resource resource = resourceLoader.getResource("file:" + filePath);
            IOUtils.copy(new FileInputStream(resource.getFile()), response.getOutputStream());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Encoding", "gzip");
            response.flushBuffer();
        } catch (IOException e) {
            throw new APIException("Cannot parse the addressHierarchy file at location [" + filePath + "]. Error [" + e.getMessage() + "]", e);
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

}
