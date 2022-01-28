package org.bahmni.module.bahmniOfflineSync.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bahmni.module.bahmniOfflineSync.utils.ConfigDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BahmniRegistrationConfig {

    private static final Logger log = Logger.getLogger(BahmniRegistrationConfig.class);

    private ConfigDirectory configDirectory;

    private static BahmniRegistrationConfig configLoader;

    private static JsonObject registrationConfig;

    private BahmniRegistrationConfig() {
       this.configDirectory = new ConfigDirectory();
        File configFile = configDirectory
                .getFileFromConfig("openmrs" + File.separator + "apps" + File.separator + "registration" + File.separator + "app.json");
        if (!configFile.exists()) {
            log.error("File not found " + configFile.getAbsolutePath());
        } else {
            try {
                InputStream is = new FileInputStream(configFile);
                String jsonTxt = IOUtils.toString( is );
                registrationConfig = new JsonParser().parse(jsonTxt).getAsJsonObject();
            } catch (IOException e) {
                log.error("Problem with the groovy class " + configFile, e);
            }
        }
    }


    public static BahmniRegistrationConfig getConfigLoader(){
        if (configLoader == null)
            configLoader = new BahmniRegistrationConfig();
        return configLoader;
    }

    public boolean isConfiguredForTopDownAddressHierarchy() {
        return registrationConfig.getAsJsonObject("config")!=null
                && registrationConfig.getAsJsonObject("config").getAsJsonObject("addressHierarchy")!=null
                && registrationConfig.getAsJsonObject("config").getAsJsonObject("addressHierarchy").get("showAddressFieldsTopDown").getAsBoolean();
    }

}
