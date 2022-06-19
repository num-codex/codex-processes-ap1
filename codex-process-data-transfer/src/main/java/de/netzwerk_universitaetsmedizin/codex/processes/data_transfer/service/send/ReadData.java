package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.HAPI_USER_DATA_SOURCE_ID_ELEMENT;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_CODE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_SYSTEM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;

public class ReadData extends AbstractServiceDelegate
{
	private static final String NUM_CODEX_STRUCTURE_DEFINITION_PREFIX = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition";
	private static final String MII_LAB_STRUCTURED_DEFINITION = "https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab";
	private static final String NUM_CODEX_DO_NOT_RESUSCITAT_ORDER = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/do-not-resuscitate-order";

	private static final Logger logger = LoggerFactory.getLogger(ReadData.class);

	private final FhirContext fhirContext;
	private final GeccoClientFactory geccoClientFactory;

	public ReadData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FhirContext fhirContext, GeccoClientFactory geccoClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.fhirContext = fhirContext;
		this.geccoClientFactory = geccoClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(geccoClientFactory, "geccoClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		String pseudonym = getPseudonym(execution)
				.orElseThrow(() -> new IllegalStateException("Patient reference does not contain identifier"));

		Task task = getCurrentTaskFromExecutionVariables();
		DateTimeType exportFrom = getExportFrom(task).orElse(null);
		InstantType exportTo = getExportTo(task).orElseThrow(() -> new IllegalStateException("No export-to in Task"));

		Bundle bundle = readDataAndCreateBundle(pseudonym, exportFrom, exportTo);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, FhirResourceValues.create(bundle));
	}

	protected Bundle readDataAndCreateBundle(String pseudonym, DateTimeType from, InstantType to)
	{
		logger.info("Reading data for DIC pseudonym {}", pseudonym);

		Stream<DomainResource> resources = geccoClientFactory.getGeccoClient().getFhirClient().getNewData(pseudonym,
				from == null ? null : new DateWithPrecision(from.getValue(), from.getPrecision()), to.getValue());

		Bundle bundle = toBundle(pseudonym, resources);

		if (logger.isDebugEnabled())
			logger.debug("Created bundle: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));

		return bundle;
	}

	private Optional<String> getPseudonym(DelegateExecution execution)
	{
		PatientReference reference = (PatientReference) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE);

		if (reference.hasIdentifier())
			return Optional.of(reference.getIdentifier().getValue());
		else
			return Optional.empty();
	}

	private Optional<DateTimeType> getExportFrom(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM, DateTimeType.class).findFirst();
	}

	private Optional<InstantType> getExportTo(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO, InstantType.class).findFirst();
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}

	protected Bundle toBundle(String pseudonym, Stream<DomainResource> resources)
	{
		Bundle b = new Bundle();
		b.setType(BundleType.TRANSACTION);

		List<BundleEntryComponent> entries = resources.map(r ->
		{
			BundleEntryComponent entry = b.addEntry();

			// storing original resource reference for validation error tracking
			entry.setUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT, getAbsoluteId(r));

			entry.setFullUrl("urn:uuid:" + UUID.randomUUID());
			entry.getRequest().setMethod(HTTPVerb.PUT).setUrl(getConditionalUpdateUrl(pseudonym, r));
			entry.setResource(setSubjectOrIdentifier(clean(r), pseudonym));

			return entry;
		}).collect(Collectors.toList());

		b.setEntry(entries);
		return b;
	}

	private IdType getAbsoluteId(DomainResource r)
	{
		if (r == null)
			return null;

		return r.getIdElement().isAbsolute() ? r.getIdElement()
				: r.getIdElement().withServerBase(geccoClientFactory.getServerBase(), r.getResourceType().name());
	}

	private DomainResource clean(DomainResource r)
	{
		r.setIdElement(null);
		List<CanonicalType> profiles = r.getMeta().getProfile().stream()
				.filter(p -> p.getValue().startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX)
						|| MII_LAB_STRUCTURED_DEFINITION.equals(p.getValue()))
				.collect(Collectors.toList());
		r.setMeta(new Meta().setProfile(profiles));

		return r;
	}

	private Resource setSubjectOrIdentifier(Resource resource, String pseudonym)
	{
		Identifier identifier = new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym);
		identifier.getType().getCodingFirstRep().setSystem(IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_SYSTEM)
				.setCode(IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_CODE);

		if (resource instanceof Patient)
		{
			((Patient) resource).setIdentifier(Collections.singletonList(identifier));
			return resource;
		}
		else
		{
			Reference patientRef = new Reference().setIdentifier(identifier);

			if (resource instanceof Condition)
			{
				((Condition) resource).setIdentifier(Collections.emptyList());
				((Condition) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Consent)
			{
				((Consent) resource).setIdentifier(Collections.emptyList());
				((Consent) resource).setPatient(patientRef);
				return resource;
			}
			else if (resource instanceof DiagnosticReport)
			{
				((DiagnosticReport) resource).setIdentifier(Collections.emptyList());
				((DiagnosticReport) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Immunization)
			{
				((Immunization) resource).setIdentifier(Collections.emptyList());
				((Immunization) resource).setPatient(patientRef);
				return resource;
			}
			else if (resource instanceof MedicationStatement)
			{
				((MedicationStatement) resource).setIdentifier(Collections.emptyList());
				((MedicationStatement) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Observation)
			{
				((Observation) resource).setIdentifier(Collections.emptyList());
				((Observation) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Procedure)
			{
				((Procedure) resource).setIdentifier(Collections.emptyList());
				((Procedure) resource).setSubject(patientRef);
				return resource;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
		}
	}

	protected String getConditionalUpdateUrl(String pseudonym, DomainResource resource)
	{
		String patientIdentifier = ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;

		if (resource instanceof Patient)
		{
			return "Patient?identifier=" + patientIdentifier;
		}
		else if (resource instanceof Condition)
		{
			Condition c = (Condition) resource;
			String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));

			String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Condition.name(), profileUrl,
					patientIdentifier);

			if (c.hasRecordedDateElement() && c.getRecordedDateElement().getValueAsString() != null)
				updateUrl = updateUrl + "&recorded-date=" + c.getRecordedDateElement().getValueAsString();

			if (c.hasCategory() && c.getCategoryFirstRep().hasCoding())
				updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(c.getCategoryFirstRep().getCodingFirstRep());

			if (c.hasCode() && c.getCode().hasCoding())
				updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(c.getCode().getCodingFirstRep());

			if (c.hasBodySite() && c.getBodySiteFirstRep().hasCoding())
				updateUrl = updateUrl + "&body-site=" + getCodingUpdateUrl(c.getBodySiteFirstRep().getCodingFirstRep());

			return updateUrl;
		}
		else if (resource instanceof Consent)
		{
			Consent c = (Consent) resource;
			String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));

			if (NUM_CODEX_DO_NOT_RESUSCITAT_ORDER.equals(profileUrl))
			{
				boolean scopePresent = c.getScope().getCoding().stream().filter(Coding::hasSystem)
						.filter(co -> "http://terminology.hl7.org/CodeSystem/consentscope".equals(co.getSystem()))
						.filter(Coding::hasCode).anyMatch(co -> "adr".equals(co.getCode()));
				boolean categoryPresent = c.getCategory().stream().flatMap(coc -> coc.getCoding().stream())
						.filter(Coding::hasSystem)
						.filter(co -> "http://terminology.hl7.org/CodeSystem/consentcategorycodes"
								.equals(co.getSystem()))
						.filter(Coding::hasCode).anyMatch(co -> "dnr".equals(co.getCode()));

				if (scopePresent && categoryPresent)
					return getBaseConditionalUpdateUrl(ResourceType.Consent.name(), profileUrl, patientIdentifier)
							+ "&scope=http://terminology.hl7.org/CodeSystem/consentscope|adr"
							+ "&category=http://terminology.hl7.org/CodeSystem/consentcategorycodes|dnr";

				else
					throw new RuntimeException("Resource of type Consent with profile " + profileUrl
							+ " is missing scope: http://terminology.hl7.org/CodeSystem/consentscope|adr and/or category: http://terminology.hl7.org/CodeSystem/consentcategorycodes|dnr");
			}
			else
				throw new RuntimeException("Resource of type Consent with profile " + profileUrl + " not supported");
		}
		else if (resource instanceof DiagnosticReport)
		{
			DiagnosticReport dr = (DiagnosticReport) resource;
			String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));

			String updateUrl = getBaseConditionalUpdateUrl(ResourceType.DiagnosticReport.name(), profileUrl,
					patientIdentifier);

			if (dr.hasEffectiveDateTimeType() && dr.getEffectiveDateTimeType().getValueAsString() != null)
				updateUrl = updateUrl + "&date=" + dr.getEffectiveDateTimeType().getValueAsString();

			if (dr.hasCategory() && dr.getCategoryFirstRep().hasCoding())
				updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(dr.getCategoryFirstRep().getCodingFirstRep());

			if (dr.hasCode() && dr.getCode().hasCoding())
				updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(dr.getCode().getCodingFirstRep());

			return updateUrl;
		}
		else if (resource instanceof Immunization)
		{
			Immunization i = (Immunization) resource;
			String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));

			String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Immunization.name(), profileUrl,
					patientIdentifier);

			if (i.hasOccurrenceDateTimeType() && i.getOccurrenceDateTimeType().getValueAsString() != null)
				updateUrl = updateUrl + "&date=" + i.getOccurrenceDateTimeType().getValueAsString();

			if (i.hasVaccineCode() && i.getVaccineCode().hasCoding())
				updateUrl = updateUrl + "&vaccine-code=" + i.getVaccineCode().getCodingFirstRep();

			return updateUrl;
		}
		else if (resource instanceof MedicationStatement)
		{
			MedicationStatement ms = (MedicationStatement) resource;
			String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));

			String updateUrl = getBaseConditionalUpdateUrl(ResourceType.MedicationStatement.name(), profileUrl,
					patientIdentifier);

			if (ms.hasEffectiveDateTimeType() && ms.getEffectiveDateTimeType().getValueAsString() != null)
				updateUrl = updateUrl + "&effective=" + ms.getEffectiveDateTimeType();

			if (ms.hasMedicationCodeableConcept() && ms.getMedicationCodeableConcept().hasCoding())
				updateUrl = updateUrl + "&code="
						+ getCodingUpdateUrl(ms.getMedicationCodeableConcept().getCodingFirstRep());

			return updateUrl;
		}
		else if (resource instanceof Observation)
		{
			Observation o = (Observation) resource;
			String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX)
					|| MII_LAB_STRUCTURED_DEFINITION.equals(v));

			String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Observation.name(), profileUrl,
					patientIdentifier);

			if (o.hasEffectiveDateTimeType() && o.getEffectiveDateTimeType().getValueAsString() != null)
				updateUrl = updateUrl + "&date=" + o.getEffectiveDateTimeType().getValueAsString();

			if (o.hasCategory() && o.getCategoryFirstRep().hasCoding())
				updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(o.getCategoryFirstRep().getCodingFirstRep());

			if (o.hasCode() && o.getCode().hasCoding())
				updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(o.getCode().getCodingFirstRep());

			return updateUrl;
		}
		else if (resource instanceof Procedure)
		{
			Procedure p = (Procedure) resource;
			String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));

			String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Procedure.name(), profileUrl,
					patientIdentifier);

			if (p.hasPerformedDateTimeType() && p.getPerformedDateTimeType().getValueAsString() != null)
				updateUrl = updateUrl + "&date=" + p.getPerformedDateTimeType().getValueAsString();

			if (p.hasCategory() && p.getCategory().hasCoding())
				updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(p.getCategory().getCodingFirstRep());

			if (p.hasCode() && p.getCode().hasCoding())
				updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(p.getCode().getCodingFirstRep());

			return updateUrl;

		}
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
	}

	private String getProfileUrl(DomainResource resource, Predicate<String> filter)
	{
		return resource.getMeta().getProfile().stream().map(CanonicalType::getValue).filter(filter).findFirst()
				.orElseThrow(() -> new RuntimeException("Resource of type " + resource.getResourceType().name()
						+ " not supported, missing NUM or MII profile"));
	}

	private String getBaseConditionalUpdateUrl(String resourceName, String profileUrl, String patientIdentifier)
	{
		return resourceName + "?_profile=" + profileUrl + "&patient:identifier=" + patientIdentifier;
	}

	private String getCodingUpdateUrl(Coding coding)
	{
		return coding.getSystem() + "|" + coding.getCode();
	}
}