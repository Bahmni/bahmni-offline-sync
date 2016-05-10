package org.bahmni.module.bahmniOfflineSync;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.VisitService;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OfflineVisitMatcherTest{

    @Mock
    VisitService visitService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void ShouldMatchActiveVisitByVisitType(){

        OfflineVisitMatcher offlineVisitMatcher = new OfflineVisitMatcher(visitService);

        Date visitEndDate = null;
        Patient patient = new Patient();
        String visitType = "FIELD";
        Date orderDate = new Date(1459747800000L);
        Date visitStartDate = new Date(1459747700000L);

        BahmniEncounterTransaction bahmniEncounterTransaction = new BahmniEncounterTransaction(new EncounterTransaction());
        bahmniEncounterTransaction.setEncounterDateTime(orderDate);
        bahmniEncounterTransaction.setVisitType(visitType);

        List<VisitType> visitTypes = new ArrayList<VisitType>();
        visitTypes.add(new VisitType("FIELD", "field visit"));

        List<Visit> visits = new ArrayList<Visit>();
        Visit visit = new Visit(1);
        visit.setStartDatetime(visitStartDate);
        visit.setVisitType(visitTypes.get(0));
        visits.add(visit);

        when(visitService.getActiveVisitsByPatient(patient)).thenReturn(visits);

        offlineVisitMatcher.createOrStretchVisit(bahmniEncounterTransaction, patient, visitStartDate,visitEndDate);

        verify(visitService, times(0)).endVisit(any(Visit.class), any(Date.class));
        verify(visitService, times(0)).saveVisit(any(Visit.class));


    }

    @Test
    public void ShouldEndVisitIfVisitTypeOfActiveVisitDoesNotMatch(){
        OfflineVisitMatcher offlineVisitMatcher = new OfflineVisitMatcher(visitService);
        Date visitEndDate = null;
        Patient patient = new Patient();
        String visitType = "FIELD";
        Date orderDate = new Date(1459747800000L);
        Date visitStartDate = new Date(1459747700000L);

        BahmniEncounterTransaction bahmniEncounterTransaction = new BahmniEncounterTransaction();
        bahmniEncounterTransaction.setEncounterDateTime(orderDate);
        bahmniEncounterTransaction.setVisitType(visitType);

        List<Visit> visits = new ArrayList<Visit>();
        Visit visit = new Visit(1);
        visit.setStartDatetime(visitStartDate);
        visit.setVisitType(new VisitType("outpatient", "description"));
        visits.add(visit);

        ArgumentCaptor<Visit> captor = ArgumentCaptor.forClass(Visit.class);

        List<VisitType> visitTypes = new ArrayList<VisitType>();
        visitTypes.add(new VisitType("FIELD", "field visit"));

        when(visitService.getVisitTypes(visitType)).thenReturn(visitTypes);
        when(visitService.getActiveVisitsByPatient(patient)).thenReturn(visits);

        offlineVisitMatcher.createOrStretchVisit(bahmniEncounterTransaction, patient, visitStartDate,visitEndDate);

        verify(visitService, times(1)).endVisit(any(Visit.class), any(Date.class));
        verify(visitService).saveVisit(captor.capture());
        assertEquals(captor.getValue().getVisitType().getName(), visitType);


    }

    @Test
    public void ShouldCreateVisitsIfThereAreNOActiveVisit(){
        OfflineVisitMatcher offlineVisitMatcher = new OfflineVisitMatcher(visitService);

        Date visitEndDate = null;
        Patient patient = new Patient();
        String visitType = "FIELD";
        Date orderDate = new Date(1459747800000L);
        Date visitStartDate = new Date(1459747700000L);

        BahmniEncounterTransaction bahmniEncounterTransaction = new BahmniEncounterTransaction();
        bahmniEncounterTransaction.setEncounterDateTime(orderDate);
        bahmniEncounterTransaction.setVisitType(visitType);

        List<Visit> visits = new ArrayList<Visit>();


        ArgumentCaptor<Visit> captor = ArgumentCaptor.forClass(Visit.class);

        List<VisitType> visitTypes = new ArrayList<VisitType>();
        visitTypes.add(new VisitType("FIELD", "field visit"));

        when(visitService.getVisitTypes(visitType)).thenReturn(visitTypes);
        when(visitService.getActiveVisitsByPatient(patient)).thenReturn(visits);

        offlineVisitMatcher.createOrStretchVisit(bahmniEncounterTransaction, patient, visitStartDate,visitEndDate);

        verify(visitService).saveVisit(captor.capture());
        assertEquals(captor.getValue().getVisitType().getName(), visitType);


    }

}
