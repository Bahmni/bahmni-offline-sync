package org.bahmni.module.bahmniOfflineSync.filter;

import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.springframework.util.StringUtils;

import java.util.*;

public class LocationBasedFilterEvaluator implements FilterEvaluator {

    private static final String ATTRIBUTE_TYPE_NAME = "addressCode";
    private LocationService locationService;

    private PatientService patientService;

    private EncounterService encounterService;

    public LocationBasedFilterEvaluator() {
        this.patientService = Context.getPatientService();
        this.encounterService = Context.getEncounterService();
        this.locationService = Context.getLocationService();
    }

    private String evaluateFilterForPatient(String uuid) {
        String patientFilter = "";
        Patient patient = patientService.getPatientByUuid(uuid);

        if (patient != null && patient.getAttribute(ATTRIBUTE_TYPE_NAME) != null) {
            patientFilter = patient.getAttribute(ATTRIBUTE_TYPE_NAME).getValue();
        }

        return patientFilter;
    }

    private String evaluateFilterForEncounter(String uuid) {
        String filter = "";
        Encounter encounter = encounterService.getEncounterByUuid(uuid);
        if (encounter != null)
            filter = evaluateFilterForPatient(encounter.getPatient().getUuid());
        return filter;
    }

    private String evaluateFilterForAddressHierarchy(String uuid) {
        String addressHierarchyFilter = "";
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(uuid);
        if (addressHierarchyEntry != null && addressHierarchyEntry.getLevel() != null && addressHierarchyEntry.getLevel().getId() > 3) {
            addressHierarchyFilter = addressHierarchyEntry.getUserGeneratedId();
        }
        return addressHierarchyFilter;
    }


    public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
        Map<String, List<String>> categoryFilterMap = new HashMap();
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(addressUuid);
        List transactionalDataFilters = getTransactionalDataFilters(loginLocationUuid, addressHierarchyService, addressHierarchyEntry);
        categoryFilterMap.put("TransactionalData", transactionalDataFilters);
        categoryFilterMap.put("AddressHierarchy", getFiltersForAddressHierarchy(addressHierarchyEntry));
        categoryFilterMap.put("ParentAddressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
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
                    wardIDs.add(childAddressHierarchyEntry.getUserGeneratedId());
                }
            }
        } else {
            for (AddressHierarchyEntry childAddressHierarchyEntry : childAddressHierarchyEntries) {
                wardIDs.add(childAddressHierarchyEntry.getUserGeneratedId());
            }
        }
        return wardIDs;
    }

    private String trim(String content) {
        content = content.trim();
        return content.replaceAll("(,\\s*)",",");
    }

    @Override
    public List<String> getEventCategoriesList() {
        List<String> eventCategoryList = new ArrayList();

        eventCategoryList.add("TransactionalData");
        eventCategoryList.add("AddressHierarchy");
        eventCategoryList.add("ParentAddressHierarchy");
        eventCategoryList.add("offline-concepts");

        return eventCategoryList;
    }

    private List<String> getFiltersForAddressHierarchy(AddressHierarchyEntry addressHierarchyEntry){
        List addressHierarchyFilters = new ArrayList();
        while(addressHierarchyEntry.getParent()!=null) {
            if(addressHierarchyEntry.getUserGeneratedId().length() == 6){
                addressHierarchyFilters.add(addressHierarchyEntry.getUserGeneratedId());
                break;
            }
            addressHierarchyEntry = addressHierarchyEntry.getParent();
        }
        return addressHierarchyFilters;
    }

    @Override
    public String evaluateFilter(String uuid, String category) {
        String filter = "";

        if (category.equalsIgnoreCase("Patient"))
            filter = evaluateFilterForPatient(uuid);
        else if (category.equalsIgnoreCase("Encounter") || category.equalsIgnoreCase("SHREncounter"))
            filter = evaluateFilterForEncounter(uuid);
        else if (category.equalsIgnoreCase("AddressHierarchy"))
            filter = evaluateFilterForAddressHierarchy(uuid);

        return filter;
    }
}
