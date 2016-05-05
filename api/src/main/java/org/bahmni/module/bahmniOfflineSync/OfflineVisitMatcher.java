package org.bahmni.module.bahmniOfflineSync;

import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.module.bahmniemrapi.encountertransaction.service.VisitMatcher;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class OfflineVisitMatcher implements VisitMatcher {
    @Override
    public Visit getVisitFor(Patient patient, String s, Date date, Date date1, Date date2) {
        return null;
    }

    @Override
    public Visit getVisitFor(Patient patient, String s, Date date) {
        return null;
    }

    @Override
    public boolean hasActiveVisit(Patient patient) {
        return false;
    }

    @Override
    public Visit createNewVisit(Patient patient, Date date, String s, Date date1, Date date2) {
        return null;
    }

    @Override
    public VisitType getVisitTypeByName(String s) {
        return null;
    }
}
