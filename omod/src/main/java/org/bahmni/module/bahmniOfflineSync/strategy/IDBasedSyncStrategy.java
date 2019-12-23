package org.bahmni.module.bahmniOfflineSync.strategy;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class IDBasedSyncStrategy extends AbstractOfflineSyncStrategy {
    private static Logger logger = Logger.getLogger(IDBasedSyncStrategy.class);

    private LocationService locationService;

    private PatientService patientService;

    private EncounterService encounterService;

    private ConceptService conceptService;

    private IdentifierSourceService identifierSourceService;

    private String encounterURL = "/openmrs/ws/rest/v1/bahmnicore/bahmniencounter/%s?includeAll=true";

    private SessionFactory sessionFactory;

    private static String identifierPrefixQuery = "select distinct idgen_seq_id_gen.prefix from idgen_seq_id_gen join " +
            " idgen_log_entry entry on idgen_seq_id_gen.id = entry.source and entry.identifier = ? " +
            " join idgen_identifier_source src on src.id = entry.source and src.retired is not null";

    public IDBasedSyncStrategy() {
        this.patientService = Context.getPatientService();
        this.encounterService = Context.getEncounterService();
        this.locationService = Context.getLocationService();
        this.conceptService = Context.getConceptService();
        this.identifierSourceService = Context.getService(IdentifierSourceService.class);
        List<SessionFactory> sessionFactories = Context.getRegisteredComponents(SessionFactory.class);
        if(sessionFactories != null && sessionFactories.size() > 0)
            this.sessionFactory = sessionFactories.get(0);
    }

    protected String evaluateFilterForPatient(String uuid) {
        final Patient patient = patientService.getPatientByUuid(uuid);

        if (patient != null) {
            PatientIdentifier identifier = getPatientIdentifier(patient);
            if (identifier == null)
                return null;
            String localIdentifierPrefix = getFilterForLocalIdentifierSource(identifier.getIdentifier());
            if(localIdentifierPrefix != null)
                return localIdentifierPrefix;
            final List<IdentifierSource> identifierSources = identifierSourceService.getAllIdentifierSources(false);

            for (IdentifierSource src : identifierSources) {
                String prefix = ((SequentialIdentifierGenerator) src).getPrefix();
                if (StringUtils.isEmpty(prefix)) {
                    RuntimeException exception = new RuntimeException("Please set prefix for " + src.getName());
                    logger.error(exception);
                    throw exception;
                }
                if (identifier.getIdentifier().startsWith(prefix)) {
                    return prefix;
                }
            }
        }

        return null;
    }

    private String evaluateFilterForAddressHierarchy(String uuid) {
        return null;
    }

    private PatientIdentifier getPatientIdentifier(Patient patient) {
        String identifierTypeName = "Patient Identifier";
        PatientIdentifier identifier = patient.getPatientIdentifier(identifierTypeName);
        if (identifier == null) {
            Set<PatientIdentifier> piList = patient.getIdentifiers();
            for (PatientIdentifier pi : piList) {
                if (pi.getIdentifierType().getName().equals(identifierTypeName)) {
                    if (identifier == null)
                        identifier = pi;
                    else if (pi.getDateCreated().after(identifier.getDateCreated()))
                        identifier = pi;
                }
            }
        }
        return identifier;
    }

    private String evaluateFilterForEncounter(String uuid) {
        String filter = null;
        Encounter encounter = encounterService.getEncounterByUuid(uuid);

        if (encounter != null)
            filter = evaluateFilterForPatient(encounter.getPatient().getUuid());

        return filter;
    }

    public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
        Location location = locationService.getLocationByUuid(loginLocationUuid);
        Map<String, List<String>> categoryFilterMap = new HashMap();
        final Collection<LocationAttribute> activeAttributes = location.getActiveAttributes();
        ArrayList<String> filters = new ArrayList<String>();

        for (LocationAttribute attr : activeAttributes) {
            if (attr.getAttributeType().getName().equals("IdentifierSourceName")) {
                String prefix = getPrefixFor(attr.getValue().toString());
                if (prefix == null) {
                    RuntimeException exception = new RuntimeException("Please check [IdentifierSourceName] config for [" + location.getName() + "]");
                    logger.error(exception);
                    throw exception;
                }
                filters.add(prefix);
            }
        }

        categoryFilterMap.put("patient", filters);
        categoryFilterMap.put("encounter", filters);
        categoryFilterMap.put("addressHierarchy", new ArrayList<String>());
        categoryFilterMap.put("offline-concepts", new ArrayList<String>());
        categoryFilterMap.put("forms", new ArrayList<>());

        return categoryFilterMap;
    }

    private String getPrefixFor(String identifierSourceName) {
        List<IdentifierSource> allIdentifierSources = identifierSourceService.getAllIdentifierSources(false);
        for (IdentifierSource identifierSource : allIdentifierSources) {
            if (identifierSource.getName().equals(identifierSourceName))
                return ((SequentialIdentifierGenerator) identifierSource).getPrefix();
        }
        return null;
    }

    @Override
    public List<String> getEventCategoriesList() {
        List<String> eventCategoryList = new ArrayList();

        eventCategoryList.add("patient");
        eventCategoryList.add("encounter");
        eventCategoryList.add("addressHierarchy");
        eventCategoryList.add("offline-concepts");
        eventCategoryList.add("forms");

        return eventCategoryList;
    }

    @Override
    public List<EventLog> getEventLogsFromEventRecords(List<EventRecord> eventRecords) {
        List<EventLog> eventLogs = new ArrayList<EventLog>();

        for (EventRecord er : eventRecords) {
            EventLog eventLog = new EventLog(er.getUuid(), er.getCategory(), er.getTimeStamp(), er.getContents(), null, er.getUuid());
            String category = er.getCategory();
            String uuid = getUuidFromURL(er.getContents());
            String filter = null;

            if (!uuid.isEmpty()) {
                if ((category.equalsIgnoreCase("all-concepts"))) {
                    if (isOfflineConceptEvent(uuid)) {
                        eventLog.setCategory("offline-concepts");
                    } else {
                        eventLog.setCategory("concepts");
                    }
                }

                if (category.equalsIgnoreCase("Patient") || category.equalsIgnoreCase("LabOrderResults"))
                    filter = evaluateFilterForPatient(uuid);
                else if (category.equalsIgnoreCase("Encounter")) {
                    filter = evaluateFilterForEncounter(uuid);
                    eventLog.setObject(String.format(encounterURL, uuid));
                } else if (category.equalsIgnoreCase("addressHierarchy"))
                    filter = evaluateFilterForAddressHierarchy(uuid);
            }
            eventLog.setFilter(filter);

            eventLogs.add(eventLog);
        }

        return eventLogs;
    }

    private boolean isOfflineConceptEvent(String eventUuid) {
        final Concept concept = conceptService.getConceptByUuid(eventUuid);
        final Concept offlineConcept = conceptService.getConceptByName("Offline Concepts");
        return offlineConcept != null && offlineConcept.getSetMembers().contains(concept);
    }

    protected String getFilterForLocalIdentifierSource(String identifier) {
        Session session = null;
        try {
            session = sessionFactory.getCurrentSession();
            session.beginTransaction();
            String prefix = session.doReturningWork(new ReturningWork<String>() {
                @Override
                public String execute(Connection conn) throws SQLException {
                    PreparedStatement pStmt = null;
                    try {
                        pStmt = conn.prepareStatement(identifierPrefixQuery);
                        pStmt.setString(1, identifier);
                        ResultSet resultSet = pStmt.executeQuery();
                        if(resultSet.next())
                            return resultSet.getString(1);
                        return null;
                    }
                    finally {
                        pStmt.close();
                    }
                }
            });
            session.getTransaction().commit();
            return prefix;
        }
        catch(HibernateException e) {
            RuntimeException exception = new RuntimeException("Error while executing query for getting local identifier [" + identifier + "]");
            logger.error(exception);
            throw exception;
        }
    }
}
