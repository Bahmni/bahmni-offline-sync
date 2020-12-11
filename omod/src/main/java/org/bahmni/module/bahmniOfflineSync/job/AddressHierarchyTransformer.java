package org.bahmni.module.bahmniOfflineSync.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.constants.KeyMapping;
import org.bahmni.module.bahmniOfflineSync.eventLog.RowTransformer;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.emrapi.concept.EmrConceptService;
import org.openmrs.module.webservices.rest.SimpleObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressHierarchyTransformer implements RowTransformer {

    protected Log log = LogFactory.getLog(getClass());

    @Override
    public SimpleObject transform(String url) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        String uuid = getUuidFromUrl(url);
        if (uuid == null) {
            return null;
        }
        try {
            AddressHierarchyEntry addressHierarchyEntry = addressHierarchyService.getAddressHierarchyEntryByUuid(uuid);
            if(addressHierarchyEntry != null && addressHierarchyEntry.getAddressHierarchyEntryId() != null) {
                StringBuilder sbr = new StringBuilder();
                sbr.append("{");
                sbr.append(KeyMapping.AddressHierarchyEntry_ID + addressHierarchyEntry.getAddressHierarchyEntryId() + ",");
                sbr.append(KeyMapping.Name + addressHierarchyEntry.getName() + "\",");
                sbr.append(KeyMapping.Level_ID + addressHierarchyEntry.getLevel().getId() + ",");
                sbr.append(KeyMapping.AddressHierarchyLevel);
                sbr.append(KeyMapping.AddressHierarchyLevelID + addressHierarchyEntry.getLevel().getId() + ",");
                sbr.append(KeyMapping.AddressHierarchyLevelName + addressHierarchyEntry.getLevel().getName() + "\",");
                sbr.append(KeyMapping.AddressHierarchyLevelParent_ID + getParentLevelID(addressHierarchyEntry.getLevel().getParent()) + ",");
                sbr.append(KeyMapping.AddressHierarchyLevel_AddressFiled + addressHierarchyEntry.getLevel().getAddressField() + "\",");
                sbr.append(KeyMapping.AddressHierarchyLevel_Required + addressHierarchyEntry.getLevel().getRequired() + ",");
                sbr.append(KeyMapping.AddressHierarchyLevel_UUID + addressHierarchyEntry.getLevel().getUuid() + "\",");
                sbr.append(KeyMapping.AddressHierarchyLevel_ID + addressHierarchyEntry.getLevel().getId() + "},");
                sbr.append(KeyMapping.Parent_ID + getParentID(addressHierarchyEntry.getParent()) + ",");
                sbr.append(KeyMapping.UserGenerated_ID + addressHierarchyEntry.getUserGeneratedId() + "\",");
                sbr.append(KeyMapping.UUID + addressHierarchyEntry.getUuid()+"\"");
                sbr.append("}");
                SimpleObject simpleObject = new SimpleObject();
                simpleObject.add("address",  SimpleObject.parseJson(sbr.toString()));
                //log.error("converted response ->" + simpleObject.get("address"));
                return simpleObject.get("address");
            }
            else
                return null;
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String getUuidFromUrl(String url) {
        Pattern uuidPattern = Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        Matcher matcher = uuidPattern.matcher(url);
        return matcher.find() ? matcher.group(0) : null;
    }

    private Integer getParentLevelID(AddressHierarchyLevel parent)
    {
        return parent != null ? parent.getLevelId() : null ;
    }

    private Integer getParentID(AddressHierarchyEntry parent)
    {
        return parent != null ? parent.getId() : null ;
    }
}
