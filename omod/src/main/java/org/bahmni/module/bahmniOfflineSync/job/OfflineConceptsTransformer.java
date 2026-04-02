package org.bahmni.module.bahmniOfflineSync.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.eventLog.RowTransformer;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.concept.EmrConceptService;
import org.openmrs.module.webservices.rest.SimpleObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OfflineConceptsTransformer implements RowTransformer {

    protected Log log = LogFactory.getLog(getClass());

    @Override
    public SimpleObject transform(String url) {
        EmrConceptService conceptService = Context.getService(EmrConceptService.class);
        String uuid = getUuidFromUrl(url);
        if (uuid == null) {
            return null;
        }
        try {
            Concept concept = conceptService.getConcept(uuid);
            if(null != concept) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
                String json2 = objectMapper.writeValueAsString(concept);
                log.error("converted response2 ->" + json2);
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                ow.canSerialize(Concept.class);
                String json = ow.writeValueAsString(concept);
                StringBuilder sbr = new StringBuilder();
                SimpleObject simpleObject = new SimpleObject();
                simpleObject.add("offlineConcept", SimpleObject.parseJson(sbr.toString()));
                log.error("converted response ->" + json);
                return simpleObject.get("offlineConcept");
            }
            else return null;
        }
        catch (Exception e) {
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
}
