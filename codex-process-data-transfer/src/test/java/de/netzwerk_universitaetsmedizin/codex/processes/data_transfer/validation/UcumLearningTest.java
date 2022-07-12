package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.fhir.ucum.UcumEssenceService;
import org.junit.Test;

public class UcumLearningTest
{
	@Test
	public void test() throws Exception
	{
		String theCode = "{beats}/min";
		try (InputStream input = Files.newInputStream(Paths.get("src/test/resources/fhir/ucum/ucum-essence.xml")))
		{
			UcumEssenceService svc = new UcumEssenceService(input);
			assertEquals("1 / (minute)", svc.analyse(theCode));
			assertEquals("s-1", svc.getCanonicalUnits(theCode));
			assertTrue(svc.getDefinedForms(theCode).isEmpty());
			assertNull(svc.validate(theCode));
			assertEquals("{beats}/min", svc.getCommonDisplay(theCode));
			assertTrue(svc.isComparable(theCode, "/min"));
		}
	}

	@Test
	public void test1() throws Exception
	{
		String theCode = "/min";
		try (InputStream input = Files.newInputStream(Paths.get("src/test/resources/fhir/ucum/ucum-essence.xml")))
		{
			UcumEssenceService svc = new UcumEssenceService(input);
			assertEquals(" / (minute)", svc.analyse(theCode));
			assertEquals("s-1", svc.getCanonicalUnits(theCode));
			assertTrue(svc.getDefinedForms(theCode).isEmpty());
			assertNull(svc.validate(theCode));
			assertEquals("/min", svc.getCommonDisplay(theCode));
			assertTrue(svc.isComparable(theCode, "/min"));
		}
	}
}
