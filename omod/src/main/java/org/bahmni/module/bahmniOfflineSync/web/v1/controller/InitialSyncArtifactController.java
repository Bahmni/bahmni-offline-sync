package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import org.apache.commons.io.IOUtils;
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

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/bahmniconnect")
public class InitialSyncArtifactController extends BaseRestController implements ResourceLoaderAware {

    public static final String GP_BAHMNICONNECT_INIT_SYNC_PATH = "bahmniconnect.initsync.directory";
    public static final String DEFAULT_INIT_SYNC_PATH = "/home/bahmni/init_sync";
    private ResourceLoader resourceLoader;

    @RequestMapping(method = RequestMethod.GET, value = "/patient", params = {"filter"})
    @ResponseBody
    public void getPatientsByFilter(HttpServletResponse response, @RequestParam(value = "filter") String filter) {
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        String filePath = String.format("%s/patient/%s.json.gz", initSyncDirectory, filter);
        File initSyncFile = new File(filePath);

        if (!initSyncFile.exists()) {
            throw new APIException("File is not available at [" + initSyncDirectory + "/patient] for [" + filter + "]");
        }

        try {
            Resource resource = resourceLoader.getResource("file:" + filePath);
            IOUtils.copy(new FileInputStream(resource.getFile()), response.getOutputStream());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Encoding","gzip");
            response.flushBuffer();
        } catch (IOException e) {
            throw new APIException("Cannot parse the patient file at location [" + filePath + "]. Error ["+ e.getMessage()+"]", e);
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
