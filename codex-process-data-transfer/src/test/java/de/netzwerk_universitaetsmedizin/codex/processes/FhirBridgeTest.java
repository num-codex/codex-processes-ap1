package de.netzwerk_universitaetsmedizin.codex.processes;

import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.HapiFhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirBridgeClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirClientBuilder;

public class FhirBridgeTest
{
	private static final String BUNDLE_STRING = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"fullUrl\":\"urn:uuid:d2a06354-4514-482d-bdc9-26def09912b8\",\"resource\":{\"resourceType\":\"Patient\",\"meta\":{\"profile\":[\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient\"]},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><table class=\\\"hapiPropertyTable\\\"><tbody><tr><td>Identifier</td><td>0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5</td></tr><tr><td>Date of birth</td><td><span>30 September 1953</span></td></tr></tbody></table></div>\"},\"extension\":[{\"url\":\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/ethnic-group\",\"valueCoding\":{\"system\":\"http://snomed.info/sct\",\"code\":\"186019001\",\"display\":\"Other ethnic, mixed origin\"}},{\"url\":\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/age\",\"extension\":[{\"url\":\"dateTimeOfDocumentation\",\"valueDateTime\":\"2020-10-01\"},{\"url\":\"age\",\"valueAge\":{\"value\":67,\"unit\":\"years\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"a\"}}]}],\"identifier\":[{\"system\":\"http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym\",\"value\":\"0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5\"}],\"birthDate\":\"1953-09-30\"},\"request\":{\"method\":\"PUT\",\"url\":\"Patient?identifier=http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym|0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5\"}},{\"fullUrl\":\"urn:uuid:fb4584cb-cc6e-4987-ad0e-fd62641b51c6\",\"resource\":{\"resourceType\":\"Condition\",\"meta\":{\"profile\":[\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/chronic-lung-diseases\"]},\"clinicalStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/condition-clinical\",\"code\":\"active\",\"display\":\"Active\"}]},\"verificationStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/condition-ver-status\",\"code\":\"confirmed\",\"display\":\"Confirmed\"},{\"system\":\"http://snomed.info/sct\",\"code\":\"410605003\",\"display\":\"Confirmed present (qualifier value)\"}]},\"category\":[{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"418112009\",\"display\":\"Pulmonary medicine\"}]}],\"code\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"413839001\",\"display\":\"Chronic lung disease\"}]},\"subject\":{\"identifier\":{\"system\":\"http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym\",\"value\":\"0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5\"}},\"recordedDate\":\"2020-11-10T15:50:41.000+01:00\"},\"request\":{\"method\":\"PUT\",\"url\":\"Condition?_profile=https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/chronic-lung-diseases&recorded-date=2020-11-10T15:50:41.000+01:00&patient:identifier=http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym|0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5\"}},{\"fullUrl\":\"urn:uuid:41a61a24-bd46-4090-92f3-edc556692d94\",\"resource\":{\"resourceType\":\"Observation\",\"meta\":{\"profile\":[\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr\"]},\"status\":\"final\",\"category\":[{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"26436-6\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/observation-category\",\"code\":\"laboratory\"}]}],\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"94500-6\",\"display\":\"SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by NAA with probe detection\"}],\"text\":\"SARS-CoV-2-RNA (PCR)\"},\"subject\":{\"identifier\":{\"system\":\"http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym\",\"value\":\"0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5\"}},\"effectiveDateTime\":\"2020-11-10T15:50:41.000+01:00\",\"valueCodeableConcept\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"260373001\",\"display\":\"Detected (qualifier value)\"}],\"text\":\"SARS-CoV-2-RNA positiv\"}},\"request\":{\"method\":\"PUT\",\"url\":\"Observation?_profile=https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr&date=2020-11-10T15:50:41.000+01:00&patient:identifier=http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym|0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5\"}}]}";

	public static void main(String[] args)
	{
		FhirContext fhirContext = FhirContext.forR4();
		HapiFhirClientFactory hapiFhirClientFactory = new HapiFhirClientFactory(fhirContext,
				"http://localhost:8888/fhir-bridge/fhir", null, null, null, 10_000, 10_000, 20_000, true);

		FhirClientBuilder clientBuilder = (fc, clientFactory, searchBundleOverride) -> new FhirBridgeClient(fc,
				hapiFhirClientFactory, searchBundleOverride);
		FhirClientFactory clientFactory = new FhirClientFactory(hapiFhirClientFactory, fhirContext, null, null,
				clientBuilder);

		Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, BUNDLE_STRING);
		clientFactory.getFhirClient().storeBundle(bundle);
	}
}
