package org.bahmni.module.bahmniOfflineSync;

import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.bahmniemrapi.encountertransaction.service.VisitIdentificationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Primary
@Component
public class OfflineVisitMatcher extends VisitIdentificationHelper{

    @Autowired
    public OfflineVisitMatcher(VisitService visitService) {
        super(visitService);
    }

    @Override
    public void createOrStretchVisit(BahmniEncounterTransaction bahmniEncounterTransaction, Patient patient, Date visitStartDate, Date visitEndDate) {
        String visitTypeForNewVisit = bahmniEncounterTransaction.getVisitType();
        Date orderDate = bahmniEncounterTransaction.getEncounterDateTime();
        List<Visit> activeVisits = visitService.getActiveVisitsByPatient(patient);
        if (matchingVisitsFound(activeVisits)) {
            if(activeVisits.get(0).getVisitType().getName().equals(visitTypeForNewVisit)) {
                Visit matchingVisit = getVisit(orderDate, activeVisits);
                stretchVisits(orderDate, matchingVisit);
                return;
            }
            else {
                visitService.endVisit(activeVisits.get(0), new Date());
            }
        }
        createNewVisit(patient, orderDate, visitTypeForNewVisit, visitStartDate, visitEndDate);
    }



}
