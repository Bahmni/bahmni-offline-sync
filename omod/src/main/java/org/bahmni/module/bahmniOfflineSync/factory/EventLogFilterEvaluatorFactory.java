package org.bahmni.module.bahmniOfflineSync.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.filter.FilterEvaluator;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class EventLogFilterEvaluatorFactory {
    private String bahmniEventLogFilterEvaluator = "bahmniOfflineSync.eventlog.filterEvaluator";
    @Autowired
    @Qualifier("adminService")
    private AdministrationService administrationService;
    protected final Log log = LogFactory.getLog(getClass());

    public FilterEvaluator getFilterEvaluatorFromGlobalProperties() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        String filterEvaluatorClassName = administrationService.getGlobalProperty(bahmniEventLogFilterEvaluator);
        if (filterEvaluatorClassName != null && !filterEvaluatorClassName.isEmpty()) {
                return (FilterEvaluator) Context.loadClass(filterEvaluatorClassName).newInstance();
        }
        else
            log.error("Global Property BahmniEventLogFilterEvaluator is not configured in OpenMRS. Configure classpath for FilterEvaluator");
        return null;
    }
}
