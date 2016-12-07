package org.bahmni.module.bahmniOfflineSync.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;

public class EncounterServiceInterceptor  implements MethodInterceptor {
    private static final String LAB_ORDER_RESULTS_URL = "/openmrs/ws/rest/v1/bahmnicore/labOrderResults?patientUuid=%s";

    private final String category = "LabOrderResults";
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    private EventService eventService;

    public EncounterServiceInterceptor() {
        atomFeedSpringTransactionManager = createTransactionManager();
        this.eventService = createService(atomFeedSpringTransactionManager);
    }

    public EncounterServiceInterceptor(AtomFeedSpringTransactionManager atomFeedSpringTransactionManager, EventService eventService) {
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
        return asList("saveEncounter", "retireEncounter", "purgeEncounter");
    }


    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final List<Event> events = new ArrayList<Event>();
        Object[] arguments = invocation.getArguments();
        Object o = invocation.proceed();
        if(operations().contains(invocation.getMethod().getName())) {
            Encounter encounter = (Encounter) arguments[0];
            EncounterType encounterType = encounter.getEncounterType();
            if(encounterType != null && "LAB_RESULT".equals(encounterType.getName())) {
                Patient patient = encounter.getPatient();
                String url = String.format(LAB_ORDER_RESULTS_URL, patient.getUuid());

                events.add(new Event(UUID.randomUUID().toString(), "Lab Order Results", null, url, url, category));

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

        return o;
    }

}
