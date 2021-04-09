package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import org.bahmni.module.bahmniOfflineSync.job.InitialSyncArtifactsPublisher;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/bahmniconnect/")
public class PreProcessArtifactController {

    @RequestMapping(method = RequestMethod.POST, value = "/initsync")
    @ResponseBody
    public void createArtifacts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        try {
            Context.authenticate(username, password);
            response.setStatus(200);
        } catch (ContextAuthenticationException e) {
            response.setStatus(401);
            throw new APIAuthenticationException(String.format("User %s is not authenticate", username));
        } finally {
            response.flushBuffer();
        }

        InitialSyncArtifactsPublisher initialSyncArtifactsPublisher = new InitialSyncArtifactsPublisher();
        initialSyncArtifactsPublisher.setUserName(username);
        initialSyncArtifactsPublisher.setPassword(password);
        initialSyncArtifactsPublisher.execute();
    }
}
