package org.bahmni.module.bahmniOfflineSync.job;

import org.bahmni.module.bahmniOfflineSync.eventLog.RowTransformer;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.patient.PatientProfile;
import org.openmrs.module.emrapi.rest.resource.PatientProfileResource;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.api.RestService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatientProfileTransformer implements RowTransformer {
    @Override
    public SimpleObject transform(String url) {
        PatientProfileResource patientProfileResource = (PatientProfileResource) Context.getService(RestService.class).getResourceBySupportedClass(PatientProfile.class);
        String uuid = getUuidFromUrl(url);
        SimpleObject profile = (SimpleObject) patientProfileResource.retrieve(uuid, null);
        return profile.get("patient");
    }

    private String getUuidFromUrl(String url) {
        Pattern uuidPattern = Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        Matcher matcher = uuidPattern.matcher(url);
        return matcher.find() ? matcher.group(0) : "";
    }
}
