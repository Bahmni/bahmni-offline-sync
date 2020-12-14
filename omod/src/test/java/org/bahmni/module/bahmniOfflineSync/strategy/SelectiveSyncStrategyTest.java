package org.bahmni.module.bahmniOfflineSync.strategy;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.bahmni.module.bahmniOfflineSync.utils.AddressHierarchyLevelWithLevelEntryName;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressField;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class SelectiveSyncStrategyTest {

    private SelectiveSyncStrategy selectiveSyncStrategy;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private PatientService patientService;
    @Mock
    private EncounterService encounterService;
    @Mock
    private PlatformTransactionManager platformTransactionManager;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    @Mock
    private LocationService locationService;
    private AddressHierarchyEntry addressHierarchyEntry;
    private Patient patient;
    Encounter encounter;
    private String encounterUuid = "ff17adba-9750-462e-be29-e35091af93df";
    private String patientUuid = "ff17adba-9750-462e-be29-e35091af93dd";
    private String addressUuid = "ff17adba-4870-462e-be29-e35091af93de";
    private Person person;
    private PersonAddress personAddress;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getPatientService()).thenReturn(patientService);
        Mockito.when(Context.getEncounterService()).thenReturn(encounterService);
        Mockito.when(Context.getLocationService()).thenReturn(locationService);
        List registeredComponents = new ArrayList();
        registeredComponents.add(platformTransactionManager);
        Mockito.when(Context.getRegisteredComponents(PlatformTransactionManager.class)).thenReturn(registeredComponents);
        selectiveSyncStrategy = new SelectiveSyncStrategy();
        person = new Person();
        person.setId(123);
        personAddress = new PersonAddress();
        personAddress.setStateProvince("Test");
        personAddress.setCityVillage("District");
        personAddress.setAddress2("Facility");
        Set<PersonAddress> personAddressSet = new TreeSet<PersonAddress>();
        personAddressSet.add(personAddress);
        person.setAddresses(personAddressSet);
        patient = new Patient(person);
        patient.setUuid(patientUuid);
        encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setUuid(encounterUuid);
        PersonAttributeType pat = new PersonAttributeType();
        pat.setName("addressCode");
        PersonAttribute pa = new PersonAttribute(pat, "202020");
        Set personAttributes = new HashSet();
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
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord("uuid","address","","url/" + addressUuid,new Date(),"addressHierarchy");
        eventRecords.add(er);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        verify(addressHierarchyService, times(1)).getAddressHierarchyEntryByUuid(addressUuid);
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());
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
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord("uuid","address","","url/"+ addressUuid,new Date(),"addressHierarchy");
        eventRecords.add(er);

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(addressUuid)).thenReturn(addressHierarchyEntry);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(addressHierarchyService, times(1)).getAddressHierarchyEntryByUuid(addressUuid);
        assertEquals(addressHierarchyEntry.getUserGeneratedId(), eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());
    }

    @Test
    public void shouldNotSetFilterIfLevelIdIsNull() throws Exception {
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setUserGeneratedId("Geocode");
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord("uuid","address","","url/"+ addressUuid,new Date(),"addressHierarchy");
        eventRecords.add(er);

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(addressUuid)).thenReturn(addressHierarchyEntry);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(addressHierarchyService, times(1)).getAddressHierarchyEntryByUuid(addressUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());
    }

    @Test
    public void shouldNotSetFilterIfUuidIsNull() throws Exception {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord(null,"address","","url/",new Date(),"addressHierarchy");
        eventRecords.add(er);

        when(addressHierarchyService.getAddressHierarchyEntryByUuid(null)).thenReturn(null);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(addressHierarchyService, times(0)).getAddressHierarchyEntryByUuid(addressUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());
    }


    @Test
    public void shouldEvaluateFilterForEncounter() {
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(encounter);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord("uuid","Encounter","","url/" + encounterUuid,new Date(),"Encounter");
        eventRecords.add(er);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        when(addressHierarchyService.getAddressHierarchyLevels()).thenReturn(new ArrayList<AddressHierarchyLevel>());
        when(addressHierarchyService.getAddressHierarchyEntriesByLevel(any(AddressHierarchyLevel.class))).thenReturn(new ArrayList<AddressHierarchyEntry>());
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(encounterService, times(1)).getEncounterByUuid(encounterUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());
    }

    @Test
    public void shouldReturnNullAsFilterIfEncounterUuidIsNull() throws Exception {
        PersonAttribute personAttribute = new PersonAttribute();
        personAttribute.setValue("Value");
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord(null,"Encounter","","url/" + null,new Date(),"Encounter");
        eventRecords.add(er);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(encounterService, times(0)).getEncounterByUuid(null);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());

        verify(patientService, never()).getPatientByUuid(patientUuid);
    }

    @Ignore
    @Test
    public void shouldReturnNullAsStringIfEncountersPatientAttributeIsNotAvailable() throws Exception {
        Patient patientTemp = this.patient;
        Set personAttributes = new HashSet();
        patientTemp.setAttributes(personAttributes);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patientTemp);
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(encounter);
        encounter.setPatient(patientTemp);
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord("uuid","Encounter","","url/" + encounterUuid, new Date(),"Encounter");
        eventRecords.add(er);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(encounterService, times(1)).getEncounterByUuid(anyString());
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());

        verify(patientService, times(1)).getPatientByUuid(anyString());
    }

    @Ignore
    @Test
    public void shouldReturnNullAsFilterIfAttributeIsNotAvailable() throws Exception {
        when(patientService.getPatientByUuid(anyString())).thenReturn(new Patient());
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er  = new EventRecord("uuid","patient","","url/" + patientUuid,new Date(),"Patient");
        eventRecords.add(er);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(patientService, times(1)).getPatientByUuid(patientUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(er.getCategory(),eventLogs.get(0).getCategory());
        assertEquals(null, eventLogs.get(0).getFilter());
    }

    @Test
    public void shouldGetCategoryList() throws Exception {
        List<String> categories = selectiveSyncStrategy.getEventCategoriesList();
        assertTrue(categories.contains("patient"));
        assertTrue(categories.contains("encounter"));
        assertTrue(categories.contains("addressHierarchy"));
        assertTrue(categories.contains("parentAddressHierarchy"));
        assertTrue(categories.contains("offline-concepts"));
        assertTrue(categories.contains("forms"));
        assertTrue(categories.size() == 6);
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
        when(addressHierarchyService.getChildAddressHierarchyEntries(Matchers.same(childAddressHierarchyEntry1))).thenReturn(new ArrayList<AddressHierarchyEntry>());
        when(addressHierarchyService.getChildAddressHierarchyEntries(Matchers.same(childAddressHierarchyEntry2))).thenReturn(new ArrayList<AddressHierarchyEntry>());
        when(addressHierarchyService.getChildAddressHierarchyEntries(Matchers.same(addressHierarchyEntry))).thenReturn(childAddressHierarchyEntries);

        Map<String, List<String>> markers = selectiveSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> transactionalDataFilters = new ArrayList<String>();
        transactionalDataFilters.add("202020");
        transactionalDataFilters.add("20202001");
        transactionalDataFilters.add("20202002");
        ArrayList<String> addressHierarchyFilters = new ArrayList<String>();
        addressHierarchyFilters.add("202020");
        categoryFilterMap.put("patient", transactionalDataFilters);
        categoryFilterMap.put("encounter", transactionalDataFilters);
        categoryFilterMap.put("addressHierarchy", addressHierarchyFilters);
       // categoryFilterMap.put("parentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        categoryFilterMap.put("forms", new ArrayList<String>());
        assertEquals(categoryFilterMap, markers);
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
        when(addressHierarchyService.getChildAddressHierarchyEntries(Matchers.same(addressHierarchyEntry))).thenReturn(childAddressHierarchyEntries);
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class), anyString())).thenReturn(childAddressHierarchyEntry2);

        Map<String, List<String>> markers = selectiveSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> transactionalDataFilters = new ArrayList<String>();
        transactionalDataFilters.add("202020");
        transactionalDataFilters.add("20202002");
        ArrayList<String> addressHierarchyFilters = new ArrayList<String>();
        addressHierarchyFilters.add("202020");
        categoryFilterMap.put("patient", transactionalDataFilters);
        categoryFilterMap.put("encounter", transactionalDataFilters);
        categoryFilterMap.put("addressHierarchy", addressHierarchyFilters);
        //categoryFilterMap.put("parentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        categoryFilterMap.put("forms", new ArrayList<String>());
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
        locationAttribute.setValue("    Ward No-01     ,     Ward No-02     ");
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
        when(addressHierarchyService.getChildAddressHierarchyEntries(Matchers.same(addressHierarchyEntry))).thenReturn(childAddressHierarchyEntries);
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class), eq(("Ward No-01")))).thenReturn(childAddressHierarchyEntry1);
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class), eq(("Ward No-02")))).thenReturn(childAddressHierarchyEntry2);

        Map<String, List<String>> markers = selectiveSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> transactionalDataFilters = new ArrayList<String>();
        transactionalDataFilters.add("202020");
        transactionalDataFilters.add("20202001");
        transactionalDataFilters.add("20202002");
        ArrayList<String> addressHierarchyFilters = new ArrayList<String>();
        addressHierarchyFilters.add("202020");
        categoryFilterMap.put("patient", transactionalDataFilters);
        categoryFilterMap.put("encounter", transactionalDataFilters);
        categoryFilterMap.put("addressHierarchy", addressHierarchyFilters);
        //categoryFilterMap.put("parentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        categoryFilterMap.put("forms", new ArrayList<String>());
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
        when(addressHierarchyService.getChildAddressHierarchyEntryByName(any(AddressHierarchyEntry.class), anyString())).thenReturn(null);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Please check your catchmentFilters configuration in openmrs!!");
        selectiveSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenAddressFieldIsNUll() throws Exception {
        selectiveSyncStrategy.getFilterForDevice("providerUuid", "null", "locationUuid");
    }

    @Test
    public void shouldGetUserGeneratedIDinFilterForPatient() throws Exception {
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "Patient", "", "url/" + patientUuid, new Date(), "Patient");
        eventRecords.add(er);
        when(patientService.getPatientByUuid(patientUuid)).thenReturn(patient);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        AddressHierarchyLevel level1 = new AddressHierarchyLevel();
        level1.setAddressField(AddressField.STATE_PROVINCE);
        level1.setId(1);
        level1.setName("Province");
        level1.setLevelId(3);
        level1.setParent(null);

        AddressHierarchyLevel level2 = new AddressHierarchyLevel();
        level2.setAddressField(AddressField.CITY_VILLAGE);
        level2.setId(2);
        level2.setName("District");
        level2.setLevelId(4);
        level2.setParent(level1);

        AddressHierarchyLevel level3 = new AddressHierarchyLevel();
        level3.setAddressField(AddressField.ADDRESS_2);
        level3.setId(3);
        level3.setName("Facility");
        level3.setLevelId(5);
        level3.setParent(level2);

        AddressHierarchyLevel level4 = new AddressHierarchyLevel();
        level4.setAddressField(AddressField.ADDRESS_1);
        level4.setId(4);
        level4.setName("Address (free text)");
        level4.setLevelId(6);
        level4.setParent(level3);

        List<AddressHierarchyLevel> levels = new ArrayList<>();
        levels.add(level1);
        levels.add(level2);
        levels.add(level3);
        levels.add(level4);

        AddressHierarchyEntry entry1 = new AddressHierarchyEntry();
        entry1.setAddressHierarchyEntryId(1);
        entry1.setAddressHierarchyLevel(level3);
        entry1.setName("GUDO CLINIC[25]");
        entry1.setUserGeneratedId("07-08-09");

        List<AddressHierarchyEntry> entries = new ArrayList<>();
        entries.add(entry1);

        AddressHierarchyLevelWithLevelEntryName customObject = new AddressHierarchyLevelWithLevelEntryName(level3, "GUDO CLINIC[25]");

        when(addressHierarchyService.getAddressHierarchyLevels()).thenReturn(levels);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevel(level1)).thenReturn(entries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevel(level2)).thenReturn(entries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevel(level3)).thenReturn(entries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndLikeName(anyObject(), anyString(), anyInt())).thenReturn(entries);
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(patientService, times(1)).getPatientByUuid(patientUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
        assertEquals("07-08-09", eventLogs.get(0).getFilter());
    }

    @Test
    public void shouldNotGetUserGeneratedIDinFilterForPatientIfNoMatchIsFound() throws Exception {
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "Patient", "", "url/" + patientUuid, new Date(), "Patient");
        eventRecords.add(er);
        when(patientService.getPatientByUuid(patientUuid)).thenReturn(patient);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

        AddressHierarchyLevel level1 = new AddressHierarchyLevel();
        level1.setAddressField(AddressField.STATE_PROVINCE);
        level1.setId(1);
        level1.setName("Province");
        level1.setLevelId(3);
        level1.setParent(null);

        AddressHierarchyLevel level2 = new AddressHierarchyLevel();
        level2.setAddressField(AddressField.CITY_VILLAGE);
        level2.setId(2);
        level2.setName("District");
        level2.setLevelId(4);
        level2.setParent(level1);

        AddressHierarchyLevel level3 = new AddressHierarchyLevel();
        level3.setAddressField(AddressField.ADDRESS_2);
        level3.setId(3);
        level3.setName("Facility");
        level3.setLevelId(5);
        level3.setParent(level2);

        AddressHierarchyLevel level4 = new AddressHierarchyLevel();
        level4.setAddressField(AddressField.ADDRESS_1);
        level4.setId(4);
        level4.setName("Address (free text)");
        level4.setLevelId(6);
        level4.setParent(level3);

        List<AddressHierarchyLevel> levels = new ArrayList<>();
        levels.add(level1);
        levels.add(level2);
        levels.add(level3);
        levels.add(level4);

        AddressHierarchyEntry entry1 = new AddressHierarchyEntry();
        entry1.setAddressHierarchyEntryId(1);
        entry1.setAddressHierarchyLevel(level3);
        entry1.setName("GUDO CLINIC[25]");
        entry1.setUserGeneratedId("07-08-09");

        List<AddressHierarchyEntry> entries = new ArrayList<>();
        entries.add(entry1);

        AddressHierarchyLevelWithLevelEntryName customObject = new AddressHierarchyLevelWithLevelEntryName(level3, "GUDO CLINIC[25]");

        when(addressHierarchyService.getAddressHierarchyLevels()).thenReturn(levels);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevel(level1)).thenReturn(entries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevel(level2)).thenReturn(entries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevel(level3)).thenReturn(entries);
        when(addressHierarchyService.getAddressHierarchyEntriesByLevelAndLikeName(anyObject(), anyString(), anyInt())).thenReturn(anyList());
        List<EventLog> eventLogs = selectiveSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(patientService, times(1)).getPatientByUuid(patientUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
        assertEquals(null, eventLogs.get(0).getFilter());
    }

}