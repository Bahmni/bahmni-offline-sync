package org.bahmni.module.bahmniOfflineSync.advice;

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
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.openmrs.module.bahmni.ie.apps.model.BahmniForm;
import org.openmrs.module.bahmni.ie.apps.model.BahmniFormResource;
import org.openmrs.module.bahmni.ie.apps.service.BahmniFormService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Method;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class BahmniFormServiceInterceptorTest {
    @Mock
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    @Mock
    private EventService eventService;
    @Mock
    private BahmniForm bahmniForm;

    private BahmniFormServiceInterceptor bahmniFormServiceInterceptor;

    private ArgumentCaptor<AFTransactionWorkWithoutResult> captor = ArgumentCaptor.forClass(AFTransactionWorkWithoutResult.class);
    private Method publishMethod;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Context.class);
        bahmniFormServiceInterceptor = new BahmniFormServiceInterceptor(atomFeedSpringTransactionManager, eventService);
        publishMethod = BahmniFormService.class.getMethod("publish", String.class);
        when(bahmniForm.getUuid()).thenReturn("test-uuid");
    }

    @Test
    public void testShouldPublishUpdateEventToFeedAfterPublishingAnyForm() throws Throwable {
        bahmniFormServiceInterceptor.afterReturning(bahmniForm, publishMethod, null, null);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }

    @Test
    public void testShouldSaveEventInTheSameTransactionAsTheTrigger() throws Throwable {
        bahmniFormServiceInterceptor.afterReturning(bahmniForm, publishMethod, null, null);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
        verify(atomFeedSpringTransactionManager).executeWithTransaction(captor.capture());
        assertEquals(AFTransactionWork.PropagationDefinition.PROPAGATION_REQUIRED, captor.getValue().getTxPropagationDefinition());
    }

    @Test
    public void testShouldCreateEventsForNewlyPublishedForm() throws Throwable {
        bahmniFormServiceInterceptor.afterReturning(bahmniForm, publishMethod, null, null);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(captor.capture());
        captor.getValue().execute();

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService, times(1)).notify(eventArgumentCaptor.capture());

        Event capturedEvent = eventArgumentCaptor.getAllValues().get(0);
        assertEquals("/openmrs/ws/rest/v1/form/test-uuid?v=custom:(resources:(value),name,version,uuid)", capturedEvent.getContents());
        assertEquals("Forms", capturedEvent.getTitle());
        assertEquals("forms", capturedEvent.getCategory());
        assertNotNull(capturedEvent.getUuid());
    }

    @Test
    public void testShouldNotCreateFeedForAnyOtherActionExceptPublishForm() throws Throwable {
        bahmniFormServiceInterceptor.afterReturning(bahmniForm, BahmniFormService.class.getMethod("saveFormResource", BahmniFormResource.class), null, null);
        verify(atomFeedSpringTransactionManager, never()).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }
}