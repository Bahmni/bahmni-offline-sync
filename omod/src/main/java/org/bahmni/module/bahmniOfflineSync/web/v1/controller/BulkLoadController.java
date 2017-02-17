package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/bulk")
public class BulkLoadController {

    public static final String GP_BAHMNICONNECT_INIT_SYNC_PATH = "bahmniconnect.initsync.directory";
    public static final String DEFAULT_INIT_SYNC_PATH = "/home/bahmni/init_sync";

    public BulkLoadController() {

    }

    @RequestMapping(method = RequestMethod.GET, value = "/patient", params = {"filter"})
    @ResponseBody
    public List<SimpleObject> getPatientsInBulk(@RequestParam(value = "filter") String filter) {
        String initSyncDirectory = Context.getAdministrationService().getGlobalProperty(GP_BAHMNICONNECT_INIT_SYNC_PATH, DEFAULT_INIT_SYNC_PATH);
        String filePath = String.format("%s/patient/%s.json", initSyncDirectory, filter);
        File initSyncFile = new File(filePath);

        if (!initSyncFile.exists()) {
            throw new APIException("Bulk patient file is not available at [" + initSyncDirectory + "/patient] for [" + filter + "]");
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(initSyncFile, new TypeReference<List<SimpleObject>>() {});
        } catch (IOException e) {
            throw new APIException("Cannot parse the patient file at location [" + filePath + "]", e);
        }
    }

}
