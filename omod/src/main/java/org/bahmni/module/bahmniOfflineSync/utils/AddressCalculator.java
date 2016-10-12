package org.bahmni.module.bahmniOfflineSync.utils;

import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;

import java.util.List;

public class AddressCalculator {
    private AddressHierarchyService service;
    private PersonService personService;

    public AddressCalculator() {
        service = Context.getService(AddressHierarchyService.class);
        personService = Context.getPersonService();
    }

    private AddressHierarchyEntry getParent(AddressHierarchyLevel level, String name) {
        List<AddressHierarchyEntry> entries = service.getAddressHierarchyEntriesByLevelAndName(level, name);
        return entries.size() > 0 ? entries.get(0) : null;
    }

    private String getAddressCode(int level, String parentAddress, String childAddress) {
        AddressHierarchyEntry parent = getParent(service.getAddressHierarchyLevel(level), parentAddress);

        return (parent != null) ?
                service.getAddressHierarchyEntriesByLevelAndNameAndParent(
                        service.getAddressHierarchyLevel(level + 1),
                        childAddress,
                        parent).get(0).getUserGeneratedId()
                : null;
    }

    public void addTopDownAddressFor(Patient p) {
        PersonAddress address = p.getPersonAddress();
        String addressCode = "";

        for (int level = service.getAddressHierarchyLevelsCount() - 2; level >= 1; level--) {
            addressCode = getAddressCode(level, address.getAddress3(), address.getAddress2());
            if (addressCode != null) break;
            addressCode = getAddressCode(level, address.getAddress4(), address.getAddress3());
            if (addressCode != null) break;
            addressCode = getAddressCode(level, address.getAddress5(), address.getAddress4());
            if (addressCode != null) break;
            addressCode = getAddressCode(level, address.getCountyDistrict(), address.getAddress5());
            if (addressCode != null) break;
            addressCode = getAddressCode(level, address.getStateProvince(), address.getCountyDistrict());
            if (addressCode != null) break;
        }

        PersonAttributeType type = personService.getPersonAttributeTypeByName("addressCode");
        if (type != null) {
            p.addAttribute(new PersonAttribute(type, addressCode));
        }
    }
}
