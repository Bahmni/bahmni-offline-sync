package org.bahmni.module.bahmniOfflineSync.utils;



import java.io.File;

public class ConfigDirectory {

    public File getFileFromConfig(String relativePath) {
        return new File("/var/www","bahmni_config"+ File.separator+relativePath);
    }
}
