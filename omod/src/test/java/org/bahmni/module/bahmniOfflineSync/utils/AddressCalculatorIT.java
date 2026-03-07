/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at https://www.bahmni.org/license/mplv2hd.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

package org.bahmni.module.bahmniOfflineSync.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttributeType;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

public class AddressCalculatorIT extends BaseModuleWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        executeDataSet("addressHierarchy.xml");
        executeDataSet("personAttributeType.xml");
    }

    @Test
    public void shouldAddAddressCodeCalculatedTopDownForStreet() throws Exception {
        AddressCalculator addressCalculator = new AddressCalculator();
        Patient patient = buildPatientWithAddress("India", "Telangana", "Hyderabad", "Gachibowli");

        addressCalculator.addTopDownAddressFor(patient);
        Assert.assertEquals(patient.getAttribute("addressCode").getValue(), "40");
    }

    @Test
    public void shouldAddAddressCodeCalculatedTopDownForSameStreetInAnotherCity() throws Exception {
        AddressCalculator addressCalculator = new AddressCalculator();
        Patient patient = buildPatientWithAddress("India", "Telangana", "Secunderabad", "Gachibowli");

        addressCalculator.addTopDownAddressFor(patient);
        Assert.assertEquals(patient.getAttribute("addressCode").getValue(), "45");
    }

    @Test
    public void shouldAddAddressCodeCalculatedTopDownForCity() throws Exception {
        AddressCalculator addressCalculator = new AddressCalculator();
        Patient patient = buildPatientWithAddress("India", "Telangana", "Secunderabad", null);

        addressCalculator.addTopDownAddressFor(patient);
        Assert.assertEquals(patient.getAttribute("addressCode").getValue(), "35");
    }

    private Patient buildPatientWithAddress(String country, String state, String city, String street) {

        PersonAttributeType addressCode = new PersonAttributeType();
        addressCode.setName("adderssCode");

        PersonAddress personAddress = new PersonAddress();
        personAddress.setCountry(country);
        personAddress.setStateProvince(state);
        personAddress.setCityVillage(city);
        personAddress.setAddress4(street);

        Person person = new Person();
        person.addAddress(personAddress);

        return new Patient(person);
    }
}