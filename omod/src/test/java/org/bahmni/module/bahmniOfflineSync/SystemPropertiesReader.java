package org.bahmni.module.bahmniOfflineSync;

public class SystemPropertiesReader  {
    public String getProperty(String key) {
        return System.getProperty(key);
    }
}
