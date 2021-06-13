package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirClientBuilder;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceList;

public class FhirClientFactory
{
	private static final String condition = "{\"resourceType\":\"Condition\",\"meta\":{\"profile\":[\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/chronic-lung-diseases\"]},\"clinicalStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/condition-clinical\",\"code\":\"active\",\"display\":\"Active\"}]},\"verificationStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/condition-ver-status\",\"code\":\"confirmed\",\"display\":\"Confirmed\"},{\"system\":\"http://snomed.info/sct\",\"code\":\"410605003\",\"display\":\"Confirmed present (qualifier value)\"}]},\"category\":[{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"418112009\",\"display\":\"Pulmonary medicine\"}]}],\"code\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"413839001\",\"display\":\"Chronic lung disease\"}]},\"recordedDate\":\"2020-11-10T15:50:41.000+01:00\"}";
	private static final String patient = "{\"resourceType\":\"Patient\",\"meta\":{\"profile\":[\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient\"]},\"extension\":[{\"url\":\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/ethnic-group\",\"valueCoding\":{\"system\":\"http://snomed.info/sct\",\"code\":\"186019001\",\"display\":\"Other ethnic, mixed origin\"}},{\"url\":\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/age\",\"extension\":[{\"url\":\"dateTimeOfDocumentation\",\"valueDateTime\":\"2020-10-01\"},{\"url\":\"age\",\"valueAge\":{\"value\":67,\"unit\":\"years\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"a\"}}]}],\"birthDate\":\"1953-09-30\"}";
	private static final String observation = "{\"resourceType\":\"Observation\",\"meta\":{\"profile\":[\"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"OBI\"}]}}],\"status\":\"final\",\"category\":[{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"26436-6\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/observation-category\",\"code\":\"laboratory\"}]}],\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"94500-6\",\"display\":\"SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by NAA with probe detection\"}],\"text\":\"SARS-CoV-2-RNA (PCR)\"},\"effectiveDateTime\":\"2020-11-10T15:50:41.000+01:00\",\"valueCodeableConcept\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"260373001\",\"display\":\"Detected (qualifier value)\"}],\"text\":\"SARS-CoV-2-RNA positiv\"}}";

	private static final Logger logger = LoggerFactory.getLogger(FhirClientFactory.class);

	private final HapiFhirClientFactory hapiClientFactory;
	private final FhirContext fhirContext;
	private final Path searchBundleOverride;
	private final String localIdentifierValue;

	private final FhirClientBuilder builder;

	public FhirClientFactory(HapiFhirClientFactory hapiClientFactory, FhirContext fhirContext,
			Path searchBundleOverride, String localIdentifierValue, FhirClientBuilder builder)
	{
		this.hapiClientFactory = hapiClientFactory;
		this.fhirContext = fhirContext;
		this.searchBundleOverride = searchBundleOverride;
		this.localIdentifierValue = localIdentifierValue;

		this.builder = builder;
	}

	public FhirClient getFhirClient()
	{
		if (hapiClientFactory.isConfigured())
			return builder.build(fhirContext, hapiClientFactory, searchBundleOverride);
		else
			return createFhirClientStub();
	}

	private FhirClient createFhirClientStub()
	{
		return new FhirClient()
		{
			@Override
			public void storeBundle(Bundle bundle)
			{
				logger.warn("Ignoring bundle with {} {}", bundle.getEntry().size(),
						bundle.getEntry().size() != 1 ? "entries" : "entry");

				if (logger.isDebugEnabled())
					logger.debug("Ignored bundle: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));
			}

			@Override
			public PatientReferenceList getPatientReferencesWithNewData(DateWithPrecision exportFrom, Date exportTo)
			{
				logger.warn("Returning demo pseudonyms for {}", localIdentifierValue);
				return new PatientReferenceList(List.of(
						PatientReference.from(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM)
								.setValue("dic_foo/bar")),
						PatientReference.from(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM)
								.setValue("dic_foo/baz")),
						PatientReference.from("http://dic-foo/fhir/Patient/3"),
						PatientReference.from("http://dic-foo/fhir/Patient/4")));
			}

			@Override
			public Stream<DomainResource> getNewData(String pseudonym, DateWithPrecision exportFrom, Date exportTo)
			{
				logger.warn("Returning demo resources for {}", pseudonym);

				Patient p = fhirContext.newJsonParser().parseResource(Patient.class, patient);
				p.addIdentifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym);
				p.setIdElement(new IdType("Patient", UUID.randomUUID().toString()));

				Condition c = fhirContext.newJsonParser().parseResource(Condition.class, condition);
				c.setSubject(new Reference(p.getIdElement()));

				Observation o = fhirContext.newJsonParser().parseResource(Observation.class, observation);
				o.setSubject(new Reference(p.getIdElement()));

				return Stream.of(p, c, o);
			}

			@Override
			public Optional<Patient> getPatient(String reference)
			{
				Patient patient = fhirContext.newJsonParser().parseResource(Patient.class, FhirClientFactory.patient);
				patient.addIdentifier().setSystem(ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_BLOOM_FILTER).setValue(
						"J75gYl+RiKSsxeu33tixBEEtFGCZwIEsWIKgvESaluvpSGBte/SBNZilz+sLSZdHSDKTL2J2d1yZsakqjtV5U2SMMJZ5IF3gEk1MT3sCRkxXEo1aJWKpnqndUTR+fvtSeMFj0y/O5yqrLV9zU79CNiTfZN5t1/6XGxZUXq2DovfCRrrpRxWjFwjKIDo0OkRANf7Mqp+Fsu0Un53JF57p/p1RLpWcJkC3xO+UslGbDo3mjgczdvxz0aLmWNA7/NIhk+Q50gxCX3B4QrntPfLLlBkrmIpsKRcLFVuYZik7pYZ9prd0qCLQ9tc8qiw1ry5kMfIvLnIS/FV36w==");
				patient.setIdElement(new IdType("Patient", UUID.randomUUID().toString()));
				return Optional.of(patient);
			}

			@Override
			public void updatePatient(Patient patient)
			{
				// Nothing to do in stub client
			}
		};
	}
}
