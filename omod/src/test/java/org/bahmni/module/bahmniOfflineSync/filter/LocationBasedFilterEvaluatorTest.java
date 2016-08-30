package org.bahmni.module.bahmniOfflineSync.filter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
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
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class LocationBasedFilterEvaluatorTest {
    private LocationBasedFilterEvaluator locationBasedFilterEvaluator ;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private PatientService patientService;
    @Mock
    private EncounterService encounterService;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    @Mock
    private LocationService locationService;
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
        Mockito.when(Context.getLocationService()).thenReturn(locationService);
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
    public void shouldGetFilterForDeviceAsAllChildAddressWhenWardListIsNotDefined() throws Exception {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();

        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");

        List<AddressHierarchyEntry> childAddressHierarchyEntries = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry childAddressHierarchyEntry1 = new AddressHierarchyEntry();
        AddressHierarchyEntry childAddressHierarchyEntry2 = new AddressHierarchyEntry();
        childAddressHierarchyEntries.add(childAddressHierarchyEntry1);
        childAddressHierarchyEntries.add(childAddressHierarchyEntry2);
        childAddressHierarchyEntry1.setParent(addressHierarchyEntry);
        childAddressHierarchyEntry2.setParent(addressHierarchyEntry);
        childAddressHierarchyEntry1.setUserGeneratedId("20202001");
        childAddressHierarchyEntry2.setUserGeneratedId("20202002");

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);
        when(locationService.getLocationByUuid(anyString())).thenReturn(new org.openmrs.Location());
        when(locationService.getLocationAttributeTypeByName(anyString())).thenReturn(new LocationAttributeType());
        when(addressHierarchyService.getChildAddressHierarchyEntries(any(AddressHierarchyEntry.class))).thenReturn(childAddressHierarchyEntries);

        Map<String,List<String>> markers =  locationBasedFilterEvaluator.getFilterForDevice("providerUuid","addressUuid","locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> transactionalDataFilters = new ArrayList<String>();
        transactionalDataFilters.add("202020");
        transactionalDataFilters.add("20202001");
        transactionalDataFilters.add("20202002");
        ArrayList<String> addressHierarchyFilters = new ArrayList<String>();
        addressHierarchyFilters.add("202020");
        categoryFilterMap.put("TransactionalData", transactionalDataFilters);
        categoryFilterMap.put("AddressHierarchy",addressHierarchyFilters);
        categoryFilterMap.put("ParentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        assertEquals(categoryFilterMap,markers);
    }

    @Test
    public void shouldGetFilterForDeviceAsOnlyConfiguredFiltersWhenWardListIsDefined() throws Exception {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        Location loginLocation = new Location();
        LocationAttribute locationAttribute = new LocationAttribute();
        LocationAttributeType locationAttributeType = new LocationAttributeType();

        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
        locationAttribute.setVoided(Boolean.FALSE);
        locationAttribute.setAttributeType(locationAttributeType);
        locationAttribute.setValue("20202002");
        loginLocation.setAttribute(locationAttribute);

        List<AddressHierarchyEntry> childAddressHierarchyEntries = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry childAddressHierarchyEntry1 = new AddressHierarchyEntry();
        AddressHierarchyEntry childAddressHierarchyEntry2 = new AddressHierarchyEntry();
        childAddressHierarchyEntries.add(childAddressHierarchyEntry1);
        childAddressHierarchyEntries.add(childAddressHierarchyEntry2);
        childAddressHierarchyEntry1.setParent(addressHierarchyEntry);
        childAddressHierarchyEntry2.setParent(addressHierarchyEntry);
        childAddressHierarchyEntry1.setUserGeneratedId("20202001");
        childAddressHierarchyEntry2.setUserGeneratedId("20202002");

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);
        when(locationService.getLocationByUuid(anyString())).thenReturn(loginLocation);
        when(locationService.getLocationAttributeTypeByName(anyString())).thenReturn(locationAttributeType);
        when(addressHierarchyService.getChildAddressHierarchyEntries(any(AddressHierarchyEntry.class))).thenReturn(childAddressHierarchyEntries);
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class),anyString())).thenReturn(childAddressHierarchyEntry2);

        Map<String, List<String>> markers = locationBasedFilterEvaluator.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> transactionalDataFilters = new ArrayList<String>();
        transactionalDataFilters.add("202020");
        transactionalDataFilters.add("20202002");
        ArrayList<String> addressHierarchyFilters = new ArrayList<String>();
        addressHierarchyFilters.add("202020");
        categoryFilterMap.put("TransactionalData", transactionalDataFilters);
        categoryFilterMap.put("AddressHierarchy", addressHierarchyFilters);
        categoryFilterMap.put("ParentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        assertEquals(categoryFilterMap, markers);
    }

    @Test
    public void shouldGetFilterForDeviceBeAbleToParseWardListNamesIfItHasSpaceInBetweenOfTwoNames() throws Exception {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        Location loginLocation = new Location();
        LocationAttribute locationAttribute = new LocationAttribute();
        LocationAttributeType locationAttributeType = new LocationAttributeType();

        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
        locationAttribute.setVoided(Boolean.FALSE);
        locationAttribute.setAttributeType(locationAttributeType);
        locationAttribute.setValue("Ward No-01, Ward No-02");
        loginLocation.setAttribute(locationAttribute);

        List<AddressHierarchyEntry> childAddressHierarchyEntries = new ArrayList<AddressHierarchyEntry>();
        AddressHierarchyEntry childAddressHierarchyEntry1 = new AddressHierarchyEntry();
        AddressHierarchyEntry childAddressHierarchyEntry2 = new AddressHierarchyEntry();
        childAddressHierarchyEntries.add(childAddressHierarchyEntry1);
        childAddressHierarchyEntries.add(childAddressHierarchyEntry2);
        childAddressHierarchyEntry1.setParent(addressHierarchyEntry);
        childAddressHierarchyEntry2.setParent(addressHierarchyEntry);
        childAddressHierarchyEntry1.setUserGeneratedId("20202001");
        childAddressHierarchyEntry1.setName("Ward No-01");
        childAddressHierarchyEntry2.setUserGeneratedId("20202002");
        childAddressHierarchyEntry2.setName("Ward No-02");

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);
        when(locationService.getLocationByUuid(anyString())).thenReturn(loginLocation);
        when(locationService.getLocationAttributeTypeByName("catchmentFilters")).thenReturn(locationAttributeType);
        when(addressHierarchyService.getChildAddressHierarchyEntries(any(AddressHierarchyEntry.class))).thenReturn(childAddressHierarchyEntries);
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class),eq(("Ward No-01")))).thenReturn(childAddressHierarchyEntry1);
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class),eq(("Ward No-02")))).thenReturn(childAddressHierarchyEntry2);

        Map<String, List<String>> markers = locationBasedFilterEvaluator.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> transactionalDataFilters = new ArrayList<String>();
        transactionalDataFilters.add("202020");
        transactionalDataFilters.add("20202001");
        transactionalDataFilters.add("20202002");
        ArrayList<String> addressHierarchyFilters = new ArrayList<String>();
        addressHierarchyFilters.add("202020");
        categoryFilterMap.put("TransactionalData", transactionalDataFilters);
        categoryFilterMap.put("AddressHierarchy", addressHierarchyFilters);
        categoryFilterMap.put("ParentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        assertEquals(categoryFilterMap, markers);
    }

    @Test
    public void shouldGetFilterForDeviceThrowsARuntimeExceptionWhenAWrongNamePresentInWardListConfig() throws Exception {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        Location loginLocation = new Location();
        LocationAttribute locationAttribute = new LocationAttribute();
        LocationAttributeType locationAttributeType = new LocationAttributeType();

        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
        locationAttribute.setVoided(Boolean.FALSE);
        locationAttribute.setAttributeType(locationAttributeType);
        locationAttribute.setValue("20202002");
        loginLocation.setAttribute(locationAttribute);
        List<AddressHierarchyEntry> childAddressHierarchyEntries = new ArrayList<AddressHierarchyEntry>();

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);
        when(locationService.getLocationByUuid(anyString())).thenReturn(loginLocation);
        when(locationService.getLocationAttributeTypeByName(anyString())).thenReturn(locationAttributeType);
        when(addressHierarchyService.getChildAddressHierarchyEntries(any(AddressHierarchyEntry.class))).thenReturn(childAddressHierarchyEntries);
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class),anyString())).thenReturn(null);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Please check your catchmentFilters configuration in openmrs!!");
        Map<String, List<String>> markers = locationBasedFilterEvaluator.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
    }
}