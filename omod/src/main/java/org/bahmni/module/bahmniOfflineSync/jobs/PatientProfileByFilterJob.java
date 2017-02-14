package org.bahmni.module.bahmniOfflineSync.jobs;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.bahmni.module.bahmniOfflineSync.utils.EventRecordServiceHelper;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PatientProfileByFilterJob extends AbstractTask {
    @Autowired
    private EventRecordServiceHelper eventRecordServiceHelper;

    @Override
    public void execute() {
        List<EventLog> eventLogList = eventRecordServiceHelper.getDistinctEventLogsByCategory("patient", "object");
        Stream<EventLog> eventLogStream = eventLogList.stream();
        Map<String, List<String>> patientsUUid=eventLogStream.filter(p->p.getFilter()!=null)
                .collect(Collectors.groupingBy(p->p.getFilter(),
                Collectors.mapping( (EventLog p) -> p.getObject(), Collectors.toList())));
        patientsUUid.forEach((k,v) -> process(k,v));
    }
    private void process(String filter, List<String> patientUrls) {
        for (String url : patientUrls) {
            Patient patient= getPatientProfile(url);
        }
    }
    private Patient getPatientProfile(String url) {
        PatientService patientService = Context.getPatientService();
        return patientService.getPatientByUuid(url);
    }
}
