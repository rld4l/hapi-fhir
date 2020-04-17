package ca.uhn.fhir.jpa.dao.lastn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.config.TestR4ConfigWithElasticsearchClient;
import ca.uhn.fhir.jpa.dao.data.IObservationIndexedCodeCodeableConceptSearchParamDao;
import ca.uhn.fhir.jpa.dao.data.IObservationIndexedSearchParamLastNDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.jpa.search.lastn.ElasticsearchSvcImpl;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestR4ConfigWithElasticsearchClient.class })
public class IntegratedObservationIndexedSearchParamLastNTest {

    @Autowired
    IObservationIndexedSearchParamLastNDao myResourceIndexedObservationLastNDao;

    @Autowired
    IObservationIndexedCodeCodeableConceptSearchParamDao myCodeableConceptIndexedSearchParamNormalizedDao;

    @Autowired
	 ObservationLastNIndexPersistR4Svc myObservationLastNIndexPersistR4Svc;

    @Autowired
    private ElasticsearchSvcImpl elasticsearchSvc;

    @Autowired
	 private IFhirSystemDao<Bundle, Meta> myDao;

    final String RESOURCEPID = "123";
    final String SUBJECTID = "4567";
    final String CATEGORYFIRSTCODINGSYSTEM = "http://mycodes.org/fhir/observation-category";
    final String FIRSTCATEGORYFIRSTCODINGCODE = "test-heart-rate";

    final String CODEFIRSTCODINGSYSTEM = "http://mycodes.org/fhir/observation-code";
    final String CODEFIRSTCODINGCODE = "test-code";

    @Before
    public void before() throws IOException {

        myResourceIndexedObservationLastNDao.deleteAll();
        myCodeableConceptIndexedSearchParamNormalizedDao.deleteAll();

    }

    @After
    public void after() {

        myResourceIndexedObservationLastNDao.deleteAll();
        myCodeableConceptIndexedSearchParamNormalizedDao.deleteAll();

    }

