package org.bahmni.module.bahmniOfflineSync.web.v1.controller;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.bahmni.module.bahmniOfflineSync.strategy.*;
import org.bahmni.module.bahmniOfflineSync.utils.EventRecordServiceHelper;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.*;
import org.openmrs.api.AdministrationService;
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
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({Context.class})
@RunWith(PowerMockRunner.class)
public class EventLogFilterControllerTest {

    @InjectMocks
    private EventLogFilterController controller;
    @Mock
    private AdministrationService administrationService;
    @Mock
    private SyncStrategyLoader syncStrategyLoader;
    @Mock
    private PatientService patientService;
    @Mock
    private EncounterService encounterService;
    @Mock
    private AddressHierarchyService addressHierarchyService;
    @Mock
    private LocationService locationService;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    EventRecordServiceHelper eventRecordServiceHelper;
    private EventRecord eventRecord;

    private LocationBasedSyncStrategy locationBasedSyncStrategy;
    AddressHierarchyEntry addressHierarchyEntry;
    Patient patient;
    Encounter encounter;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getPatientService()).thenReturn(patientService);
        Mockito.when(Context.getEncounterService()).thenReturn(encounterService);
        Mockito.when(Context.getLocationService()).thenReturn(locationService);
        List registeredComponents = new ArrayList();
        registeredComponents.add(platformTransactionManager);
        Mockito.when(Context.getRegisteredComponents(PlatformTransactionManager.class)).thenReturn(registeredComponents);
        locationBasedSyncStrategy = new LocationBasedSyncStrategy();
        patient = new Patient();
        patient.setUuid("patientUuid");
        encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setUuid("encounterUuid");
        PersonAttributeType pat = new PersonAttributeType();
        pat.setName("addressCode");
        PersonAttribute pa = new PersonAttribute(pat, "202020");
        Set personAttributes = new HashSet();
        personAttributes.add(pa);
        patient.setAttributes(personAttributes);
        addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
        eventRecord = new EventRecord("eventUuid", "p1", "", "/openmrs/patient/9449ee4b-456d-44a1-b865-2be8158e29d2", new Date(), "Patient");

    }

    @Test
    public void shouldGetFilterForPatient() throws Exception {
        when(syncStrategyLoader.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationBasedSyncStrategy);
        when(eventRecordServiceHelper.getEventRecordsAfterUuid("lastReadUuid")).thenReturn(Collections.singletonList(eventRecord));
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        List<EventLog> eventLogs = controller.getEventLogsAfter("lastReadUuid");
        assertEquals(eventLogs.size(), 1);
        assertEquals(eventLogs.get(0).getUuid(), eventRecord.getUuid());
        assertEquals("202020", eventLogs.get(0).getFilter());
    }

    @Test
    public void shouldGetFilterForEncounter() throws Exception {
        eventRecord = new EventRecord("eventUuid", "p1", "", "/openmrs/encounter/9449ee4b-456d-44a1-b865-2be8158e29d2", new Date(), "Encounter");
        when(eventRecordServiceHelper.getEventRecordsAfterUuid("lastReadUuid")).thenReturn(Collections.singletonList(eventRecord));
        when(syncStrategyLoader.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationBasedSyncStrategy);
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(encounter);
        when(patientService.getPatientByUuid(anyString())).thenReturn(patient);
        List<EventLog> eventLogs = controller.getEventLogsAfter("lastReadUuid");
        assertEquals(eventLogs.size(), 1);
        assertEquals(eventLogs.get(0).getUuid(), eventRecord.getUuid());
        assertEquals("202020", eventLogs.get(0).getFilter());
    }

    @Test
    public void shouldGetFilterForAddressHierarchy() throws Exception {
        eventRecord = new EventRecord("eventUuid", "p1", "", "/openmrs/addresshierarchy/9449ee4b-456d-44a1-b865-2be8158e29d2", new Date(), "addressHierarchy");
        when(eventRecordServiceHelper.getEventRecordsAfterUuid("lastReadUuid")).thenReturn(Collections.singletonList(eventRecord));
        AddressHierarchyLevel ahl = new AddressHierarchyLevel();
        ahl.setLevelId(6);
        addressHierarchyEntry.setLevel(ahl);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        when(syncStrategyLoader.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationBasedSyncStrategy);
        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);
        List<EventLog> eventLogs = controller.getEventLogsAfter("lastReadUuid");
        assertEquals(eventLogs.size(), 1);
        assertEquals(eventLogs.get(0).getUuid(), eventRecord.getUuid());
        assertEquals("202020", eventLogs.get(0).getFilter());
    }

    @Test
    public void shouldGetFilterForDevice() throws Exception {
        when(syncStrategyLoader.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationBasedSyncStrategy);
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);
        AddressHierarchyEntry addressHierarchyEntry = new AddressHierarchyEntry();
        AddressHierarchyEntry parentAddressHierarchyEntry = new AddressHierarchyEntry();
        addressHierarchyEntry.setParent(parentAddressHierarchyEntry);
        addressHierarchyEntry.setUserGeneratedId("202020");
        when(addressHierarchyService.getAddressHierarchyEntryByUuid(anyString())).thenReturn(addressHierarchyEntry);
        when(locationService.getLocationByUuid(anyString())).thenReturn(new org.openmrs.Location());
        when(locationService.getLocationAttributeTypeByName(anyString())).thenReturn(new LocationAttributeType());

        Map<String, List<String>> markers = controller.getFilterForDevice("providerUuid", "addressUuid", "locationUuid");
        Map categoryFilterMap = new HashMap();
        ArrayList<String> filters = new ArrayList<String>();
        filters.add("202020");
        categoryFilterMap.put("patient", filters);
        categoryFilterMap.put("encounter", filters);
        categoryFilterMap.put("addressHierarchy", filters);
        categoryFilterMap.put("parentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        assertEquals(categoryFilterMap, markers);

    }

    @Test
    public void shouldGetCategoryList() throws Exception {
        when(syncStrategyLoader.getFilterEvaluatorFromGlobalProperties()).thenReturn(locationBasedSyncStrategy);

        List<String> categories = controller.getCategoryList();
        assertTrue(categories.contains("patient"));
        assertTrue(categories.contains("encounter"));
        assertTrue(categories.contains("addressHierarchy"));
        assertTrue(categories.contains("parentAddressHierarchy"));
        assertTrue(categories.contains("offline-concepts"));
        assertTrue(categories.size() == 5);
    }


    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenGlobalPropertyIsNotConfigured() throws Exception {
        when(administrationService.getGlobalProperty(anyString())).thenReturn(new String());
        controller.getCategoryList();
    }

}
