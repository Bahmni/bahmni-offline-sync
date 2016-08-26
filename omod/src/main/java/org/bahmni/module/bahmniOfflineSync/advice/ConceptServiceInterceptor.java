package org.bahmni.module.bahmniOfflineSync.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.ConceptSet;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;

public class ConceptServiceInterceptor implements MethodInterceptor {
    public static final String CONCEPT_NAME_URL = "/openmrs/ws/rest/v1/concept/%s?s=byFullySpecifiedName&v=bahmni&name=%s";

    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    private EventService eventService;

    public ConceptServiceInterceptor() {
        atomFeedSpringTransactionManager = createTransactionManager();
        this.eventService = createService(atomFeedSpringTransactionManager);
    }

    public ConceptServiceInterceptor(AtomFeedSpringTransactionManager atomFeedSpringTransactionManager, EventService eventService) {
        this.atomFeedSpringTransactionManager = atomFeedSpringTransactionManager;
        this.eventService = eventService;
    }

    private AtomFeedSpringTransactionManager createTransactionManager() {
        PlatformTransactionManager platformTransactionManager = getSpringPlatformTransactionManager();
        return new AtomFeedSpringTransactionManager(platformTransactionManager);
    }

    private EventServiceImpl createService(AtomFeedSpringTransactionManager atomFeedSpringTransactionManager) {
        AllEventRecordsQueue allEventRecordsQueue = new AllEventRecordsQueueJdbcImpl(atomFeedSpringTransactionManager);
        return new EventServiceImpl(allEventRecordsQueue);
    }

    private List<String> operations() {
        return asList("saveConcept", "retireConcept", "purgeConcept");
    }


    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final List<Event> events = new ArrayList<Event>();
        Object[] arguments = invocation.getArguments();
        List<ConceptSet> newlyAddedSetMembers = new ArrayList<ConceptSet>();
        if (operations().contains(invocation.getMethod().getName())) {
            Concept concept = (Concept) arguments[0];
            if (concept.getName(Context.getLocale()).getName().equals("Offline Concepts")) {
                for (ConceptSet setMember : concept.getConceptSets()) {
                    if (setMember.getConceptSetId() == null) {
                        newlyAddedSetMembers.add(setMember);
                    }
                }
            }
        }

        Object o = invocation.proceed();

        if (operations().contains(invocation.getMethod().getName())) {
            Concept concept = (Concept) arguments[0];
            for (ConceptSet setmember : newlyAddedSetMembers) {
                if (setmember.getConceptSetId() != null) {
                    String url = String.format(CONCEPT_NAME_URL, setmember.getConcept().getUuid(), setmember.getConcept().getName(Context.getLocale()).getName().replaceAll(" ", "+"));
                    events.add(new Event(UUID.randomUUID().toString(), "Offline Concepts", DateTime.now(), url, url, "offline-concepts"));
                }
            }

            String url = String.format(CONCEPT_NAME_URL, concept.getUuid(), concept.getName(Context.getLocale()).getName().replaceAll(" ", "+"));
            events.add(new Event(UUID.randomUUID().toString(), "concepts", DateTime.now(), url, url, "all-concepts"));

            atomFeedSpringTransactionManager.executeWithTransaction(
                    new AFTransactionWorkWithoutResult() {
                        @Override
                        protected void doInTransaction() {
                            for (Event event : events) {
                                eventService.notify(event);
                            }
                        }

                        @Override
                        public PropagationDefinition getTxPropagationDefinition() {
                            return PropagationDefinition.PROPAGATION_REQUIRED;
                        }
                    }
            );

        }

        return o;
    }
}
