package ca.uhn.fhir.jpa.dao.r4;

import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.util.TestUtil;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hamcrest.Matchers;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

@SuppressWarnings({"Duplicates"})
public class FhirResourceDaoR4FilterTest extends BaseJpaR4Test {

	@After
	public void after() {
		myDaoConfig.setFilterParameterEnabled(new DaoConfig().isFilterParameterEnabled());
	}

	@Before
	public void before() {
		myDaoConfig.setFilterParameterEnabled(true);
	}

	@Test
	public void testMalformedFilter() {
		SearchParameterMap map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name eq smith))"));
		try {
			myPatientDao.search(map);
			fail();
		} catch (InvalidRequestException e) {
			assertEquals("Error parsing _filter syntax: Expression did not terminate at 13", e.getMessage());
		}
	}

	@Test
	public void testBrackets() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name eq smith"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("(name eq smith) or (name eq jones)"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1, id2));

	}

	@Test
	public void testStringComparatorEq() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name eq smi"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, Matchers.empty());

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name eq smith"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

	}

	@Test
	public void testReferenceComparatorEq() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		IIdType ptId = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		p = new Patient();
		p.addName().setFamily("Smith").addGiven("John2");
		p.setActive(true);
		IIdType ptId2 = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		p = new Patient();
		p.addName().setFamily("Smith").addGiven("John3");
		p.setActive(true);
		IIdType ptId3 = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		CarePlan cp = new CarePlan();
		cp.getSubject().setReference(ptId.getValue());
		String cpId = myCarePlanDao.create(cp).getId().toUnqualifiedVersionless().getValue();

		cp = new CarePlan();
		cp.addActivity().getDetail().addPerformer().setReference(ptId2.getValue());
		String cpId2 = myCarePlanDao.create(cp).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("subject eq " + ptId.getValue()));
		found = toUnqualifiedVersionlessIdValues(myCarePlanDao.search(map));
		assertThat(found, containsInAnyOrder(cpId));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("subject eq " + ptId.getIdPart()));
		found = toUnqualifiedVersionlessIdValues(myCarePlanDao.search(map));
		assertThat(found, containsInAnyOrder(cpId));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("(subject eq " + ptId.getIdPart() + ") or (performer eq " + ptId2.getValue() + ")"));
		found = toUnqualifiedVersionlessIdValues(myCarePlanDao.search(map));
		assertThat(found, containsInAnyOrder(cpId, cpId2));

	}

	@Test
	public void testFilterDisabled() {
		myDaoConfig.setFilterParameterEnabled(false);

		SearchParameterMap map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name eq smith"));
		try {
			myPatientDao.search(map);
		} catch (InvalidRequestException e) {
			assertEquals("_filter parameter is disabled on this server", e.getMessage());
		}
	}

	@Test
	public void testRetrieveDifferentTypeEq() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();
		String idVal = id1.split("/")[1];

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam(String.format("status eq active or _id eq %s",
			idVal)));
		found = toUnqualifiedVersionlessIdValues(myEncounterDao.search(map));
		assertThat(found, Matchers.empty());

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam(String.format("_id eq %s",
			idVal)));
		found = toUnqualifiedVersionlessIdValues(myEncounterDao.search(map));
		assertThat(found, Matchers.empty());

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam(String.format("_id eq %s",
			idVal)));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

	}

	@Test
	public void testStringComparatorNe() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family ne smith"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));
		assertThat(found, containsInAnyOrder(Matchers.not(id1)));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family ne jones"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));
		assertThat(found, containsInAnyOrder(Matchers.not(id2)));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given ne john"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));
		assertThat(found, containsInAnyOrder(Matchers.not(id1)));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given ne frank"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));
		assertThat(found, containsInAnyOrder(Matchers.not(id2)));

	}

	@Test
	public void testReferenceComparatorNe() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		IIdType ptId = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		p = new Patient();
		p.addName().setFamily("Smith").addGiven("John2");
		p.setActive(true);
		IIdType ptId2 = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		CarePlan cp = new CarePlan();
		cp.getSubject().setReference(ptId.getValue());
		String cpId = myCarePlanDao.create(cp).getId().toUnqualifiedVersionless().getValue();

		cp = new CarePlan();
		cp.addActivity().getDetail().addPerformer().setReference(ptId2.getValue());
		String cpId2 = myCarePlanDao.create(cp).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("subject ne " + ptId.getValue()));
		found = toUnqualifiedVersionlessIdValues(myCarePlanDao.search(map));
		assertThat(found, containsInAnyOrder(cpId2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("subject ne " + ptId.getIdPart()));
		found = toUnqualifiedVersionlessIdValues(myCarePlanDao.search(map));
		assertThat(found, containsInAnyOrder(cpId2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("(subject ne " + ptId.getIdPart() + ") and (performer ne " + ptId2.getValue() + ")"));
		found = toUnqualifiedVersionlessIdValues(myCarePlanDao.search(map));
		assertThat(found, Matchers.empty());

	}

	@Test
	public void testStringComparatorCo() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name co smi"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name co smith"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given co frank"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family co jones"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));

	}

	@Test
	public void testStringComparatorSw() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name sw smi"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name sw mi"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, Matchers.empty());

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given sw fr"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));

	}

	@Test
	public void testStringComparatorEw() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family ew ith"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("name ew it"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, Matchers.empty());

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given ew nk"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));

	}

	@Test
	public void testStringComparatorGt() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family gt jones"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family gt arthur"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1, id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given gt john"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, Matchers.empty());

	}

	@Test
	public void testStringComparatorLt() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family lt smith"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family lt walker"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1, id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given lt frank"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, Matchers.empty());

	}

	@Test
	public void testStringComparatorGe() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family ge jones"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1, id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family ge justin"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family ge arthur"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1, id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("given ge jon"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, Matchers.empty());

	}

	@Test
	public void testStringComparatorLe() {

		Patient p = new Patient();
		p.addName().setFamily("Smith").addGiven("John");
		p.setActive(true);
		String id1 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		p = new Patient();
		p.addName().setFamily("Jones").addGiven("Frank");
		p.setActive(false);
		String id2 = myPatientDao.create(p).getId().toUnqualifiedVersionless().getValue();

		SearchParameterMap map;
		List<String> found;

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family le smith"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id1, id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family le jones"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, containsInAnyOrder(id2));

		map = new SearchParameterMap();
		map.setLoadSynchronous(true);
		map.add(Constants.PARAM_FILTER, new StringParam("family le jackson"));
		found = toUnqualifiedVersionlessIdValues(myPatientDao.search(map));
		assertThat(found, Matchers.empty());

	}

	@AfterClass
	public static void afterClassClearContext() {
		TestUtil.clearAllStaticFieldsForUnitTest();
	}

}
