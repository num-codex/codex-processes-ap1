package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;

public class AbstractFhirClientTest
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractFhirClientTest.class);

	@Test
	public void testSetSearchBundleWithExportTo() throws Exception
	{
		FhirContext fhirContext = FhirContext.forR4();
		GeccoClient geccoClient = Mockito.mock(GeccoClient.class);
		DataLogger dataLogger = Mockito.mock(DataLogger.class);
		when(geccoClient.getSearchBundleOverride())
				.thenReturn(Paths.get("src/main/resources/fhir/Bundle/SearchBundle.xml"));
		when(geccoClient.getFhirContext()).thenReturn(fhirContext);
		AbstractFhirClient client = Mockito.mock(AbstractFhirClient.class,
				Mockito.withSettings().useConstructor(geccoClient, dataLogger).defaultAnswer(CALLS_REAL_METHODS));

		Date exportTo = new Date();

		Bundle bundle = client.getSearchBundle(null, exportTo);
		assertNotNull(bundle);
		assertTrue(bundle.hasEntry());
		assertNotNull(bundle.getEntry());
		assertEquals(64, bundle.getEntry().size());

		logger.debug("Search Bundle after replacement: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));

		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		String exportToString = timeFormat.format(exportTo).replaceAll("\\+", "%2B").replaceAll(":", "%3A");

		String expectedUrl = "Condition?_profile=https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/cardiovascular-diseases&_include=Condition%3Apatient"
				+ "&_lastUpdated=lt" + exportToString;

		long entriesWithExpectedUrl = bundle.getEntry().stream().filter(e -> e.hasRequest())
				.filter(e -> e.getRequest().hasUrl()).filter(e -> e.getRequest().getUrl().equals(expectedUrl)).count();
		assertEquals(1, entriesWithExpectedUrl);
	}

	@Test
	public void testSetSearchBundleWithExportFromAndExportTo() throws Exception
	{
		FhirContext fhirContext = FhirContext.forR4();
		GeccoClient geccoClient = Mockito.mock(GeccoClient.class);
		DataLogger dataLogger = Mockito.mock(DataLogger.class);
		when(geccoClient.getSearchBundleOverride())
				.thenReturn(Paths.get("src/main/resources/fhir/Bundle/SearchBundle.xml"));
		when(geccoClient.getFhirContext()).thenReturn(fhirContext);
		AbstractFhirClient client = Mockito.mock(AbstractFhirClient.class,
				Mockito.withSettings().useConstructor(geccoClient, dataLogger).defaultAnswer(CALLS_REAL_METHODS));

		DateWithPrecision exportFrom = new DateWithPrecision(new Date(), TemporalPrecisionEnum.MILLI);
		Date exportTo = new Date();

		Bundle bundle = client.getSearchBundle(exportFrom, exportTo);
		assertNotNull(bundle);
		assertTrue(bundle.hasEntry());
		assertNotNull(bundle.getEntry());
		assertEquals(64, bundle.getEntry().size());

		logger.debug("Search Bundle after replacement: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));

		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		String exportFromString = timeFormat.format(exportFrom).replaceAll("\\+", "%2B").replaceAll(":", "%3A");
		String exportToString = timeFormat.format(exportTo).replaceAll("\\+", "%2B").replaceAll(":", "%3A");

		String expectedUrl = "Condition?_profile=https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/cardiovascular-diseases&_include=Condition%3Apatient"
				+ "&_lastUpdated=lt" + exportToString + "&_lastUpdated=ge" + exportFromString;

		long entriesWithExpectedUrl = bundle.getEntry().stream().filter(e -> e.hasRequest())
				.filter(e -> e.getRequest().hasUrl()).filter(e -> e.getRequest().getUrl().equals(expectedUrl)).count();
		assertEquals(1, entriesWithExpectedUrl);
	}

	@Test
	public void testSetSearchBundleWithPatientIdAndExportFromAndExportTo() throws Exception
	{
		FhirContext fhirContext = FhirContext.forR4();
		GeccoClient geccoClient = Mockito.mock(GeccoClient.class);
		DataLogger dataLogger = Mockito.mock(DataLogger.class);
		when(geccoClient.getSearchBundleOverride())
				.thenReturn(Paths.get("src/main/resources/fhir/Bundle/SearchBundle.xml"));
		when(geccoClient.getFhirContext()).thenReturn(fhirContext);
		AbstractFhirClient client = Mockito.mock(AbstractFhirClient.class,
				Mockito.withSettings().useConstructor(geccoClient, dataLogger).defaultAnswer(CALLS_REAL_METHODS));

		String patientId = "some-patient-id";
		DateWithPrecision exportFrom = new DateWithPrecision(new Date(), TemporalPrecisionEnum.MILLI);
		Date exportTo = new Date();

		Bundle bundle = client.getSearchBundleWithPatientId(patientId, exportFrom, exportTo);
		assertNotNull(bundle);
		assertTrue(bundle.hasEntry());
		assertNotNull(bundle.getEntry());
		assertEquals(63, bundle.getEntry().size());

		logger.debug("Search Bundle after replacement: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));

		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		String exportFromString = timeFormat.format(exportFrom).replaceAll("\\+", "%2B").replaceAll(":", "%3A");
		String exportToString = timeFormat.format(exportTo).replaceAll("\\+", "%2B").replaceAll(":", "%3A");

		String expectedUrl = "Condition?_profile=https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/cardiovascular-diseases"
				+ "&patient=" + patientId + "&_lastUpdated=lt" + exportToString + "&_lastUpdated=ge" + exportFromString;

		long entriesWithExpectedUrl = bundle.getEntry().stream().filter(e -> e.hasRequest())
				.filter(e -> e.getRequest().hasUrl()).filter(e -> e.getRequest().getUrl().equals(expectedUrl)).count();
		assertEquals(1, entriesWithExpectedUrl);
	}

	@Test
	public void testSetSearchBundleWithPseudonymIdAndExportFromAndExportTo() throws Exception
	{
		FhirContext fhirContext = FhirContext.forR4();
		GeccoClient geccoClient = Mockito.mock(GeccoClient.class);
		DataLogger dataLogger = Mockito.mock(DataLogger.class);
		when(geccoClient.getSearchBundleOverride())
				.thenReturn(Paths.get("src/main/resources/fhir/Bundle/SearchBundle.xml"));
		when(geccoClient.getFhirContext()).thenReturn(fhirContext);
		AbstractFhirClient client = Mockito.mock(AbstractFhirClient.class,
				Mockito.withSettings().useConstructor(geccoClient, dataLogger).defaultAnswer(CALLS_REAL_METHODS));

		String pseudonym = "some-pseudonym";
		DateWithPrecision exportFrom = new DateWithPrecision(new Date(), TemporalPrecisionEnum.MILLI);
		Date exportTo = new Date();

		Bundle bundle = client.getSearchBundleWithPseudonym(pseudonym, exportFrom, exportTo);
		assertNotNull(bundle);
		assertTrue(bundle.hasEntry());
		assertNotNull(bundle.getEntry());
		assertEquals(64, bundle.getEntry().size());

		logger.debug("Search Bundle after replacement: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));

		String namingSystemString = URLEncoder.encode(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM, StandardCharsets.UTF_8);
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		String exportFromString = timeFormat.format(exportFrom).replaceAll("\\+", "%2B").replaceAll(":", "%3A");
		String exportToString = timeFormat.format(exportTo).replaceAll("\\+", "%2B").replaceAll(":", "%3A");

		String expectedUrl = "Condition?_profile=https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/cardiovascular-diseases"
				+ "&patient:identifier=" + namingSystemString + "%7C" + pseudonym + "&_lastUpdated=lt" + exportToString
				+ "&_lastUpdated=ge" + exportFromString;

		long entriesWithExpectedUrl = bundle.getEntry().stream().filter(e -> e.hasRequest())
				.filter(e -> e.getRequest().hasUrl()).filter(e -> e.getRequest().getUrl().equals(expectedUrl)).count();
		assertEquals(1, entriesWithExpectedUrl);
	}
}
