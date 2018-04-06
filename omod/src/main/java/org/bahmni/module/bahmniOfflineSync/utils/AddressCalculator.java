package org.bahmni.module.bahmniOfflineSync.utils;

import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressField;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class AddressCalculator {
    private AddressHierarchyService addressHierarchyService;
    private PersonService personService;

    public AddressCalculator() {
        addressHierarchyService = Context.getService(AddressHierarchyService.class);
        personService = Context.getPersonService();
    }


    private String getAddressCode(int level, AddressHierarchyEntry childAddress, PersonAddress personAddress) {
        AddressHierarchyEntry parentAddress;
        level = level + 1;
        String addressString;
        if (level <= addressHierarchyService.getBottomAddressHierarchyLevel().getLevelId()) {
            addressString = getAddressValueForLevel(level, personAddress);
        } else {
            return childAddress.getUserGeneratedId();
        }
        parentAddress = childAddress;
        AddressHierarchyLevel addressHierarchyLevel = addressHierarchyService.getAddressHierarchyLevel(level);
        List<AddressHierarchyEntry> childAddressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntriesByLevelAndNameAndParent(addressHierarchyLevel, addressString, parentAddress);
        return isEmpty(childAddressHierarchyEntry) ? parentAddress.getUserGeneratedId() :
                getAddressCode(level, childAddressHierarchyEntry.get(0), personAddress);
    }

    public void addTopDownAddressFor(Patient patient) {
        PersonAddress address = patient.getPersonAddress();
        String addressCode = "";

        if (address == null) {
            addressCode = null;
        } else {
            AddressHierarchyLevel addressHierarchyLevel = addressHierarchyService.getTopAddressHierarchyLevel();
            String addressString = getAddressValueForLevel(addressHierarchyLevel.getLevelId(), address);
            List<AddressHierarchyEntry> childAddressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(addressHierarchyLevel, addressString);
            addressCode = isEmpty(childAddressHierarchyEntry) ? null : getAddressCode(addressHierarchyLevel.getLevelId(), childAddressHierarchyEntry.get(0), address);
        }

        PersonAttributeType type = personService.getPersonAttributeTypeByName("addressCode");

        if (type != null) {
            PersonAttribute personAttribute = patient.getAttribute(type);
            if (personAttribute != null)
                personAttribute.setValue(addressCode);
            else
                patient.addAttribute(new PersonAttribute(type, addressCode));
        }
    }

    private boolean isEmpty(List<AddressHierarchyEntry> childAddressHierarchyEntry) {
        return CollectionUtils.isEmpty(childAddressHierarchyEntry);
    }

    private String getAddressValueForLevel(int addressHierarchyLevelID, PersonAddress personAddress) {
        AddressHierarchyLevel addressHierarchyLevel = addressHierarchyService.getAddressHierarchyLevel(addressHierarchyLevelID);
        AddressField addressField = addressHierarchyLevel.getAddressField();
        String address = "";
        if ("address1".equals(addressField.getName()))
            address = personAddress.getAddress1();
        if ("address2".equals(addressField.getName()))
            address = personAddress.getAddress2();
        if ("address3".equals(addressField.getName()))
            address = personAddress.getAddress3();
        if ("address4".equals(addressField.getName()))
            address = personAddress.getAddress4();
        if ("address5".equals(addressField.getName()))
            address = personAddress.getAddress5();
        if ("address6".equals(addressField.getName()))
            address = personAddress.getAddress6();
        if ("cityVillage".equals(addressField.getName()))
            address = personAddress.getCityVillage();
        if ("countyDistrict".equals(addressField.getName()))
            address = personAddress.getCountyDistrict();
        if ("stateProvince".equals(addressField.getName()))
            address = personAddress.getStateProvince();
        if ("country".equals(addressField.getName()))
            address = personAddress.getCountry();
        if ("postalCode".equals(addressField.getName()))
            address = personAddress.getPostalCode();
        if ("latitude".equals(addressField.getName()))
            address = personAddress.getLatitude();
        if ("longitude".equals(addressField.getName()))
            address = personAddress.getLongitude();

        return address;
    }
}
