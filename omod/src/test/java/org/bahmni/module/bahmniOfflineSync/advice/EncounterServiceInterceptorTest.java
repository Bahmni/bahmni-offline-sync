package org.bahmni.module.bahmniOfflineSync.advice;

import org.aopalliance.intercept.MethodInvocation;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.transaction.AFTransactionWork;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class EncounterServiceInterceptorTest {
    @Mock
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    @Mock
    private EventService eventService;
    @Mock
    private MethodInvocation methodInvocation;

    private ArgumentCaptor<AFTransactionWorkWithoutResult> captor = ArgumentCaptor.forClass(AFTransactionWorkWithoutResult.class);

    private EncounterServiceInterceptor publishedFeed;

    private Encounter encounter;


    @Before
    public void setup() throws NoSuchMethodException {
        MockitoAnnotations.initMocks(this);
        Patient patient = new Patient();
        patient.setUuid("puuid");
        Locale defaultLocale = new Locale("en", "GB");
        encounter = new Encounter();
        String uuid = "uuid";
        encounter.setUuid(uuid);
        encounter.setPatient(patient);
        PowerMockito.mockStatic(Context.class);
        PowerMockito.when(Context.getLocale()).thenReturn(defaultLocale);

        Object[] arguments = {encounter};
        when(methodInvocation.getArguments()).thenReturn(arguments);

        publishedFeed = new EncounterServiceInterceptor(atomFeedSpringTransactionManager, eventService);
    }

    @Test
    public void shouldPublishEventWhenSaveIsCalled() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(EncounterService.class.getMethod("saveEncounter", Encounter.class));
        EncounterType encounterType = new EncounterType("LAB_RESULT", "lab order");
        encounter.setEncounterType(encounterType);
        publishedFeed.invoke(methodInvocation);
        verify(atomFeedSpringTransactionManager,times(1)).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }

    @Test
    public void shouldNotPublishUpdateEventWhenEncounterTypeIsNull() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(EncounterService.class.getMethod("saveEncounter", Encounter.class));
        publishedFeed.invoke(methodInvocation);
        verify(atomFeedSpringTransactionManager, times(0)).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }


    @Test
    public void shouldNotPublishUpdateEventWhenEncounterTypeIsNotLabResult() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(EncounterService.class.getMethod("saveEncounter", Encounter.class));
        EncounterType encounterType = new EncounterType("LAB", "lab order");
        encounter.setEncounterType(encounterType);
        publishedFeed.invoke(methodInvocation);
        verify(atomFeedSpringTransactionManager, times(0)).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }

    @Test
    public void shouldSaveEventInTheSameTransactionAsTheTrigger() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(EncounterService.class.getMethod("saveEncounter", Encounter.class));
        EncounterType encounterType = new EncounterType("LAB_RESULT", "lab order");
        encounter.setEncounterType(encounterType);
        publishedFeed.invoke(methodInvocation);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(captor.capture());

        assertEquals(AFTransactionWork.PropagationDefinition.PROPAGATION_REQUIRED, captor.getValue().getTxPropagationDefinition());
    }

}