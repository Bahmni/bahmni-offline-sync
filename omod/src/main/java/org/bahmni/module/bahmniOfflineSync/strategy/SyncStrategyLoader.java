package org.bahmni.module.bahmniOfflineSync.strategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SyncStrategyLoader {
    private String syncStrategyProp = "bahmniOfflineSync.strategy";
    @Autowired
    @Qualifier("adminService")
    private AdministrationService administrationService;
    protected final Log log = LogFactory.getLog(getClass());

    public OfflineSyncStrategy getFilterEvaluatorFromGlobalProperties() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        String syncStrategyClassPath = administrationService.getGlobalProperty(syncStrategyProp);
        if (syncStrategyClassPath != null && !syncStrategyClassPath.isEmpty()) {
                return (OfflineSyncStrategy) Context.loadClass(syncStrategyClassPath).newInstance();
        }
        else
            log.error("Global Property " + syncStrategyProp + " is not configured in OpenMRS. Configure classpath for OfflineSyncStrategy");
        return null;
    }

    public String getKeyValueFromGlobalProperties(String key)  {

        String valueForKey = administrationService.getGlobalProperty(key);
        if (valueForKey != null && !valueForKey.isEmpty()) {
            log.error("key value is ->" + valueForKey);
            return valueForKey;
        }
        else{
            log.error("Global Property " + key + " is not configured in OpenMRS. ");
        return null;}
    }
}
