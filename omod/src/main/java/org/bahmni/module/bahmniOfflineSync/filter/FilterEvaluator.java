package org.bahmni.module.bahmniOfflineSync.filter;


import java.util.List;
import java.util.Map;

public interface FilterEvaluator {
   public String evaluateFilter(String uuid, String category);
   public Map<String,List<String>> getFilterForDevice(String provider, String addressUuid, String loginLocationUuid);
   public List<String> getEventCategoriesList();

}
