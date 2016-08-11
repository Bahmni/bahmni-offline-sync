package org.bahmni.module.bahmniOfflineSync.filter;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationBasedFilterEvaluator implements FilterEvaluator{

    private static final String ATTRIBUTE_TYPE_NAME = "addressCode";

    private PatientService patientService;

    private EncounterService encounterService;

    public LocationBasedFilterEvaluator() {
        this.patientService = Context.getPatientService();
        this.encounterService = Context.getEncounterService();
    }

    private String evaluateFilterForPatient(String uuid) {
        String patientFilter = "";
        Patient patient = patientService.getPatientByUuid(uuid);
        if(patient!=null && patient.getAttribute(ATTRIBUTE_TYPE_NAME)!=null) {
            patientFilter = patient.getAttribute(ATTRIBUTE_TYPE_NAME).getValue();
        }
        return patientFilter;
    }


    private String evaluateFilterForEncounter(String uuid) {
        String filter = "";
        Encounter encounter = encounterService.getEncounterByUuid(uuid);
        if(encounter!=null)
            filter = evaluateFilterForPatient(encounter.getPatient().getUuid());
        return filter;

    }


    private String evaluateFilterForAddressHierarchy(String uuid) {
        String addressHierarchyFilter = "";
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(uuid);
        if (addressHierarchyEntry!=null && addressHierarchyEntry.getLevel() != null && addressHierarchyEntry.getLevel().getId() > 3) {
            addressHierarchyFilter =  addressHierarchyEntry.getUserGeneratedId();
        }
        return addressHierarchyFilter;
    }

    @Override
    public Map<String, String> getFilterForDevice(String providerUuid , String locationUuid) {
        Map categoryFilterMap = new HashMap();
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(locationUuid);
        String userGeneratedId = null;
        if(addressHierarchyEntry!=null)
            userGeneratedId = addressHierarchyEntry.getUserGeneratedId();
        categoryFilterMap.put("TransactionalData", userGeneratedId);
        categoryFilterMap.put("AddressHierarchy",getCatchmentNumberForAddressHierarchy(addressHierarchyEntry));
        categoryFilterMap.put("ParentAddressHierarchy", null);
        categoryFilterMap.put("offline-concepts", null);

        return categoryFilterMap;
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


    private String getCatchmentNumberForAddressHierarchy (AddressHierarchyEntry addressHierarchyEntry){
        String filterForAddressHierarchy = null;
        while(addressHierarchyEntry.getParent()!=null) {
            if(addressHierarchyEntry.getUserGeneratedId().length() == 6){
                filterForAddressHierarchy = addressHierarchyEntry.getUserGeneratedId();
                break;
            }
            addressHierarchyEntry = addressHierarchyEntry.getParent();
        }
        return filterForAddressHierarchy;
    }

    @Override
    public  String evaluateFilter(String uuid, String category) {
        String filter = "";
        if(category.equalsIgnoreCase("Patient"))
            filter = evaluateFilterForPatient(uuid);
        else if(category.equalsIgnoreCase("Encounter") || category.equalsIgnoreCase("SHREncounter"))
            filter =  evaluateFilterForEncounter(uuid);
        else if(category.equalsIgnoreCase("AddressHierarchy"))
            filter = evaluateFilterForAddressHierarchy(uuid);

        return filter;
    }
}
