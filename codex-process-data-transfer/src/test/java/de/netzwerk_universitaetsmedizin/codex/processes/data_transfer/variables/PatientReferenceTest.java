package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.highmed.dsf.fhir.json.ObjectMapperFactory;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

public class PatientReferenceTest
{
	private static final Logger logger = LoggerFactory.getLogger(PatientReferenceTest.class);

	@Test
	public void testSerializationAbsoluteReference() throws Exception
	{
		ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		PatientReference reference = PatientReference.from("http://test/fhir/Patient/" + UUID.randomUUID().toString());

		String stringValue = objectMapper.writeValueAsString(reference);
		assertNotNull(stringValue);

		logger.debug("PatientReference: '{}'", stringValue);

		PatientReference read = objectMapper.readValue(stringValue, PatientReference.class);

		assertNotNull(read);

		assertTrue(read.hasAbsoluteReference());
		assertNotNull(read.getAbsoluteReference());
		assertEquals(reference.getAbsoluteReference(), read.getAbsoluteReference());

		assertFalse(read.hasIdentifier());
		assertNotNull(read.getIdentifier()); // <- always returns a not null value
		assertFalse(read.getIdentifier().hasSystem());
		assertFalse(read.getIdentifier().hasValue());
	}

	@Test
	public void testSerializationIdentifier() throws Exception
	{
		ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		Identifier identifier = new Identifier().setSystem("http://test/fhir/sid/identifier-system")
				.setValue("some-value");
		PatientReference reference = PatientReference.from(identifier);

		String stringValue = objectMapper.writeValueAsString(reference);
		assertNotNull(stringValue);

		logger.debug("PatientReference: '{}'", stringValue);

		PatientReference read = objectMapper.readValue(stringValue, PatientReference.class);

		assertNotNull(read);

		assertFalse(read.hasAbsoluteReference());
		assertNull(read.getAbsoluteReference());

		assertTrue(read.hasIdentifier());
		assertNotNull(read.getIdentifier());
		assertTrue(read.getIdentifier().hasSystem());
		assertTrue(read.getIdentifier().hasValue());
		assertEquals(identifier.getSystem(), read.getIdentifier().getSystem());
		assertEquals(identifier.getValue(), read.getIdentifier().getValue());
	}
}
