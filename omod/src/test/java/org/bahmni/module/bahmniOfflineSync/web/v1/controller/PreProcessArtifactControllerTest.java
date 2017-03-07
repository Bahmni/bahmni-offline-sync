package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import org.bahmni.module.bahmniOfflineSync.job.InitialSyncArtifactsPublisher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.*;

@PrepareForTest({Context.class, PreProcessArtifactController.class})
@RunWith(PowerMockRunner.class)
public class PreProcessArtifactControllerTest {

    @Mock
    HttpServletRequest httpServletRequest;

    @Mock
    InitialSyncArtifactsPublisher initialSyncArtifactsPublisher;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private PreProcessArtifactController preProcessArtifactController;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        whenNew(InitialSyncArtifactsPublisher.class).withNoArguments().thenReturn(initialSyncArtifactsPublisher);
        when(httpServletRequest.getParameter("username")).thenReturn("username");
        when(httpServletRequest.getParameter("password")).thenReturn("password");
        preProcessArtifactController = new PreProcessArtifactController();
    }

    @Test
    public void createArtifactsShouldThrowAPIExceptionWhenUserIsNotAuthenticated() throws Exception {
        doThrow(new ContextAuthenticationException()).when(Context.class);
        Context.authenticate("username", "password");
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        thrown.expect(APIAuthenticationException.class);
        thrown.expectMessage("User username is not authenticate");
        preProcessArtifactController.createArtifacts(httpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void createArtifactsShouldTriggerInitArtifactPublisherWhenUserIsAuthenticated() throws Exception {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        preProcessArtifactController.createArtifacts(httpServletRequest, mockHttpServletResponse);
        doNothing().when(initialSyncArtifactsPublisher).execute();
        assertEquals(200, mockHttpServletResponse.getStatus());
        verifyNew(InitialSyncArtifactsPublisher.class).withNoArguments();
        verify(initialSyncArtifactsPublisher, times(1)).execute();
    }
}
