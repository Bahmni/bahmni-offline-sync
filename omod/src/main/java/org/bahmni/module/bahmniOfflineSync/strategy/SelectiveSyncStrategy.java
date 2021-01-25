package org.bahmni.module.bahmniOfflineSync.strategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.bahmni.module.bahmniOfflineSync.utils.AddressHierarchyLevelWithLevelEntryName;
import org.bahmni.module.bahmniOfflineSync.utils.SelectiveSyncStrategyHelper;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.*;

public class SelectiveSyncStrategy extends AbstractOfflineSyncStrategy {
    private static final String ATTRIBUTE_TYPE_NAME = "addressCode";
    protected static Log log = LogFactory.getLog(SelectiveSyncStrategy.class);
    private static AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);

    public SelectiveSyncStrategy() throws SQLException {
        super();

    }

    protected String evaluateFilterForPatient(String uuid) {
        String patientFilter = null;
        Patient patient = patientService.getPatientByUuid(uuid);

        if (patient != null && patient.getAttribute(ATTRIBUTE_TYPE_NAME) != null) {
            patientFilter = patient.getAttribute(ATTRIBUTE_TYPE_NAME).getValue();
        }

        return patientFilter;
    }

    private String evaluateFilterForEncounter(String uuid) {
        String filter = null;
        Encounter encounter = encounterService.getEncounterByUuid(uuid);
        if (encounter != null)
            filter = evaluateFilterForPatient(encounter.getPatient().getUuid());
        return filter;
    }

    private String evaluateFilterForAddressHierarchy(String uuid) {
        String addressHierarchyFilter = null;
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(uuid);
        if (addressHierarchyEntry != null && addressHierarchyEntry.getLevel() != null && addressHierarchyEntry.getLevel().getId() > 3) {
            addressHierarchyFilter = addressHierarchyEntry.getUserGeneratedId();
        }
        return addressHierarchyFilter;
    }


    public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
        if(addressUuid.equals("null") || addressUuid.isEmpty()) {
            throw new RuntimeException("Please give address for this login location in openmrs");
        }
        Map<String, List<String>> categoryFilterMap = new HashMap();
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(addressUuid);
        List transactionalDataFilters = getTransactionalDataFilters(loginLocationUuid, addressHierarchyService, addressHierarchyEntry);
        categoryFilterMap.put("patient", transactionalDataFilters);
        categoryFilterMap.put("encounter", transactionalDataFilters);
        categoryFilterMap.put("addressHierarchy", getFiltersForAddressHierarchy(addressHierarchyEntry));
        //categoryFilterMap.put("parentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        categoryFilterMap.put("forms", new ArrayList<>());
        return categoryFilterMap;
    }

    private List getTransactionalDataFilters(String loginLocationUuid, AddressHierarchyService addressHierarchyService, AddressHierarchyEntry addressHierarchyEntry) {
        List transactionalDataFilters = new ArrayList();
        if (addressHierarchyEntry != null) {
            LocationAttributeType catchmentFiltersAttribute = locationService.getLocationAttributeTypeByName("catchmentFilters");
            String userGeneratedId = addressHierarchyEntry.getUserGeneratedId();
            LocationAttribute catchmentFilters = getCatchmentFilters(loginLocationUuid, catchmentFiltersAttribute);
            List<AddressHierarchyEntry> childAddressHierarchyEntries = addressHierarchyService.getChildAddressHierarchyEntries(addressHierarchyEntry);
            List<String> transactionalFilters = getCatchmentIds(catchmentFilters, childAddressHierarchyEntries, addressHierarchyService, addressHierarchyEntry);
            transactionalDataFilters.add(userGeneratedId);
            transactionalDataFilters.addAll(transactionalFilters);
        }
        return transactionalDataFilters;
    }

    private LocationAttribute getCatchmentFilters(String loginLocationUuid, LocationAttributeType catchmentFiltersAttribute) {
        Location location = locationService.getLocationByUuid(loginLocationUuid);
        List<LocationAttribute> attributes = (List<LocationAttribute>) location.getActiveAttributes();
        for (LocationAttribute attribute : attributes) {
            if (attribute.getAttributeType().equals(catchmentFiltersAttribute)) {
                return attribute;
            }
        }
        return null;
    }

    private List<String> getCatchmentIds(LocationAttribute catchmentFilters, List<AddressHierarchyEntry> childAddressHierarchyEntries, AddressHierarchyService addressHierarchyService, AddressHierarchyEntry addressHierarchyEntry) {
        List<String> wardIDs = new ArrayList();
        if (catchmentFilters != null) {
            String wardsName = trim(catchmentFilters.getValue().toString());
            Set<String> wardsNameList = StringUtils.commaDelimitedListToSet(wardsName);
            for (String wardName : wardsNameList) {
                AddressHierarchyEntry childAddressHierarchyEntry = addressHierarchyService.getChildAddressHierarchyEntryByName(addressHierarchyEntry, wardName);
                if (childAddressHierarchyEntry == null) {
                    throw new RuntimeException("Please check your catchmentFilters configuration in openmrs!!");
                } else {
                    getAllWardIds(childAddressHierarchyEntry, addressHierarchyService, wardIDs);
                }
            }
        } else {
            updateWardIds(addressHierarchyService, wardIDs, childAddressHierarchyEntries);
        }
        return wardIDs;
    }

    private void getAllWardIds(AddressHierarchyEntry addressHierarchyEntry, AddressHierarchyService addressHierarchyService, List<String> wardIDs) {
        if (addressHierarchyEntry == null) {
            return;
        }
        wardIDs.add(addressHierarchyEntry.getUserGeneratedId());
        List<AddressHierarchyEntry> childAddressHierarchyEntries = addressHierarchyService.getChildAddressHierarchyEntries(addressHierarchyEntry);
        updateWardIds(addressHierarchyService, wardIDs, childAddressHierarchyEntries);
    }

    private void updateWardIds(AddressHierarchyService addressHierarchyService, List<String> wardIDs, List<AddressHierarchyEntry> childAddressHierarchyEntries) {
        for (AddressHierarchyEntry childAddressHierarchyEntry : childAddressHierarchyEntries) {
            getAllWardIds(childAddressHierarchyEntry, addressHierarchyService, wardIDs);
        }
    }

    private String trim(String content) {
        content = content.trim();
        return content.replaceAll("(\\s*,\\s*)", ",");
    }

    @Override
    public List<String> getEventCategoriesList() {
        List<String> eventCategoryList = new ArrayList();

        eventCategoryList.add("patient");
        eventCategoryList.add("encounter");
        eventCategoryList.add("addressHierarchy");
        eventCategoryList.add("parentAddressHierarchy");
        eventCategoryList.add("offline-concepts");
        eventCategoryList.add("forms");

        return eventCategoryList;
    }



    private List<String> getFiltersForAddressHierarchy(AddressHierarchyEntry addressHierarchyEntry) {
        List addressHierarchyFilters = new ArrayList();
        while (addressHierarchyEntry.getParent() != null) {
            if (addressHierarchyEntry.getUserGeneratedId().length() == 6) {
                addressHierarchyFilters.add(addressHierarchyEntry.getUserGeneratedId());
                break;
            }
            addressHierarchyEntry = addressHierarchyEntry.getParent();
        }
        return addressHierarchyFilters;
    }

    @Override
    public List<EventLog> getEventLogsFromEventRecords(List<EventRecord> eventRecords) {
        List<EventLog> eventLogs = new ArrayList<EventLog>();

        for (EventRecord er : eventRecords) {
            EventLog eventLog = new EventLog(er.getUuid(),er.getCategory(),er.getTimeStamp(),er.getContents(), null, er.getUuid());
            String category = er.getCategory();
            String uuid = getUuidFromURL(er.getContents());
            String filter = null;

            if (!uuid.isEmpty()) {
                if (category.equalsIgnoreCase("all-concepts")) {
                    if (isOfflineConceptEvent(uuid)) {
                        eventLog.setCategory("offline-concepts");
                    } else {
                        eventLog.setCategory("concepts");
                    }
                }

                if (category.equalsIgnoreCase("Patient")|| category.equalsIgnoreCase("LabOrderResults")) {
                    setAdditionalFilters(eventLog,uuid);
                }
                else if (category.equalsIgnoreCase("Encounter") || category.equalsIgnoreCase("SHREncounter")) {
                    setAdditionalFilters(eventLog,uuid);
                }
                else if (category.equalsIgnoreCase("addressHierarchy")) {
                    filter = evaluateFilterForAddressHierarchy(uuid);
                    eventLog.setFilter(filter);
                }
            }
            eventLogs.add(eventLog);
        }

        return eventLogs;
    }

    private void setAdditionalFilters(EventLog eventLog, String uuid) {
        if (eventLog.getCategory().equalsIgnoreCase("patient")) {
            AddressHierarchyLevel lowerLevel = getLowestLevel();
            String lowestLevelUserGeneratedId = (null == lowerLevel ? null :
                    getUserGeneratedIDValue(findLowestLevelAvailableInPersonAddress(lowerLevel, getAddressHierarchyLevelsList(), getPatient(uuid).getPerson().getPersonAddress())));
            
            eventLog.setFilter(lowestLevelUserGeneratedId);

        } else if (eventLog.getCategory().equalsIgnoreCase("encounter")) {
            Encounter encounter = encounterService.getEncounterByUuid(uuid);
            AddressHierarchyLevel lowerLevel = getLowestLevel();
            String lowestLevelUserGeneratedId = (null == lowerLevel ? null
                    : getUserGeneratedIDValue(findLowestLevelAvailableInPersonAddress(lowerLevel, getAddressHierarchyLevelsList(), getPatient(encounter.getPatient().getUuid()).getPerson().getPersonAddress())));
           
            eventLog.setFilter(lowestLevelUserGeneratedId);
        }
    }

    private Patient getPatient(String uuid)
    {
        return patientService.getPatientByUuid(uuid);
    }

    private boolean isOfflineConceptEvent(String eventUuid) {
        final Concept concept = conceptService.getConceptByUuid(eventUuid);
        final Concept offlineConcept = conceptService.getConceptByName("Offline Concepts");
        return offlineConcept != null && offlineConcept.getSetMembers().contains(concept);
    }

    private static String getLowerLevelName(AddressHierarchyLevel level, PersonAddress address) {
        switch (level.getAddressField()) {
            case ADDRESS_1:
                return address.getAddress1();
            case ADDRESS_2:
                return address.getAddress2();
            case ADDRESS_3:
                return address.getAddress3();
            case ADDRESS_4:
                return address.getAddress4();
            case ADDRESS_5:
                return address.getAddress5();
            case ADDRESS_6:
                return address.getAddress6();
            case STATE_PROVINCE:
                return address.getStateProvince();
            case CITY_VILLAGE:
                return address.getCityVillage();
            case COUNTRY:
                return address.getCountry();
            case COUNTY_DISTRICT:
                return address.getCountyDistrict();
            case POSTAL_CODE:
                return address.getPostalCode();
            default:
                return null;
        }
    }

    private static String getUserGeneratedIDValue(AddressHierarchyLevelWithLevelEntryName level) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        List<AddressHierarchyEntry> filteredEntries = addressHierarchyService.getAddressHierarchyEntriesByLevelAndLikeName(level.getLowerLevel(), level.getLowerLevelEntryName(), 1);
        return filteredEntries.size() != 0 ? filteredEntries.get(0).getUserGeneratedId() : null;
    }

    private static AddressHierarchyLevel getLowestLevel() {
        List<AddressHierarchyLevel> levels = getAddressHierarchyLevelsList();
        List<AddressHierarchyLevel> levels1 = new ArrayList<>();
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        for (AddressHierarchyLevel level : levels) {
            if (addressHierarchyService.getAddressHierarchyEntriesByLevel(level).size() != 0) {
                levels1.add(level);
            }
        }
        Collections.sort(levels1, Comparator.comparingInt(AddressHierarchyLevel::getLevelId));
        AddressHierarchyLevel lowerLevel = levels1.size() != 0 ? levels1.get(levels1.size() - 1) : null;
        return lowerLevel;
    }

    private static List<AddressHierarchyLevel> getAddressHierarchyLevelsList() {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        return addressHierarchyService.getAddressHierarchyLevels();
    }

    private static AddressHierarchyLevelWithLevelEntryName findLowestLevelAvailableInPersonAddress(AddressHierarchyLevel level, List<AddressHierarchyLevel> levels, PersonAddress personAddress) {
        String name = getLowerLevelName(level, personAddress);
        if (null == getLowerLevelName(level, personAddress)) {
            levels = levels.subList(0,levels.indexOf(level));
            return findLowestLevelAvailableInPersonAddress(levels.get(levels.size() - 1), levels, personAddress);
        } else {
            return new AddressHierarchyLevelWithLevelEntryName(level, name);
        }

    }

}
