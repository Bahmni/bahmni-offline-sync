package org.bahmni.module.bahmniOfflineSync.filter;


import java.util.List;
import java.util.Map;

public interface FilterEvaluator {
   public String evaluateFilter(String uuid, String category);
   public Map<String,String> getFilterForDevice(String provider, String location);
   public List<String> getEventCategoriesList();

}
