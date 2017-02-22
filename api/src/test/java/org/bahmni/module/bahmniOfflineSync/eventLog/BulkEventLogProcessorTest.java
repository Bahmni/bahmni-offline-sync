//package org.bahmni.module.bahmniOfflineSync.eventLog;
//
//import org.bahmni.module.bahmniOfflineSync.utils.PatientProfileWriter;
//import org.junit.Before;
//import org.junit.Test;
//import org.openmrs.Person;
//import org.openmrs.api.context.Context;
//import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
//import org.openmrs.module.webservices.rest.SimpleObject;
//import org.openmrs.test.BaseModuleContextSensitiveTest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import java.io.StringWriter;
//import java.sql.Connection;
//
//import static org.junit.Assert.assertEquals;
//
//public class BulkEventLogProcessorTest extends BaseModuleContextSensitiveTest {
//    @Autowired
//    private PlatformTransactionManager platformTransactionManager;
//
//    private AtomFeedSpringTransactionManager transactionManager;
//    private Connection connection;
//
//    @Before
//    public void setUp() throws Exception {
//        transactionManager = new AtomFeedSpringTransactionManager(platformTransactionManager);
//        connection = transactionManager.getConnection();
//    }
//
//    @Test
//    public void shouldIterateThroughAllPatient() throws Exception {
//        String sql = "select uuid from person";
//        PatientProfileWriter writer = new PatientProfileWriter(new StringWriter());
//        BulkEventLogProcessor bulkEventLogProcessor = new BulkEventLogProcessor(sql, connection, new PersonNameTransform(), writer);
//        bulkEventLogProcessor.process();
//        String expected = "{ given_name: Super, uuid: ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562},{ given_name: Horatio, uuid: da7f524f-27ce-4bb2-86d6-6d1d05312bd5},{ given_name: Johnny, uuid: a7e04421-525f-442f-8138-05b619d16def},{ given_name: Collet, uuid: 5946f880-b197-400b-9caa-a3c661d23041},{ given_name: Anet, uuid: 8adf539e-4b5a-47aa-80c0-ba1025c957fa},{ given_name: Jimmy, uuid: c04ee3c8-b68f-43cc-bff3-5a831ee7225f},{ given_name: , uuid: 29BF6F2B-0CE4-4CDB-A9BD-231E1B01C044},{ given_name: , uuid: BB3D4E98-1804-471E-B4B5-5180C1ECCEA0},{ given_name: , uuid: 86526ed6-3c11-11de-a0ba-001e378eb67f},{ given_name: Bruno, uuid: df8ae447-6745-45be-b859-403241d9913c},{ given_name: Hippocrates, uuid: 341b4e41-790c-484f-b6ed-71dc8da222de},{ given_name: , uuid: 86526ed6-3c11-11de-a0ba-001e378eb67e}";
//        assertEquals(expected, writer.toString());
//    }
//
//    class PersonNameTransform implements RowTransformer {
//
//        @Override
//        public SimpleObject transform(String uuid) {
//            Person person = Context.getPersonService().getPersonByUuid(uuid);
//            SimpleObject simpleObject = new SimpleObject();
//            simpleObject.put("given_name", person.getGivenName());
//            simpleObject.put("uuid", uuid);
////            return String.format("{ given_name: %s, uuid: %s}", person.getGivenName(), uuid);
//            return simpleObject;
//        }
//    }
//}
