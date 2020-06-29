package org.bahmni.module.bahmniOfflineSync.strategy;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.ict4h.atomfeed.server.domain.EventRecord;
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
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)

public class IDBasedSyncStrategyTest {
    private IDBasedSyncStrategy idBasedSyncStrategy;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private PatientService patientService;
    @Mock
    private EncounterService encounterService;
    @Mock
    private IdentifierSourceService identifierSourceService;
    @Mock
    private PlatformTransactionManager platformTransactionManager;
    @Mock
    private LocationService locationService;
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Session session;
    @Mock
    private Transaction transaction;

    private AddressHierarchyEntry addressHierarchyEntry;
    private Patient patient;
    Encounter encounter;
    private String encounterUuid = "ff17adba-9750-462e-be29-e35091af93df";
    private String patientUuid = "ff17adba-9750-462e-be29-e35091af93dd";
    private String addressUuid = "ff17adba-4870-462e-be29-e35091af93de";
    List<IdentifierSource> identifierSources;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getPatientService()).thenReturn(patientService);
        Mockito.when(Context.getEncounterService()).thenReturn(encounterService);
        Mockito.when(Context.getLocationService()).thenReturn(locationService);
        Mockito.when(Context.getService(IdentifierSourceService.class)).thenReturn(identifierSourceService);
        List registeredComponents = new ArrayList();
        registeredComponents.add(platformTransactionManager);
        Mockito.when(Context.getRegisteredComponents(PlatformTransactionManager.class)).thenReturn(registeredComponents);
        List<SessionFactory> sessionFactoryComponents = new ArrayList<SessionFactory>();
        sessionFactoryComponents.add(sessionFactory);
        Mockito.when(Context.getRegisteredComponents(SessionFactory.class)).thenReturn(sessionFactoryComponents);
        Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
        Mockito.when(session.getTransaction()).thenReturn(transaction);
        idBasedSyncStrategy = new IDBasedSyncStrategy();
        patient = new Patient();
        patient.setUuid(patientUuid);
        encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setUuid(encounterUuid);
        PatientIdentifierType pit = new PatientIdentifierType();
        pit.setName("Patient Identifier");
        PatientIdentifier pi = new PatientIdentifier();
        pi.setIdentifierType(pit);
        pi.setIdentifier("GAN");
        patient.setIdentifiers(Collections.singleton(pi));
        addressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setUuid(addressUuid);
        SequentialIdentifierGenerator ganIdentifierSource = new SequentialIdentifierGenerator();
        ganIdentifierSource.setName("GAN");
        ganIdentifierSource.setPrefix("GAN");
        identifierSources = new ArrayList<IdentifierSource>();
        identifierSources.add(ganIdentifierSource);
    }

    @Test
    public void shouldSetFilterAsNullIfUuidIsEmpty() throws Exception {
        PowerMockito.mockStatic(Context.class);
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "address", "", "url/ff17adba-4870-462e-be29-e35091af9xde", new Date(), "patient");
        eventRecords.add(er);
        List<EventLog> eventLogs = idBasedSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
    }

    @Test
    public void shouldNotSetFilterForAddressHierarchy() throws Exception {
        PowerMockito.mockStatic(Context.class);
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "address", "", "url/" + addressUuid, new Date(), "addressHierarchy");
        eventRecords.add(er);
        List<EventLog> eventLogs = idBasedSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
    }

    @Test
    public void shouldEvaluateFilterForEncounter() {
        when(encounterService.getEncounterByUuid(encounterUuid)).thenReturn(encounter);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        when(identifierSourceService.getAllIdentifierSources(false)).thenReturn(identifierSources);
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "Encounter", "", "url/" + encounterUuid, new Date(), "Encounter");
        eventRecords.add(er);
        List<EventLog> eventLogs = idBasedSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(encounterService, times(1)).getEncounterByUuid(encounterUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals("GAN", eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
    }

    @Test
    public void shouldReturnNullAsFilterIfEncounterUuidIsNull() throws Exception {
        PersonAttribute personAttribute = new PersonAttribute();
        personAttribute.setValue("Value");
        List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "Encounter", "", "url/" + encounterUuid, new Date(), "Encounter");
        eventRecords.add(er);
        List<EventLog> eventLogs = idBasedSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(encounterService, times(1)).getEncounterByUuid(encounterUuid);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(null, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());

        verify(patientService, never()).getPatientByUuid(patientUuid);
    }

    @Test
    public void shouldThrowRuntimeExceptionIfPrefixIsNotConfiguredForIdentifierSourceName() throws Exception {
        SequentialIdentifierGenerator ganIdentifierSource = new SequentialIdentifierGenerator();
        ganIdentifierSource.setName("GAN");
        ArrayList<IdentifierSource> identifierSources = new ArrayList<>();
        identifierSources.add(ganIdentifierSource);

        when(encounterService.getEncounterByUuid(encounterUuid)).thenReturn(encounter);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        when(identifierSourceService.getAllIdentifierSources(false)).thenReturn(identifierSources);
        List<EventRecord> eventRecords = new ArrayList<>();
        EventRecord er = new EventRecord("uuid", "Patient", "", "url/" + patientUuid, new Date(), "Patient");
        eventRecords.add(er);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Please set prefix for GAN");
        idBasedSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        fail();
    }

    @Test
    public void shouldSetFilterAsNullIfPatientDoesNotHavePatientIdentifier() throws Exception {
        Patient patient = new Patient();
        patient.setUuid(patientUuid);
        PatientIdentifierType pit = new PatientIdentifierType();
        pit.setName("National Identifier");
        PatientIdentifier pi = new PatientIdentifier();
        pi.setIdentifierType(pit);
        pi.setIdentifier("GAN");
        patient.setIdentifiers(Collections.singleton(pi));

        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        List<EventRecord> eventRecords = new ArrayList<>();
        EventRecord er = new EventRecord("uuid", "Patient", "", "url/" + patientUuid, new Date(), "Patient");
        eventRecords.add(er);
        List<EventLog> eventLogs = idBasedSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        verify(patientService, times(1)).getPatientByUuid(patientUuid);
        verify(identifierSourceService, times(0)).getAllIdentifierSources(false);
        assertEquals(1, eventLogs.size());
        assertNull(eventLogs.get(0).getFilter());
    }

    @Test
    public void shouldGetCategoryList() throws Exception {
        List<String> categories = idBasedSyncStrategy.getEventCategoriesList();
        assertTrue(categories.contains("patient"));
        assertTrue(categories.contains("encounter"));
        assertTrue(categories.contains("addressHierarchy"));
        assertTrue(categories.contains("offline-concepts"));
        assertTrue(categories.contains("forms"));
        assertTrue(categories.size() == 5);
    }

    @Test
    public void shouldGetFilterForDevice() {
        Location loginLocation = new Location();
        loginLocation.setName("Test Login Location");

        LocationAttribute locationAttribute = new LocationAttribute();
        LocationAttributeType locationAttributeType = new LocationAttributeType();
        locationAttributeType.setName("IdentifierSourceName");
        locationAttribute.setAttributeType(locationAttributeType);
        loginLocation.setAttribute(locationAttribute);
        locationAttribute.setValue("GAN");
        SequentialIdentifierGenerator ganIdentifierSource = new SequentialIdentifierGenerator();
        ganIdentifierSource.setName("GAN");
        ganIdentifierSource.setPrefix("GAN");
        ArrayList<IdentifierSource> identifierSources = new ArrayList<>();
        identifierSources.add(ganIdentifierSource);

        when(identifierSourceService.getAllIdentifierSources(false)).thenReturn(identifierSources);
        when(locationService.getLocationByUuid(anyString())).thenReturn(loginLocation);
        when(locationService.getLocationAttributeTypeByName(anyString())).thenReturn(locationAttributeType);

        Map<String, List<String>> markers = idBasedSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> transactionalDataFilters = new ArrayList<String>();
        transactionalDataFilters.add("GAN");

        categoryFilterMap.put("patient", transactionalDataFilters);
        categoryFilterMap.put("encounter", transactionalDataFilters);
        categoryFilterMap.put("addressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        categoryFilterMap.put("forms", new ArrayList<String>());
        assertEquals(categoryFilterMap, markers);
    }

    @Test
    public void shouldThrowExceptionIfIdentifierSourceNameConfigIsWrong() throws Exception {
        Location loginLocation = new Location();
        loginLocation.setName("Test Login Location");
        LocationAttribute locationAttribute = new LocationAttribute();
        LocationAttributeType locationAttributeType = new LocationAttributeType();
        locationAttributeType.setName("IdentifierSourceName");
        locationAttribute.setAttributeType(locationAttributeType);
        loginLocation.setAttribute(locationAttribute);
        locationAttribute.setValue("GAN");

        SequentialIdentifierGenerator ganIdentifierSource = new SequentialIdentifierGenerator();
        ganIdentifierSource.setName("BAH");
        ganIdentifierSource.setPrefix("GAN");
        ArrayList<IdentifierSource> identifierSources = new ArrayList<>();
        identifierSources.add(ganIdentifierSource);

        when(identifierSourceService.getAllIdentifierSources(false)).thenReturn(identifierSources);
        when(locationService.getLocationByUuid(anyString())).thenReturn(loginLocation);
        when(locationService.getLocationAttributeTypeByName(anyString())).thenReturn(locationAttributeType);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Please check [IdentifierSourceName] config for [Test Login Location]");
        idBasedSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
    }
}
