package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_BAD_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponents.UriTemplateVariables;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.OutcomeLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceList;

public abstract class AbstractFhirClient implements GeccoFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractFhirClient.class);
	private static final OutcomeLogger outcomeLogger = new OutcomeLogger(logger);

	private static final class DomainResourceUniqueByUnqualifiedVersionlessId
	{
		private final DomainResource resource;
		private final String unqualifiedVersionlessIdValue;

		public DomainResourceUniqueByUnqualifiedVersionlessId(DomainResource resource)
		{
			this.resource = resource;

			unqualifiedVersionlessIdValue = resource.getIdElement().toUnqualifiedVersionless().getValue();
		}

		public DomainResource getResource()
		{
			return resource;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((unqualifiedVersionlessIdValue == null) ? 0 : unqualifiedVersionlessIdValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DomainResourceUniqueByUnqualifiedVersionlessId other = (DomainResourceUniqueByUnqualifiedVersionlessId) obj;
			if (unqualifiedVersionlessIdValue == null)
			{
				if (other.unqualifiedVersionlessIdValue != null)
					return false;
			}
			else if (!unqualifiedVersionlessIdValue.equals(other.unqualifiedVersionlessIdValue))
				return false;
			return true;
		}
	}

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

	private static final class QueryParameters implements UriTemplateVariables
	{
		final List<QuerParameter> parameters = new ArrayList<>();

		@Override
		public Object getValue(String name)
		{
			return parameters.stream().filter(p -> p.getValue(name) != null).findFirst().map(p -> p.getValue(name))
					.orElseThrow(() -> new IllegalArgumentException("No value for '" + name + "'"));
		}

		boolean add(QuerParameter param)
		{
			if (param != null)
				return parameters.add(param);
			else
				return false;
		}

		void replace(UriComponentsBuilder urlBuilder)
		{
			parameters.forEach(p -> p.replace(urlBuilder));
		}
	}

	private static final class QuerParameter
	{
		final String name;
		final Map<String, String> valuesByTemplateParameter = new HashMap<>();

		QuerParameter(String templateParameter, String name, String... values)
		{
			Objects.requireNonNull(templateParameter, "templateParameter");
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(values, "values");

			this.name = name;
			IntStream.range(0, values.length).forEach(i ->
			{
				if (values[i] != null)
					valuesByTemplateParameter.put(templateParameter + "_" + i, values[i]);
			});
		}

		void replace(UriComponentsBuilder builder)
		{
			builder.replaceQueryParam(name);
			valuesByTemplateParameter.keySet()
					.forEach(templateParam -> builder.queryParam(name, "{" + templateParam + "}"));
		}

		String getValue(String templateParameter)
		{
			return valuesByTemplateParameter.get(templateParameter);
		}
	}

	protected final GeccoClient geccoClient;

	/**
	 * @param geccoClient
	 *            not <code>null</code>
	 * @param clientFactory
	 *            not <code>null</code>
	 * @param searchBundleOverride
	 *            may be <code>null</code>
	 */
	public AbstractFhirClient(GeccoClient geccoClient)
	{
		this.geccoClient = geccoClient;
	}

	@Override
	public PatientReferenceList getPatientReferencesWithNewData(DateWithPrecision exportFrom, Date exportTo)
	{
		Bundle searchBundle = getSearchBundle(exportFrom, exportTo);
		BundleType expectedResponseType = BundleType.BATCH.equals(searchBundle.getType()) ? BundleType.BATCHRESPONSE
				: BundleType.TRANSACTIONRESPONSE;

		if (logger.isDebugEnabled())
			logger.debug("Executing Search-Bundle: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(searchBundle));

		Bundle resultBundle = geccoClient.getGenericFhirClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(resultBundle));

		if (!resultBundle.hasType() || !expectedResponseType.equals(resultBundle.getType()) || !resultBundle.hasEntry())
		{
			logger.warn("Search-Bundle result not a {} or has no entries", expectedResponseType.toCode());
			throw new RuntimeException(
					"Search-Bundle result not a " + expectedResponseType.toCode() + " or has no entries");
		}

		for (int i = 0; i < resultBundle.getEntry().size(); i++)
		{
			BundleEntryComponent entry = resultBundle.getEntry().get(i);

			if (!entry.hasResource() || !(entry.getResource() instanceof Bundle) || !entry.hasResponse()
					|| !entry.getResponse().hasStatus() || !entry.getResponse().getStatus().startsWith("200"))
			{
				logger.warn(
						"Error in Search-Bundle at index {}: entry has no Bundle resource or response is not 200 OK",
						i);
				if (entry.hasResource() && !(entry.getResource() instanceof Bundle))
					logger.debug("Unexpected entry resource: {}",
							geccoClient.getFhirContext().newJsonParser().encodeResourceToString(entry.getResource()));
			}
		}

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
		return PatientReference
				.from(new IdType(geccoClient.getServerBase(), idElement.getResourceType(), idElement.getIdPart(), null)
						.getValue());
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

		Bundle resultBundle = (Bundle) geccoClient.getGenericFhirClient().search().byUrl(url)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(resultBundle));

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
			Path searchBundleOverride = geccoClient.getSearchBundleOverride();

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
						logger.warn("Using Search-Bundle override from {}", searchBundleOverride.toString());
						return geccoClient.getFhirContext().newXmlParser().parseResource(Bundle.class, in);
					}
				}
			}
			else
			{
				try (InputStream in = getClass().getResourceAsStream("/fhir/Bundle/SearchBundle.xml"))
				{
					logger.debug("Using internal Search-Bundle");
					return geccoClient.getFhirContext().newXmlParser().parseResource(Bundle.class, in);
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
			UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(entry.getRequest().getUrl());
			QueryParameters queryParameters = new QueryParameters();

			if (RESOURCES_WITH_PATIENT_REF.contains(resource))
			{
				if (patientId != null)
					queryParameters.add(createPatIdSearchUrlPart(patientId));
				else
					queryParameters.add(createPatPrefixPseudonymSearchUrlPart(pseudonym));

				if (includePatient)
					queryParameters.add(createIncludeSearchUrlPart(resource));
			}
			else if ("Patient".equals(resource))
			{
				// filtering search for patient if patient id known
				if (patientId != null)
					return null;
				else
					queryParameters.add(createPseudonymSearchUrlPart(pseudonym));
			}
			else
			{
				logger.warn(
						"Search-Bundle contains entry with invalid serach query {}, target resource {} not supported",
						entry.getRequest().getUrl(), resource);
				throw new RuntimeException("Search-Bundle contains entry with invalid serach query, target resource "
						+ resource + " not supported");
			}

			queryParameters.add(new QuerParameter("from_to", "_lastUpdated", createExportFromSearchUrlPart(exportFrom),
					createExportToSearchUrlPart(exportTo)));

			queryParameters.replace(urlBuilder);
			UriComponents url = urlBuilder.encode().build().expand(queryParameters);

			entry.getRequest().setUrl(url.toString());
			return entry;
		};
	}

	private QuerParameter createPseudonymSearchUrlPart(String pseudonym)
	{
		if (pseudonym == null || pseudonym.isBlank())
			return null;
		else
			return new QuerParameter("pseudonym", "identifier",
					NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym);
	}

	private QuerParameter createPatIdSearchUrlPart(String patientId)
	{
		if (patientId == null || patientId.isBlank())
			return null;
		else
			return new QuerParameter("patientId", "patient", patientId);
	}

	private QuerParameter createPatPrefixPseudonymSearchUrlPart(String pseudonym)
	{
		if (pseudonym == null || pseudonym.isBlank())
			return null;
		else
			return new QuerParameter("pseudonym", "patient:identifier",
					NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym);
	}

	private String createExportFromSearchUrlPart(DateWithPrecision exportFrom)
	{
		if (exportFrom == null)
			return null;
		else
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

			return "ge" + dateTime;
		}
	}

	private String createExportToSearchUrlPart(Date exportTo)
	{
		if (exportTo == null)
			return null;
		else
			return "lt" + TIME_FORMAT.format(exportTo);
	}

	private QuerParameter createIncludeSearchUrlPart(String resource)
	{
		if (resource == null)
			return null;
		else
			return new QuerParameter("include_resource", "_include", resource + ":patient");
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
		// includes first
		return Stream.concat(
				bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
						.filter(e -> e.hasSearch() && SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
						.map(BundleEntryComponent::getResource).filter(r -> r instanceof DomainResource)
						.map(r -> (DomainResource) r),
				bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
						.filter(e -> e.hasSearch() && SearchEntryMode.MATCH.equals(e.getSearch().getMode()))
						.map(BundleEntryComponent::getResource).filter(r -> r instanceof DomainResource)
						.map(r -> (DomainResource) r));
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
		IGenericClient client = geccoClient.getGenericFhirClient();

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
			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_BAD_PATIENT_REFERENCE,
					"Patient reference not an absolute reference to a resouce at the local GECCO FHIR server: "
							+ client.getServerBase());
	}

	@Override
	public void updatePatient(Patient patient)
	{
		Objects.requireNonNull(patient, "patient");

		String id = patient.getIdElement().toVersionless().getValue();
		logger.info("Updating patient {}", id);

		try
		{
			MethodOutcome outcome = geccoClient.getGenericFhirClient().update().resource(patient)
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

	protected Stream<DomainResource> distinctById(Stream<DomainResource> resources)
	{
		return resources.map(DomainResourceUniqueByUnqualifiedVersionlessId::new).distinct()
				.map(DomainResourceUniqueByUnqualifiedVersionlessId::getResource);
	}
}
