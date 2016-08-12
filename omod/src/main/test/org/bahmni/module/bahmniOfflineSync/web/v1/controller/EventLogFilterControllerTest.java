package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import org.bahmni.module.bahmniOfflineSync.factory.EventLogFilterEvaluatorFactory;
import org.bahmni.module.bahmniOfflineSync.filter.LocationBasedFilterEvaluator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;
@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class EventLogFilterControllerTest {

    @InjectMocks
    private EventLogFilterController controller;
    @Mock
    private AdministrationService administrationService;
    @Mock
    private EventLogFilterEvaluatorFactory eventLogFilterFactory;
    @Mock
    private PatientService patientService;
    @Mock
    private EncounterService encounterService;
    @Mock
    private AddressHierarchyService addressHierarchyService;

    private LocationBasedFilterEvaluator locationFilterEvaluator;

    AddressHierarchyEntry addressHierarchyEntry;
    Patient patient;
    Encounter encounter;

    @Before
    public void setup() {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getPatientService()).thenReturn(patientService);
        Mockito.when(Context.getEncounterService()).thenReturn(encounterService);
        locationFilterEvaluator = new LocationBasedFilterEvaluator();
        patient = new Patient();
        patient.setUuid("patientUuid");
        encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setUuid("encounterUuid");
        PersonAttributeType pat= new PersonAttributeType();
        pat.setName("addressCode");
        PersonAttribute pa = new PersonAttribute(pat,"202020");
        Set personAttributes =  new HashSet();
        personAttributes.add(pa);
        patient.setAttributes(personAttributes);
        addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");


    }

    @Test
    public void shouldGetFilterForPatient() throws Exception {
        when(eventLogFilterFactory.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationFilterEvaluator);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        String filter = controller.getFilter("patientUuid","Patient");
        assertEquals("202020",filter);
    }

    @Test
    public void shouldGetFilterForEncounter() throws Exception {
        when(eventLogFilterFactory.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationFilterEvaluator);
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(encounter);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        String filter = controller.getFilter("encounterUuid","Encounter");
        assertEquals("202020",filter);
    }

    @Test
    public void shouldGetFilterForAddressHierarchy() throws Exception {
        AddressHierarchyLevel ahl = new AddressHierarchyLevel();
        ahl.setLevelId(6);
        addressHierarchyEntry.setLevel(ahl);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        when(eventLogFilterFactory.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationFilterEvaluator);
        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);
        String filter = controller.getFilter("addressHierarchyUuid","AddressHierarchy");
        assertEquals("202020",filter);
    }

    @Test
    public void shouldGetFilterForDevice() throws Exception {
        when(eventLogFilterFactory.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationFilterEvaluator);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);

        Map<String,String> markers =  controller.getFilterForDevice("providerUuid","locationUuid");
        Map categoryFilterMap = new HashMap();
        categoryFilterMap.put("TransactionalData", "202020");
        categoryFilterMap.put("AddressHierarchy","202020");
        categoryFilterMap.put("ParentAddressHierarchy", null);
        categoryFilterMap.put("offline-concepts", null);
        assertEquals(markers,categoryFilterMap);

    }

    @Test
    public void shouldGetCategoryList() throws Exception {
        when(eventLogFilterFactory.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationFilterEvaluator);

        List<String> categories = controller.getCategoryList();
        assertTrue(categories.contains("TransactionalData"));
        assertTrue(categories.contains("AddressHierarchy"));
        assertTrue(categories.contains("ParentAddressHierarchy"));
        assertTrue(categories.contains("offline-concepts"));
        assertTrue(categories.size()==4);
    }


    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenGlobalPropertyIsNotConfigured() throws Exception {
        when(administrationService.getGlobalProperty(anyString())).thenReturn(new String());
        controller.getCategoryList();
    }

}