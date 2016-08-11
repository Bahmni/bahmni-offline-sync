package org.bahmni.module.bahmniOfflineSync.web.v1.controller;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.factory.EventLogFilterEvaluatorFactory;
import org.bahmni.module.bahmniOfflineSync.filter.FilterEvaluator;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/eventlog/filter")
public class EventLogFilterController extends BaseRestController {
    @Autowired
    EventLogFilterEvaluatorFactory eventLogFilterEvaluatorFactory;

    private FilterEvaluator filterEvaluator;
    protected final Log log = LogFactory.getLog(getClass());


    @RequestMapping(method = RequestMethod.GET, value = "/{category}/{uuid}")
    @ResponseBody
    public String getFilter(@PathVariable("uuid") String uuid,@PathVariable("category") String category ) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        filterEvaluator = eventLogFilterEvaluatorFactory.getFilterEvaluatorFromGlobalProperties();
        if(filterEvaluator!=null) {
            return filterEvaluator.evaluateFilter(uuid,category);
        }
        else
            throw new RuntimeException("Global Property BahmniEventLogFilterEvaluator is not configured in OpenMRS. Configure classpath for FilterEvaluator");
    }


    @RequestMapping(method = RequestMethod.GET, value = "/markers/{providerUuid}/{locationUuid}", produces={ "application/json"})
    @ResponseBody
    public Map<String, String> getFilterForDevice(@PathVariable("providerUuid") String providerUuid,@PathVariable("locationUuid") String locationUuid ) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        filterEvaluator = eventLogFilterEvaluatorFactory.getFilterEvaluatorFromGlobalProperties();
        if(filterEvaluator!=null) {
            return filterEvaluator.getFilterForDevice(providerUuid, locationUuid);
        }
        else
            throw new RuntimeException("Global Property BahmniEventLogFilterEvaluator is not configured in OpenMRS. Configure classpath for FilterEvaluator");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/category")
    @ResponseBody
    public List<String> getCategoryList() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        filterEvaluator = eventLogFilterEvaluatorFactory.getFilterEvaluatorFromGlobalProperties();
        if(filterEvaluator!=null) {
            return filterEvaluator.getEventCategoriesList();
        }
        else
            throw new RuntimeException("Global Property BahmniEventLogFilterEvaluator is not configured in OpenMRS. Configure classpath for FilterEvaluator");
    }


}
