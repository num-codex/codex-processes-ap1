package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.Constants;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PseudonymList;

public class FhirClientImpl implements FhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientImpl.class);

	private static final List<String> RESOURCES_WITH_PATIENT_REF = Arrays.asList("AllergyIntolerance", "CarePlan",
			"CareTeam", "ClinicalImpression", "Composition", "Condition", "Consent", "DetectedIssue", "DeviceRequest",
			"DeviceUseStatement", "DiagnosticReport", "DocumentManifest", "DocumentReference", "Encounter",
			"EpisodeOfCare", "FamilyMemberHistory", "Flag", "Goal", "ImagingStudy", "Immunization", "List",
			"MedicationAdministration", "MedicationDispense", "MedicationRequest ", "MedicationStatement",
			"NutritionOrder", "Observation", "Procedure", "RiskAssessment", "ServiceRequest", "SupplyDelivery",
			"VisionPrescription");

	private static final Pattern QUERY_PATTERN = Pattern.compile(
			"(?<resource>Account|ActivityDefinition|AdverseEvent|AllergyIntolerance|Appointment|AppointmentResponse"
					+ "|AuditEvent|Basic|Binary|BiologicallyDerivedProduct|BodyStructure|Bundle|CapabilityStatement"
					+ "|CarePlan|CareTeam|CatalogEntry|ChargeItem|ChargeItemDefinition|Claim|ClaimResponse"
					+ "|ClinicalImpression|CodeSystem|Communication|CommunicationRequest|CompartmentDefinition"
					+ "|Composition|ConceptMap|Condition|Consent|Contract|Coverage|CoverageEligibilityRequest"
					+ "|CoverageEligibilityResponse|DetectedIssue|Device|DeviceDefinition|DeviceMetric|DeviceRequest"
					+ "|DeviceUseStatement|DiagnosticReport|DocumentManifest|DocumentReference|EffectEvidenceSynthesis"
					+ "|Encounter|Endpoint|EnrollmentRequest|EnrollmentResponse|EpisodeOfCare|EventDefinition|Evidence"
					+ "|EvidenceVariable|ExampleScenario|ExplanationOfBenefit|FamilyMemberHistory|Flag|Goal"
					+ "|GraphDefinition|Group|GuidanceResponse|HealthcareService|ImagingStudy|Immunization"
					+ "|ImmunizationEvaluation|ImmunizationRecommendation|ImplementationGuide|InsurancePlan|Invoice"
					+ "|Library|Linkage|List|Location|Measure|MeasureReport|Media|Medication|MedicationAdministration"
					+ "|MedicationDispense|MedicationKnowledge|MedicationRequest|MedicationStatement|MedicinalProduct"
					+ "|MedicinalProductAuthorization|MedicinalProductContraindication|MedicinalProductIndication"
					+ "|MedicinalProductIngredient|MedicinalProductInteraction|MedicinalProductManufactured"
					+ "|MedicinalProductPackaged|MedicinalProductPharmaceutical|MedicinalProductUndesirableEffect"
					+ "|MessageDefinition|MessageHeader|MolecularSequence|NamingSystem|NutritionOrder|Observation"
					+ "|ObservationDefinition|OperationDefinition|OperationOutcome|Organization|OrganizationAffiliation"
					+ "|Patient|PaymentNotice|PaymentReconciliation|Person|PlanDefinition|Practitioner|PractitionerRole"
					+ "|Procedure|Provenance|Questionnaire|QuestionnaireResponse|RelatedPerson|RequestGroup"
					+ "|ResearchDefinition|ResearchElementDefinition|ResearchStudy|ResearchSubject|RiskAssessment"
					+ "|RiskEvidenceSynthesis|Schedule|SearchParameter|ServiceRequest|Slot|Specimen|SpecimenDefinition"
					+ "|StructureDefinition|StructureMap|Subscription|Substance|SubstanceDefinition|SubstanceNucleicAcid"
					+ "|SubstancePolymer|SubstanceProtein|SubstanceReferenceInformation|SubstanceSourceMaterial"
					+ "|SupplyDelivery|SupplyRequest|Task|TerminologyCapabilities|TestReport|TestScript|ValueSet"
					+ "|VerificationResult|VisionPrescription)" + "(?<query>\\?.*)");

	private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
	private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy-MM");
	private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	private final HapiFhirClientFactory clientFactory;
	private final FhirContext fhirContext;
	private final Path searchBundleOverride;

	/**
	 * @param clientFactory
	 *            not <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param searchBundleOverride
	 *            may be <code>null</code>
	 */
	public FhirClientImpl(HapiFhirClientFactory clientFactory, FhirContext fhirContext, Path searchBundleOverride)
	{
		this.clientFactory = clientFactory;
		this.fhirContext = fhirContext;
		this.searchBundleOverride = searchBundleOverride;
	}

	/**
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	private Bundle getSearchBundle(DateWithPrecision exportFrom, Date exportTo)
	{
		return doGetSearchBundle(null, null, exportFrom, exportTo, true);
	}

	/**
	 * @param pseudonym
	 *            may be <code>null</code>
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	private Bundle getSearchBundleWithPseudonym(String pseudonym, DateWithPrecision exportFrom, Date exportTo)
	{
		Objects.requireNonNull(pseudonym, "pseudonym");

		return doGetSearchBundle(null, pseudonym, exportFrom, exportTo, false);
	}

	/**
	 * @param patientId
	 *            not <code>null</code>
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	private Bundle getSearchBundleWithPatientId(String patientId, DateWithPrecision exportFrom, Date exportTo)
	{
		Objects.requireNonNull(patientId, "patientId");

		return doGetSearchBundle(patientId, null, exportFrom, exportTo, false);
	}

	private Bundle doGetSearchBundle(String patientId, String pseudonym, DateWithPrecision exportFrom, Date exportTo,
			boolean includePatient)
	{
		Objects.requireNonNull(exportTo, "exportTo");

		Bundle bundle = readSerachBundleTemplate();
		if (!EnumSet.of(BundleType.BATCH, BundleType.TRANSACTION).contains(bundle.getType()))
		{
			logger.warn("Search-Bundle type not batch or transaction but {}",
					bundle.hasType() ? bundle.getType().toCode() : null);
			throw new RuntimeException("Search-Bundle type not batch or transaction");
		}

		List<BundleEntryComponent> entries = bundle.getEntry().stream()
				.map(modifySearchUrl(patientId, pseudonym, exportFrom, exportTo, includePatient)).filter(e -> e != null)
				.collect(Collectors.toList());

		bundle.setEntry(entries);

		return bundle;
	}

	private Function<BundleEntryComponent, BundleEntryComponent> modifySearchUrl(String patientId, String pseudonym,
			DateWithPrecision exportFrom, Date exportTo, boolean includePatient)
	{
		return entry ->
		{
			if (entry == null || entry.hasResponse() || entry.hasResponse() || entry.hasSearch() || !entry.hasRequest()
					|| !entry.getRequest().hasMethod() || !HTTPVerb.GET.equals(entry.getRequest().getMethod())
					|| !entry.getRequest().hasUrl())
			{
				logger.warn("Search-Bundle contains invalid entry");
				throw new RuntimeException("Search-Bundle contains invalid entry");
			}
			Matcher queryPatternMatcher = QUERY_PATTERN.matcher(entry.getRequest().getUrl());
			if (!queryPatternMatcher.matches())
			{
				logger.warn("Search-Bundle contains entry with invalid serach query");
				throw new RuntimeException("Search-Bundle contains entry with invalid serach query");
			}

			String resource = queryPatternMatcher.group("resource");
			String query = queryPatternMatcher.group("query");

			if (RESOURCES_WITH_PATIENT_REF.contains(resource))
			{
				if (patientId != null)
					query += createPatIdSearchUrlPart(patientId);
				else
					query += createPatPrefixPseudonymSearchUrlPart(pseudonym);

				if (includePatient)
					query += createIncludeSearchUrlPart(resource);
			}
			else if ("Patient".equals(resource))
			{
				// filtering search for patient if patient id known
				if (patientId != null)
					return null;

				query += createPseudonymSearchUrlPart(pseudonym);
			}
			else
			{
				logger.warn(
						"Search-Bundle contains entry with invalid serach query {}, target resource {} not supported",
						resource + query, resource);
				throw new RuntimeException("Search-Bundle contains entry with invalid serach query, target resource "
						+ resource + " not supported");
			}

			query += createExportFromSearchUrlPart(exportFrom);
			query += createExportToSearchUrlPart(exportTo);

			entry.getRequest().setUrl(resource + query);

			return entry;
		};
	}

	private String createPseudonymSearchUrlPart(String pseudonym)
	{
		if (pseudonym != null && !pseudonym.isBlank())
			return "&identifier=" + ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
		else
			return "";
	}

	private String createPatIdSearchUrlPart(String patientId)
	{
		if (patientId != null && !patientId.isBlank())
			return "&patient=" + patientId;
		else
			return "";
	}

	private String createPatPrefixPseudonymSearchUrlPart(String pseudonym)
	{
		if (pseudonym != null && !pseudonym.isBlank())
			return "&patient:identifier=" + ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|"
					+ pseudonym;
		else
			return "";
	}

	private String createExportFromSearchUrlPart(DateWithPrecision exportFrom)
	{
		if (exportFrom != null)
		{
			String dateTime = null;
			switch (exportFrom.getPrecision())
			{
				case YEAR:
					dateTime = YEAR_FORMAT.format(exportFrom);
					break;
				case MONTH:
					dateTime = MONTH_FORMAT.format(exportFrom);
					break;
				case DAY:
					dateTime = DAY_FORMAT.format(exportFrom);
					break;
				case SECOND:
				case MINUTE:
				case MILLI:
					dateTime = TIME_FORMAT.format(exportFrom);
					break;

				default:
					throw new RuntimeException(
							"TemporalPrecisionEnum value " + exportFrom.getPrecision() + " not supported");
			}

			return "&_lastUpdated=ge" + dateTime;
		}
		else
			return "";
	}

	private String createExportToSearchUrlPart(Date exportTo)
	{
		if (exportTo != null)
			return "&_lastUpdated=lt" + TIME_FORMAT.format(exportTo);
		else
			return "";
	}

	private String createIncludeSearchUrlPart(String resource)
	{
		if (resource != null)
			return "&_include=" + resource + ":patient";
		else
			return "";
	}

	private Bundle readSerachBundleTemplate()
	{
		try
		{
			if (searchBundleOverride != null)
			{
				if (!Files.isReadable(searchBundleOverride))
				{
					logger.warn("Search-Bundle override at {} not readable", searchBundleOverride.toString());
					throw new RuntimeException(
							"Search-Bundle override at " + searchBundleOverride.toString() + " not readable");
				}
				else
				{
					try (InputStream in = Files.newInputStream(searchBundleOverride))
					{
						logger.warn("Using Search-Bundle from {}", searchBundleOverride.toString());
						return fhirContext.newXmlParser().parseResource(Bundle.class, in);
					}
				}
			}
			else
			{
				try (InputStream in = FhirClientImpl.class.getResourceAsStream("/fhir/Bundle/SearchBundle.xml"))
				{
					logger.info("Using internal Search-Bundle");
					return fhirContext.newXmlParser().parseResource(Bundle.class, in);
				}

			}
		}
		catch (DataFormatException | IOException e)
		{
			logger.warn("Error while reading Search-Bundle: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public PseudonymList getPseudonymsWithNewData(DateWithPrecision exportFrom, Date exportTo)
	{
		Bundle searchBundle = getSearchBundle(exportFrom, exportTo);

		if (logger.isDebugEnabled())
			logger.debug("Executing Search-Bundle: {}",
					fhirContext.newJsonParser().encodeResourceToString(searchBundle));

		Bundle resultBundle = clientFactory.getFhirStoreClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}", fhirContext.newJsonParser().encodeResourceToString(resultBundle));

		Stream<Patient> patients = resultBundle.getEntry().stream()
				.filter(e -> e.hasResource() && e.getResource() instanceof Bundle).map(e -> (Bundle) e.getResource())
				.flatMap(this::getPatients);

		return new PseudonymList(patients
				.map(p -> p.getIdentifier().stream()
						.filter(i -> i.hasSystem() && i.hasValue()
								&& ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM.equals(i.getSystem()))
						.map(i -> i.getValue()).findFirst().orElse(null))
				.filter(p -> p != null).distinct().collect(Collectors.toList()));
	}

	private Stream<Patient> getPatients(Bundle bundle)
	{
		Stream<Patient> patients = getPatientsFromBundle(bundle);

		if (bundle.getTotal() > bundle.getEntry().size())
			return Stream.concat(patients,
					doGetPatients(bundle.getLink(Bundle.LINK_NEXT).getUrl(), bundle.getEntry().size()));
		else
			return patients;
	}

	private Stream<Patient> getPatientsFromBundle(Bundle bundle)
	{
		return bundle.getEntry().stream().filter(e -> e.hasResource() && e.getResource() instanceof Patient)
				.map(e -> (Patient) e.getResource());
	}

	private Stream<Patient> doGetPatients(String nextUrl, int subTotal)
	{
		Bundle bundle = continueSearch(nextUrl);
		Stream<Patient> patients = getPatientsFromBundle(bundle);

		if (bundle.getTotal() > bundle.getEntry().size() + subTotal)
			return Stream.concat(patients,
					doGetPatients(bundle.getLink(Bundle.LINK_NEXT).getUrl(), bundle.getEntry().size() + subTotal));
		else
			return patients;
	}

	private Bundle continueSearch(String url)
	{
		if (logger.isDebugEnabled())
			logger.debug("Executing search: {}", url);

		Bundle resultBundle = (Bundle) clientFactory.getFhirStoreClient().search().byUrl(url)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}", fhirContext.newJsonParser().encodeResourceToString(resultBundle));

		return resultBundle;
	}

	@Override
	public Stream<DomainResource> getNewData(String pseudonym, DateWithPrecision exportFrom, Date exportTo)
	{
		if (clientFactory.supportsIdentifierReferenceSearch())
			return getNewDataWithIdentifierReferenceSupport(pseudonym, exportFrom, exportTo);
		else
			return getNewDataWithoutIdentifierReferenceSupport(pseudonym, exportFrom, exportTo);
	}

	private Stream<DomainResource> getNewDataWithIdentifierReferenceSupport(String pseudonym,
			DateWithPrecision exportFrom, Date exportTo)
	{
		Bundle searchBundle = getSearchBundleWithPseudonym(pseudonym, exportFrom, exportTo);

		if (logger.isDebugEnabled())
			logger.debug("Executing Search-Bundle: {}",
					fhirContext.newJsonParser().encodeResourceToString(searchBundle));

		Bundle resultBundle = clientFactory.getFhirStoreClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}", fhirContext.newJsonParser().encodeResourceToString(resultBundle));

		return resultBundle.getEntry().stream().filter(e -> e.hasResource() && e.getResource() instanceof Bundle)
				.map(e -> (Bundle) e.getResource()).flatMap(this::getDomainResources);
	}

	private Stream<DomainResource> getNewDataWithoutIdentifierReferenceSupport(String pseudonym,
			DateWithPrecision exportFrom, Date exportTo)
	{
		Bundle patientBundle = (Bundle) clientFactory.getFhirStoreClient().search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly()
						.systemAndIdentifier(ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM, pseudonym))
				.execute();

		if (logger.isDebugEnabled())
			logger.debug("Patient search-bundle result: {}",
					fhirContext.newJsonParser().encodeResourceToString(patientBundle));

		if (patientBundle.getTotal() != 1 || !(patientBundle.getEntryFirstRep().getResource() instanceof Patient))
		{
			logger.warn(
					"Error while retrieving patient for pseudonym {}, result bundle total not 1 or first entry not patient",
					pseudonym);
			throw new RuntimeException("Error while retrieving patient for pseudonym " + pseudonym);
		}

		Patient patient = (Patient) patientBundle.getEntryFirstRep().getResource();

		Bundle searchBundle = getSearchBundleWithPatientId(patient.getIdElement().getIdPart(), exportFrom, exportTo);

		if (logger.isDebugEnabled())
			logger.debug("Executing Search-Bundle: {}",
					fhirContext.newJsonParser().encodeResourceToString(searchBundle));

		Bundle resultBundle = clientFactory.getFhirStoreClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}", fhirContext.newJsonParser().encodeResourceToString(resultBundle));

		return Stream.concat(Stream.of(patient),
				resultBundle.getEntry().stream().filter(e -> e.hasResource() && e.getResource() instanceof Bundle)
						.map(e -> (Bundle) e.getResource()).flatMap(this::getDomainResources));
	}

	private Stream<DomainResource> getDomainResources(Bundle bundle)
	{
		Stream<DomainResource> domainResources = getDomainResourcesFromBundle(bundle);

		if (bundle.getTotal() > bundle.getEntry().size())
			return Stream.concat(domainResources,
					doGetDomainResources(bundle.getLink(Bundle.LINK_NEXT).getUrl(), bundle.getEntry().size()));
		else
			return domainResources;
	}

	private Stream<DomainResource> getDomainResourcesFromBundle(Bundle bundle)
	{
		return bundle.getEntry().stream().filter(e -> e.hasResource() && e.getResource() instanceof DomainResource)
				.map(e -> (DomainResource) e.getResource());
	}

	private Stream<DomainResource> doGetDomainResources(String nextUrl, int subTotal)
	{
		Bundle bundle = continueSearch(nextUrl);
		Stream<DomainResource> domainResources = getDomainResourcesFromBundle(bundle);

		if (bundle.getTotal() > bundle.getEntry().size() + subTotal)
			return Stream.concat(domainResources, doGetDomainResources(bundle.getLink(Bundle.LINK_NEXT).getUrl(),
					bundle.getEntry().size() + subTotal));
		else
			return domainResources;
	}

	@Override
	public void storeBundle(Bundle bundle)
	{
		clientFactory.getFhirStoreClient().transaction().withBundle(bundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();
	}
}
