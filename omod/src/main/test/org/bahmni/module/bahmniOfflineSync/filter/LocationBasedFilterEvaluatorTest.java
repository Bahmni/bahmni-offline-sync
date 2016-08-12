package org.bahmni.module.bahmniOfflineSync.filter;

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

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class LocationBasedFilterEvaluatorTest {
    private LocationBasedFilterEvaluator locationBasedFilterEvaluator ;

    @Mock
    private PatientService patientService;
    @Mock
    private EncounterService encounterService;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    private AddressHierarchyEntry addressHierarchyEntry;
    private Patient patient;
    Encounter encounter;
    private String encounterUuid = "encounterUuid";
    private String patientUuid = "patientUuid";
    private String addressUuid = "addressUuid";
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getPatientService()).thenReturn(patientService);
        Mockito.when(Context.getEncounterService()).thenReturn(encounterService);
        locationBasedFilterEvaluator = new LocationBasedFilterEvaluator();
        patient = new Patient();
        patient.setUuid(patientUuid);
        encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setUuid(encounterUuid);
        PersonAttributeType pat= new PersonAttributeType();
        pat.setName("addressCode");
        PersonAttribute pa = new PersonAttribute(pat,"202020");
        Set personAttributes =  new HashSet();
        personAttributes.add(pa);
        patient.setAttributes(personAttributes);
        addressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setUuid(addressUuid);
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
    }



    @Test
    public void shouldEvaluateFilterForAddressHierarchyForTop3Levels() throws Exception {

        AddressHierarchyLevel ahl = new AddressHierarchyLevel();
        ahl.setLevelId(1);
        addressHierarchyEntry.setLevel(ahl);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(addressUuid)).thenReturn(addressHierarchyEntry);

        assertEquals("",locationBasedFilterEvaluator.evaluateFilter(addressUuid, "addressHierarchy"));
        verify(addressHierarchyService, times(1)).getAddressHierarchyEntryByUuid(addressUuid);
    }

    @Test
    public void shouldEvaluateFilterForAddressHierarchyForLowersLevels() throws Exception {
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setUserGeneratedId("Geocode");
        AddressHierarchyLevel ahl = new AddressHierarchyLevel();
        ahl.setLevelId(4);
        addressHierarchyEntry.setLevel(ahl);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        when(addressHierarchyService.getAddressHierarchyEntryByUuid("addressUuid")).thenReturn(addressHierarchyEntry);
        String filter = locationBasedFilterEvaluator.evaluateFilter("addressUuid", "addressHierarchy");
        verify(addressHierarchyService, times(1)).getAddressHierarchyEntryByUuid("addressUuid");
        assertEquals(addressHierarchyEntry.getUserGeneratedId(), filter);
    }


    @Test
    public void shouldNotSetFilterIfLevelIdIsNull() throws Exception {
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setUserGeneratedId("Geocode");
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        when(addressHierarchyService.getAddressHierarchyEntryByUuid("addressUuid")).thenReturn(addressHierarchyEntry);
        assertEquals("",locationBasedFilterEvaluator.evaluateFilter("addressUuid", "addressHierarchy"));
        verify(addressHierarchyService, times(1)).getAddressHierarchyEntryByUuid("addressUuid");
    }


    @Test
    public void shouldNotSetFilterIfUuidIsNull() throws Exception {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        assertEquals("",locationBasedFilterEvaluator.evaluateFilter(null, "addressHierarchy"));
        verify(addressHierarchyService, times(0)).getAddressHierarchyEntryByUuid("addressUuid");
    }


    @Test
    public void shouldEvaluateFilterForEncounter() {

        when(encounterService.getEncounterByUuid(encounterUuid)).thenReturn(encounter);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);


        String filter = locationBasedFilterEvaluator.evaluateFilter(encounterUuid, "Encounter");

        verify(encounterService, times(1)).getEncounterByUuid(encounterUuid);
        assertEquals("202020", filter);
    }

    @Test
    public void shouldReturnEmptyFilterIfEncounterUuidIsNull() throws Exception {
        PersonAttribute personAttribute = new PersonAttribute();
        personAttribute.setValue("Value");
        String filter = locationBasedFilterEvaluator.evaluateFilter(null, "Encounter");

        verify(patientService, never()).getPatientByUuid(patientUuid);
        assertEquals("",filter);
    }

    @Test
    public void shouldRetrunEmptyStringIfEncountersPatientAttributeIsNotAvailable() throws Exception {
        Patient patient = new Patient();
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        when(encounterService.getEncounterByUuid(encounterUuid)).thenReturn(encounter);
        encounter.setPatient(patient);

        String filter = locationBasedFilterEvaluator.evaluateFilter(encounterUuid, "Encounter");

        verify(patientService, times(1)).getPatientByUuid(anyString());
        assertEquals("", filter);
    }


    @Test
    public void shouldEvaluateFilterForPatient() {
        when(patientService.getPatientByUuid(patientUuid)).thenReturn(patient);
        String filter = locationBasedFilterEvaluator.evaluateFilter(patientUuid, "patient");
        verify(patientService, times(1)).getPatientByUuid(patientUuid);
        assertEquals("202020", filter);
    }

    @Test
    public void shouldReturnEmptyStringIfUuidIsNull() throws Exception {
        String filter = locationBasedFilterEvaluator.evaluateFilter(null, "patient");
        assertEquals("", filter);
    }

    @Test
    public void shouldReturnEmptyStringIfAttributeIsNotAvailable() throws Exception {
        when(patientService.getPatientByUuid(anyString())).thenReturn(new Patient());
        String filter = locationBasedFilterEvaluator.evaluateFilter(patientUuid, "patient");

        verify(patientService, times(1)).getPatientByUuid(patientUuid);
        assertEquals("", filter);
    }

    @Test
    public void shouldGetCategoryList() throws Exception {
        List<String> categories = locationBasedFilterEvaluator.getEventCategoriesList();
        assertTrue(categories.contains("TransactionalData"));
        assertTrue(categories.contains("AddressHierarchy"));
        assertTrue(categories.contains("ParentAddressHierarchy"));
        assertTrue(categories.contains("offline-concepts"));
        assertTrue(categories.size()==4);
    }

    @Test
    public void shouldGetFilterForDevice() throws Exception {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);

        Map<String,String> markers =  locationBasedFilterEvaluator.getFilterForDevice("providerUuid","locationUuid");
        Map categoryFilterMap = new HashMap();
        categoryFilterMap.put("TransactionalData", "202020");
        categoryFilterMap.put("AddressHierarchy","202020");
        categoryFilterMap.put("ParentAddressHierarchy", null);
        categoryFilterMap.put("offline-concepts", null);
        assertEquals(markers,categoryFilterMap);

    }
}