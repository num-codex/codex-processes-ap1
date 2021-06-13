package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

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
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.HapiFhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.OutcomeLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceList;

public abstract class AbstractFhirClient implements FhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractFhirClient.class);
	private static final OutcomeLogger outcomeLogger = new OutcomeLogger(logger);

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

	protected final FhirContext fhirContext;
	protected final HapiFhirClientFactory clientFactory;
	private final Path searchBundleOverride;

	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param clientFactory
	 *            not <code>null</code>
	 * @param searchBundleOverride
	 *            may be <code>null</code>
	 */
	public AbstractFhirClient(FhirContext fhirContext, HapiFhirClientFactory clientFactory, Path searchBundleOverride)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
		this.searchBundleOverride = searchBundleOverride;
	}

	@Override
	public PatientReferenceList getPatientReferencesWithNewData(DateWithPrecision exportFrom, Date exportTo)
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

		List<PatientReference> patientReferences = patients
				.map(p -> getIdentifierPatientReference(p).orElse(getAbsoluteUrlPatientReference(p))).distinct()
				.collect(Collectors.toList());

		return new PatientReferenceList(patientReferences);
	}

	private Optional<PatientReference> getIdentifierPatientReference(Patient patient)
	{
		return Optional.ofNullable(patient)
				.flatMap(p -> p.getIdentifier().stream().filter(Identifier::hasValue).filter(Identifier::hasSystem)
						.filter(i -> NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM.equals(i.getSystem())).findFirst())
				.map(i -> PatientReference.from(i));
	}

	private PatientReference getAbsoluteUrlPatientReference(Patient patient)
	{
		IdType idElement = patient.getIdElement();
		return PatientReference.from(new IdType(clientFactory.getFhirStoreClient().getServerBase(),
				idElement.getResourceType(), idElement.getIdPart(), null).getValue());
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
		return bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof Patient).map(r -> (Patient) r);
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

	/**
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	protected Bundle getSearchBundle(DateWithPrecision exportFrom, Date exportTo)
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
	protected Bundle getSearchBundleWithPseudonym(String pseudonym, DateWithPrecision exportFrom, Date exportTo)
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
	protected Bundle getSearchBundleWithPatientId(String patientId, DateWithPrecision exportFrom, Date exportTo)
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
				try (InputStream in = getClass().getResourceAsStream("/fhir/Bundle/SearchBundle.xml"))
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
			return "&identifier=" + NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
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
			return "&patient:identifier=" + NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
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

	protected Stream<DomainResource> getDomainResources(Bundle bundle)
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
		return bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof DomainResource)
				.map(r -> (DomainResource) r);
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
	public Optional<Patient> getPatient(String reference)
	{
		Objects.requireNonNull(reference, "reference");

		logger.info("Requesting patient {}", reference);

		IdType idType = new IdType(reference);
		IGenericClient client = clientFactory.getFhirStoreClient();

		if (client.getServerBase().equals(idType.getBaseUrl()))
		{
			try
			{
				Patient patient = client.read().resource(Patient.class).withUrl(reference).execute();
				return Optional.of(patient);
			}
			catch (Exception e)
			{
				logger.warn("Patient " + reference + " not found", e);
				return Optional.empty();
			}
		}
		else
			throw new RuntimeException(
					"Reference should be an absolute local fhir store url to " + client.getServerBase());
	}

	@Override
	public void updatePatient(Patient patient)
	{
		Objects.requireNonNull(patient, "patient");

		String id = patient.getIdElement().toVersionless().getValue();
		logger.info("Updating patient {}", id);

		try
		{
			MethodOutcome outcome = clientFactory.getFhirStoreClient().update().resource(patient)
					.prefer(PreferReturnEnum.OPERATION_OUTCOME).execute();

			if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Could not update patient {}, message: {}, status: {}", id, e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not update patient {}, message: {}, status: {}, body: {}", id, e.getMessage(),
					e.getStatusCode(), e.getResponseBody());
			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Could not update patient " + id, e);
			throw e;
		}
	}
}
