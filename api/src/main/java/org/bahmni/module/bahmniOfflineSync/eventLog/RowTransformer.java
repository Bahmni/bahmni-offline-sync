/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at https://www.bahmni.org/license/mplv2hd.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

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