    @Test
    public void testIndexObservationSingle() throws IOException {

        Observation myObservation = new Observation();
        String resourcePID = "123";
        myObservation.setId(resourcePID);
        Reference subjectId = new Reference("4567");
        myObservation.setSubject(subjectId);
        Date effectiveDtm = new Date();
        myObservation.setEffective(new DateTimeType(effectiveDtm));

        // Add three CodeableConcepts for category
        List<CodeableConcept> categoryConcepts = new ArrayList<>();
        // Create three codings and first category CodeableConcept
        List<Coding> category1 = new ArrayList<>();
        CodeableConcept categoryCodeableConcept1 = new CodeableConcept().setText("Test Codeable Concept Field for first category");
        category1.add(new Coding("http://mycodes.org/fhir/observation-category", "test-heart-rate", "test-heart-rate display"));
        category1.add(new Coding("http://myalternatecodes.org/fhir/observation-category", "test-alt-heart-rate", "test-alt-heart-rate display"));
        category1.add(new Coding("http://mysecondaltcodes.org/fhir/observation-category", "test-2nd-alt-heart-rate", "test-2nd-alt-heart-rate display"));
        categoryCodeableConcept1.setCoding(category1);
        categoryConcepts.add(categoryCodeableConcept1);
        // Create three codings and second category CodeableConcept
        List<Coding> category2 = new ArrayList<>();
        CodeableConcept categoryCodeableConcept2 = new CodeableConcept().setText("Test Codeable Concept Field for for second category");
        category2.add(new Coding("http://mycodes.org/fhir/observation-category", "test-vital-signs", "test-vital-signs display"));
        category2.add(new Coding("http://myalternatecodes.org/fhir/observation-category", "test-alt-vitals", "test-alt-vitals display"));
        category2.add(new Coding("http://mysecondaltcodes.org/fhir/observation-category", "test-2nd-alt-vitals", "test-2nd-alt-vitals display"));
        categoryCodeableConcept2.setCoding(category2);
        categoryConcepts.add(categoryCodeableConcept2);
        // Create three codings and third category CodeableConcept
        List<Coding> category3 = new ArrayList<>();
        CodeableConcept categoryCodeableConcept3 = new CodeableConcept().setText("Test Codeable Concept Field for third category");
        category3.add(new Coding("http://mycodes.org/fhir/observation-category", "test-vitals-panel", "test-vitals-panel display"));
        category3.add(new Coding("http://myalternatecodes.org/fhir/observation-category", "test-alt-vitals-panel", "test-alt-vitals-panel display"));
        category3.add(new Coding("http://mysecondaltcodes.org/fhir/observation-category", "test-2nd-alt-vitals-panel", "test-2nd-alt-vitals-panel display"));
        categoryCodeableConcept3.setCoding(category3);
        categoryConcepts.add(categoryCodeableConcept3);
        myObservation.setCategory(categoryConcepts);

        // Create CodeableConcept for Code with three codings.
		 // TODO: Temporarily limit this to two codings until we sort out how to manage multiple codings
        String observationCodeText = "Test Codeable Concept Field for Code";
        CodeableConcept codeableConceptField = new CodeableConcept().setText(observationCodeText);
        codeableConceptField.addCoding(new Coding("http://mycodes.org/fhir/observation-code", "test-code", "test-code display"));
//        codeableConceptField.addCoding(new Coding("http://myalternatecodes.org/fhir/observation-code", "test-alt-code", "test-alt-code display"));
//        codeableConceptField.addCoding(new Coding("http://mysecondaltcodes.org/fhir/observation-code", "test-second-alt-code", "test-second-alt-code display"));
        myObservation.setCode(codeableConceptField);

        myObservationLastNIndexPersistR4Svc.indexObservation(myObservation, "4567");

        SearchParameterMap searchParameterMap = new SearchParameterMap();
        ReferenceParam subjectParam = new ReferenceParam("Patient", "", SUBJECTID);
        searchParameterMap.add(Observation.SP_SUBJECT, new ReferenceAndListParam().addAnd(new ReferenceOrListParam().addOr(subjectParam)));
        TokenParam categoryParam = new TokenParam(CATEGORYFIRSTCODINGSYSTEM, FIRSTCATEGORYFIRSTCODINGCODE);
		  searchParameterMap.add(Observation.SP_CATEGORY, new TokenAndListParam().addAnd(new TokenOrListParam().addOr(categoryParam)));
        TokenParam codeParam = new TokenParam(CODEFIRSTCODINGSYSTEM, CODEFIRSTCODINGCODE);
        searchParameterMap.add(Observation.SP_CODE, new TokenAndListParam().addAnd(new TokenOrListParam().addOr(codeParam)));

		  List<String> observationIdsOnly = elasticsearchSvc.executeLastN(searchParameterMap, 3);

        assertEquals(1, observationIdsOnly.size());
        assertEquals(RESOURCEPID, observationIdsOnly.get(0));

    }


