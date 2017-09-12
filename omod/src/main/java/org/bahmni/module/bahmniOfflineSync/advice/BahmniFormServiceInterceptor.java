package org.bahmni.module.bahmniOfflineSync.advice;


import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.openmrs.module.bahmni.ie.apps.model.BahmniForm;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class BahmniFormServiceInterceptor implements AfterReturningAdvice {
    private static final String TEMPLATE = "/openmrs/ws/rest/v1/form/%s?v=custom:(resources:(value),name,version,uuid)";
    public static final String CATEGORY = "forms";
    public static final String TITLE = "Forms";
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    private EventService eventService;

    public BahmniFormServiceInterceptor() {
        atomFeedSpringTransactionManager = createTransactionManager();
        this.eventService = createService(atomFeedSpringTransactionManager);
    }

    public BahmniFormServiceInterceptor(AtomFeedSpringTransactionManager atomFeedSpringTransactionManager, EventService eventService) {
        this.atomFeedSpringTransactionManager = atomFeedSpringTransactionManager;
        this.eventService = eventService;
    }

    private AtomFeedSpringTransactionManager createTransactionManager() {
        PlatformTransactionManager platformTransactionManager = getSpringPlatformTransactionManager();
        return new AtomFeedSpringTransactionManager(platformTransactionManager);
    }

    private List operations() {
        return Collections.singletonList("publish");
    }

    private EventServiceImpl createService(AtomFeedSpringTransactionManager atomFeedSpringTransactionManager) {
        AllEventRecordsQueue allEventRecordsQueue = new AllEventRecordsQueueJdbcImpl(atomFeedSpringTransactionManager);
        return new EventServiceImpl(allEventRecordsQueue);
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] arguments, Object target) throws Throwable {
        List<Event> events = new ArrayList<>();
        if (operations().contains(method.getName())) {
            BahmniForm bahmniForm = (BahmniForm) returnValue;
            String contents = String.format(TEMPLATE, bahmniForm.getUuid());
            Event event = new Event(UUID.randomUUID().toString(), TITLE, null, (URI) null, contents, CATEGORY);
            events.add(event);
        }
        if (isNotEmpty(events)) {
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
    }

    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }
}