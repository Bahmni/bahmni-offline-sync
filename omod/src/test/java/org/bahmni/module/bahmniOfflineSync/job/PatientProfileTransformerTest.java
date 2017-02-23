package org.bahmni.module.bahmniOfflineSync.job;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.patient.PatientProfile;
import org.openmrs.module.emrapi.rest.resource.PatientProfileResource;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@PrepareForTest({Context.class})
@RunWith(PowerMockRunner.class)
public class PatientProfileTransformerTest {
    @Mock
    RestService restService;

    @Mock
    PatientProfileResource patientProfileResource;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(Context.class);
    }

    @Test
    public void shouldReturnPatientProfileAsSimpleObjectForGivenUUID() throws Exception {
        SimpleObject person = new SimpleObject();
        SimpleObject patient = new SimpleObject();
        patient.put("uuid", "83668ce6-c22b-416c-f5c5-d223375c7aea");
        patient.put("given_name", "given name");
        patient.put("last_name", "last name");
        person.put("patient", patient);

        when(Context.getService(RestService.class)).thenReturn(restService);
        when(restService.getResourceBySupportedClass(PatientProfile.class)).thenReturn(patientProfileResource);
        when(patientProfileResource.retrieve("83668ce6-c22b-416c-f5c5-d223375c7aea", null)).thenReturn(person);

        PatientProfileTransformer patientProfileTransformer = new PatientProfileTransformer();
        SimpleObject patientProfile = patientProfileTransformer.transform("/openmrs/ws/rest/v1/patient/83668ce6-c22b-416c-f5c5-d223375c7aea?v=full");
        assertEquals(patient, patientProfile);

    }
}