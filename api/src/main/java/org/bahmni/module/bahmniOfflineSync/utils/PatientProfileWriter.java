/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at https://www.bahmni.org/license/mplv2hd.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

package org.bahmni.module.bahmniOfflineSync.utils;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.webservices.rest.SimpleObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class PatientProfileWriter extends Writer {

    private BufferedWriter writer;

    public PatientProfileWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void write(SimpleObject object) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.writeValue(writer, object);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
