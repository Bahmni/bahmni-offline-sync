package org.bahmni.module.bahmniOfflineSync;

import org.aopalliance.intercept.MethodInvocation;
import org.bahmni.module.bahmniOfflineSync.advice.ConceptServiceInterceptor;
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
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptSet;
import org.openmrs.api.ConceptService;
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
public class ConceptServiceInterceptorTest {
    @Mock
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    @Mock
    private EventService eventService;
    @Mock
    private ConceptService conceptService;
    @Mock
    private MethodInvocation methodInvocation;

    private ArgumentCaptor<AFTransactionWorkWithoutResult> captor = ArgumentCaptor.forClass(AFTransactionWorkWithoutResult.class);

    private ConceptServiceInterceptor publishedFeed;

    private Concept concept;


    @Before
    public void setup() throws NoSuchMethodException {
        MockitoAnnotations.initMocks(this);

        Locale defaultLocale = new Locale("en", "GB");
        concept = new Concept();
        String uuid = "uuid";
        concept.setUuid(uuid);
        concept.setPreferredName(new ConceptName("name", defaultLocale));

        PowerMockito.mockStatic(Context.class);
        when(Context.getConceptService()).thenReturn(conceptService);
        PowerMockito.when(Context.getLocale()).thenReturn(defaultLocale);

        Object[] arguments = {concept};
        when(methodInvocation.getArguments()).thenReturn(arguments);

        publishedFeed = new ConceptServiceInterceptor(atomFeedSpringTransactionManager, eventService);
    }

    @Test
    public void shouldPublishUpdateEventToFeedAfterUpdateConceptOperation() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(ConceptService.class.getMethod("updateConcept", Concept.class));

        publishedFeed.invoke(methodInvocation);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }

    @Test
    public void shouldPublishUpdateEventToFeedAfterEveryUpdateConceptOperation() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(ConceptService.class.getMethod("updateConcept", Concept.class));
        int updates = 2;
        for (int i = 0; i < updates; i++) {
            publishedFeed.invoke(methodInvocation);
        }
        verify(atomFeedSpringTransactionManager, times(updates)).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }
    @Test
    public void shouldPublishUpdateEventToFeedAfterSaveConceptOperation() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(ConceptService.class.getMethod("saveConcept", Concept.class));

        publishedFeed.invoke(methodInvocation);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }

    @Test
    public void shouldPublishUpdateEventToFeedAfterEverySaveConceptOperation() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(ConceptService.class.getMethod("saveConcept", Concept.class));

        int updates = 2;
        for (int i = 0; i < updates; i++) {
            publishedFeed.invoke(methodInvocation);
        }
        verify(atomFeedSpringTransactionManager, times(updates)).executeWithTransaction(any(AFTransactionWorkWithoutResult.class));
    }

    @Test
    public void shouldSaveEventInTheSameTransactionAsTheTrigger() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(ConceptService.class.getMethod("saveConcept", Concept.class));

        publishedFeed.invoke(methodInvocation);
        verify(atomFeedSpringTransactionManager).executeWithTransaction(captor.capture());

        assertEquals(AFTransactionWork.PropagationDefinition.PROPAGATION_REQUIRED, captor.getValue().getTxPropagationDefinition());
    }

    @Test
    public void shouldCreateEventsForNewlyAddedSetMembersOfOfflineConcepts() throws Throwable {
        when(methodInvocation.getMethod()).thenReturn(ConceptService.class.getMethod("saveConcept", Concept.class));
        concept.setPreferredName(new ConceptName("Offline Concepts", new Locale("en", "GB")));

        Concept childConcept = new Concept();
        childConcept.setPreferredName(new ConceptName("Child1", new Locale("en", "GB")));
        childConcept.setUuid("childUuid1");

        Concept childConcept2 = new Concept();
        childConcept2.setPreferredName(new ConceptName("Child2", new Locale("en", "GB")));
        childConcept2.setUuid("childUuid2");

        final ConceptSet child1 = new ConceptSet();
        child1.setConcept(childConcept);

        final ConceptSet child2 = new ConceptSet();
        child2.setConcept(childConcept2);

        ArrayList<ConceptSet> setMembers = new ArrayList<ConceptSet>();
        setMembers.add(child1);
        setMembers.add(child2);
        concept.setConceptSets(setMembers);

        doAnswer(new Answer<Void>(){
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                child1.setConceptSetId(1);
                child2.setConceptSetId(2);
                return null;
            }
        }).when(methodInvocation).proceed();

        publishedFeed.invoke(methodInvocation);

        verify(atomFeedSpringTransactionManager).executeWithTransaction(captor.capture());

        captor.getValue().execute();

        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService, times(3)).notify(eventArgumentCaptor.capture());

        List<Event> capturedEvents = eventArgumentCaptor.getAllValues();

        assertTrue(capturedEvents.get(0).getUri().toString().equals("/openmrs/ws/rest/v1/concept/childUuid1?s=byFullySpecifiedName&v=bahmni&name=Child1"));
        assertTrue(capturedEvents.get(1).getUri().toString().equals("/openmrs/ws/rest/v1/concept/childUuid2?s=byFullySpecifiedName&v=bahmni&name=Child2"));
        assertTrue(capturedEvents.get(2).getUri().toString().equals("/openmrs/ws/rest/v1/concept/uuid?s=byFullySpecifiedName&v=bahmni&name=Offline+Concepts"));
    }

}