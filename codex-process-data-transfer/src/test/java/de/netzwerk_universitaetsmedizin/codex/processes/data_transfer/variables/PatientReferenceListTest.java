package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.UUID;

import org.highmed.dsf.fhir.json.ObjectMapperFactory;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

public class PatientReferenceListTest
{
	private static final Logger logger = LoggerFactory.getLogger(PatientReferenceListTest.class);

	@Test
	public void testSerialization() throws Exception
	{
		ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		PatientReference r1 = PatientReference.from("http://test/fhir/Patient/" + UUID.randomUUID().toString());
		PatientReference r2 = PatientReference
				.from(new Identifier().setSystem("http://test/fhir/sid/identifier-system").setValue("some-value"));
		PatientReferenceList list = new PatientReferenceList(Arrays.asList(r1, r2));

		String stringValue = objectMapper.writeValueAsString(list);
		assertNotNull(stringValue);

		logger.debug("PatientReferenceList: '{}'", stringValue);

		PatientReferenceList read = objectMapper.readValue(stringValue, PatientReferenceList.class);

		assertNotNull(read);
		assertNotNull(read.getReferences());
		assertEquals(2, read.getReferences().size());

		assertNotNull(read.getReferences().get(0));
		assertTrue(read.getReferences().get(0).hasAbsoluteReference());
		assertFalse(read.getReferences().get(0).hasIdentifier());
		assertNotNull(read.getReferences().get(0).getAbsoluteReference());
		assertEquals(r1.getAbsoluteReference(), read.getReferences().get(0).getAbsoluteReference());
		assertNotNull(read.getReferences().get(0).getIdentifier());
		assertNull(read.getReferences().get(0).getIdentifier().getSystem());
		assertNull(read.getReferences().get(0).getIdentifier().getValue());

		assertNotNull(read.getReferences().get(1));
		assertFalse(read.getReferences().get(1).hasAbsoluteReference());
		assertTrue(read.getReferences().get(1).hasIdentifier());
		assertNull(read.getReferences().get(1).getAbsoluteReference());
		assertNotNull(read.getReferences().get(1).getIdentifier());
		assertNotNull(read.getReferences().get(1).getIdentifier().getSystem());
		assertNotNull(read.getReferences().get(1).getIdentifier().getValue());
		assertEquals(r2.getIdentifier().getSystem(), read.getReferences().get(1).getIdentifier().getSystem());
		assertEquals(r2.getIdentifier().getValue(), read.getReferences().get(1).getIdentifier().getValue());
	}
}
