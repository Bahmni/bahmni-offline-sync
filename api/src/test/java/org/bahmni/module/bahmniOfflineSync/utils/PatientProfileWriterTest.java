package org.bahmni.module.bahmniOfflineSync.utils;

import org.junit.Test;
import org.openmrs.module.webservices.rest.SimpleObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class PatientProfileWriterTest {
    @Test
    public void shouldWriteGivenSimpleObjectToWriter() throws Exception {
        StringWriter out = new StringWriter();
        PatientProfileWriter patientProfileWriter = new PatientProfileWriter(new BufferedWriter(out));
        SimpleObject person = new SimpleObject();
        person.put("given_name", "Super");
        person.put("uuid", "ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562");
        patientProfileWriter.write(person);
        assertEquals("{\"given_name\":\"Super\",\"uuid\":\"ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562\"}", out.toString());
    }

    @Test
    public void shouldNotCloseTheWriterAfterWrite() throws Exception {
        StringWriter out = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(out);
        PatientProfileWriter patientProfileWriter = new PatientProfileWriter(bufferedWriter);
        SimpleObject person = new SimpleObject();
        person.put("given_name", "Super");
        person.put("uuid", "ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562");
        patientProfileWriter.write(person);
        assertEquals("{\"given_name\":\"Super\",\"uuid\":\"ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562\"}", out.toString());

        bufferedWriter.flush(); //This is to make sure the stream is not closed
    }

}