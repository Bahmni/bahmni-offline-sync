package org.bahmni.module.bahmniOfflineSync.utils;

import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;

 public class AddressHierarchyLevelWithLevelEntryName {

    AddressHierarchyLevel lowerLevel;
    String lowerLevelEntryName;

     public AddressHierarchyLevelWithLevelEntryName() {
     }

     public AddressHierarchyLevelWithLevelEntryName(AddressHierarchyLevel lowerLevel, String lowerLevelEntryName) {
        this.lowerLevel = lowerLevel;
        this.lowerLevelEntryName = lowerLevelEntryName;
    }

    public AddressHierarchyLevel getLowerLevel() {
        return lowerLevel;
    }

    public void setLowerLevel(AddressHierarchyLevel lowerLevel) {
        this.lowerLevel = lowerLevel;
    }

    public String getLowerLevelEntryName() {
        return lowerLevelEntryName;
    }

    public void setLowerLevelEntryName(String lowerLevelEntryName) {
        this.lowerLevelEntryName = lowerLevelEntryName;
    }

}
