package org.bahmni.module.bahmniOfflineSync.advice;

import org.bahmni.module.bahmniOfflineSync.utils.AddressCalculator;
import org.openmrs.Patient;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

public class PatientSaveAdvice implements MethodBeforeAdvice {
    private static final String SAVE_PATIENT_METHOD = "savePatient";

    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        if (method.getName().equals(SAVE_PATIENT_METHOD)) {
            new AddressCalculator().addTopDownAddressFor((Patient) args[0]);
        }
    }
}
