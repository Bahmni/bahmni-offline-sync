package org.bahmni.module.bahmniOfflineSync.eventLog;

import org.openmrs.module.webservices.rest.SimpleObject;

public interface RowTransformer {
    /**
     *
     * @param key using which an openmrs object is retrieved.
     * @return The string that needs to be written as bytes by subsequent writer.
     */

    SimpleObject transform(String key);
}