    @Test
    public void testIndexObservationMultiple() {

        // Create two CodeableConcept values each for a Code with three codings.
        CodeableConcept codeableConceptField1 = new CodeableConcept().setText("Test Codeable Concept Field for First Code");
        codeableConceptField1.addCoding(new Coding("http://mycodes.org/fhir/observation-code", "test-code-1", "test-code-1 display"));
        codeableConceptField1.addCoding(new Coding("http://myalternatecodes.org/fhir/observation-code", "test-alt-code-1", "test-alt-code-1 display"));
        codeableConceptField1.addCoding(new Coding("http://mysecondaltcodes.org/fhir/observation-code", "test-second-alt-code-1", "test-second-alt-code-1 display"));

        CodeableConcept codeableConceptField2 = new CodeableConcept().setText("Test Codeable Concept Field for Second Code");
        codeableConceptField2.addCoding(new Coding("http://mycodes.org/fhir/observation-code", "test-code-2", "test-code-2 display"));
        codeableConceptField2.addCoding(new Coding("http://myalternatecodes.org/fhir/observation-code", "test-alt-code-2", "test-alt-code-2 display"));
        codeableConceptField2.addCoding(new Coding("http://mysecondaltcodes.org/fhir/observation-code", "test-second-alt-code-2", "test-second-alt-code-2 display"));

        // Create two CodeableConcept entities for category, each with three codings.
        List<Coding> category1 = new ArrayList<>();
        // Create three codings and first category CodeableConcept
        category1.add(new Coding("http://mycodes.org/fhir/observation-category", "test-heart-rate", "test-heart-rate display"));
        category1.add(new Coding("http://myalternatecodes.org/fhir/observation-category", "test-alt-heart-rate", "test-alt-heart-rate display"));
        category1.add(new Coding("http://mysecondaltcodes.org/fhir/observation-category", "test-2nd-alt-heart-rate", "test-2nd-alt-heart-rate display"));
        List<CodeableConcept> categoryConcepts1 = new ArrayList<>();
        CodeableConcept categoryCodeableConcept1 = new CodeableConcept().setText("Test Codeable Concept Field for first category");
        categoryCodeableConcept1.setCoding(category1);
        categoryConcepts1.add(categoryCodeableConcept1);
        // Create three codings and second category CodeableConcept
        List<Coding> category2 = new ArrayList<>();
        category2.add(new Coding("http://mycodes.org/fhir/observation-category", "test-vital-signs", "test-vital-signs display"));
        category2.add(new Coding("http://myalternatecodes.org/fhir/observation-category", "test-alt-vitals", "test-alt-vitals display"));
        category2.add(new Coding("http://mysecondaltcodes.org/fhir/observation-category", "test-2nd-alt-vitals", "test-2nd-alt-vitals display"));
        List<CodeableConcept> categoryConcepts2 = new ArrayList<>();
        CodeableConcept categoryCodeableConcept2 = new CodeableConcept().setText("Test Codeable Concept Field for second category");
        categoryCodeableConcept2.setCoding(category2);
        categoryConcepts2.add(categoryCodeableConcept2);

        for (int patientCount = 0; patientCount < 10 ; patientCount++) {

            String subjectId = String.valueOf(patientCount);

            for ( int entryCount = 0; entryCount < 10 ; entryCount++ ) {

                Observation observation = new Observation();
                observation.setId(String.valueOf(entryCount + patientCount*10));
                Reference subject = new Reference(subjectId);
                observation.setSubject(subject);

                if (entryCount%2 == 1) {
                    observation.setCategory(categoryConcepts1);
                    observation.setCode(codeableConceptField1);
                } else {
                    observation.setCategory(categoryConcepts2);
                    observation.setCode(codeableConceptField2);
                }

                Calendar observationDate = new GregorianCalendar();
                observationDate.add(Calendar.HOUR, -10 + entryCount);
                Date effectiveDtm = observationDate.getTime();
                observation.setEffective(new DateTimeType(effectiveDtm));

                myObservationLastNIndexPersistR4Svc.indexObservation(observation, String.valueOf(patientCount));

            }

        }

        assertEquals(100, myResourceIndexedObservationLastNDao.count());
        assertEquals(2, myCodeableConceptIndexedSearchParamNormalizedDao.count());

    }

    @Test
	public void testSampleBundleInTransaction() throws IOException {
		 FhirContext myFhirCtx = FhirContext.forR4();

		 PathMatchingResourcePatternResolver provider = new PathMatchingResourcePatternResolver();
		 final Resource[] bundleResources;
		 try {
			 bundleResources = provider.getResources("lastntestbundle.json");
		 } catch (IOException e) {
			 throw new RuntimeException("Unexpected error during transmission: " + e.toString(), e);
		 }

		 AtomicInteger index = new AtomicInteger();

		 Arrays.stream(bundleResources).forEach(
			 resource -> {
				 index.incrementAndGet();

				 InputStream resIs = null;
				 String nextBundleString;
				 try {
					 resIs = resource.getInputStream();
					 nextBundleString = IOUtils.toString(resIs, Charsets.UTF_8);
				 } catch (IOException e) {
					 return;
				 } finally {
					 try {
						 if (resIs != null) {
							 resIs.close();
						 }
					 } catch (final IOException ioe) {
						 // ignore
					 }
				 }

				 /*
				  * SMART demo apps rely on the use of LOINC 3141-9 (Body Weight Measured)
				  * instead of LOINC 29463-7 (Body Weight)
				  */
				 nextBundleString = nextBundleString.replace("\"29463-7\"", "\"3141-9\"");

				 IParser parser = myFhirCtx.newJsonParser();
				 parser.setParserErrorHandler(new LenientErrorHandler(false));
				 Bundle bundle = parser.parseResource(Bundle.class, nextBundleString);

				 myDao.transaction(null, bundle);


			 }
		 );

		 SearchParameterMap searchParameterMap = new SearchParameterMap();

		 // execute Observation ID search - Composite Aggregation
		 List<String>  observationIdsOnly = elasticsearchSvc.executeLastN(searchParameterMap,1);

		 assertEquals(20, observationIdsOnly.size());

		 observationIdsOnly = elasticsearchSvc.executeLastN(searchParameterMap, 3);

		 assertEquals(38, observationIdsOnly.size());

	 }

}