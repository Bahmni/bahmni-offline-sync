package org.bahmni.module.bahmniOfflineSync.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttributeType;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

import static org.junit.Assert.assertNull;

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

    @Test
    public void shouldNotHaveAddressCodeAttributeIfThePatientDoesNotHaveAddress() {
        AddressCalculator addressCalculator = new AddressCalculator();
        Patient patient = new Patient();

        addressCalculator.addTopDownAddressFor(patient);
        assertNull(patient.getAttribute("addressCode"));
    }

    @Test
    public void shouldNotHaveAddressCodeAttributeIfTheGivenCountryIsNotExistInDatabase() {
        AddressCalculator addressCalculator = new AddressCalculator();
        Patient patient = buildPatientWithAddress("randomText", "Telangana", "Secunderabad", "Gachibowli");

        addressCalculator.addTopDownAddressFor(patient);
        assertNull(patient.getAttribute("addressCode"));
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