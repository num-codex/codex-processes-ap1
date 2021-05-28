package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DomainResource;
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
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;

public class ReadData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ReadData.class);

	private final FhirContext fhirContext;
	private final FhirClientFactory fhirClientFactory;

	public ReadData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper, FhirContext fhirContext,
			FhirClientFactory fhirClientFactory)
	{
		super(clientProvider, taskHelper);

		this.fhirContext = fhirContext;
		this.fhirClientFactory = fhirClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(fhirClientFactory, "fhirClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();

		String pseudonym = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM);
		Optional<DateTimeType> exportFrom = getExportFrom(task);
		Optional<InstantType> exportTo = getExportTo(task);

		if (exportTo.isEmpty())
			throw new IllegalStateException("No export-to in Task");

		Bundle bundle = readDataAndCreateBundle(pseudonym, exportFrom.orElse(null), exportTo.get());

		execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, FhirResourceValues.create(bundle));
	}

	protected Bundle readDataAndCreateBundle(String pseudonym, DateTimeType from, InstantType to)
	{
		logger.info("Reading data for DIC pseudonym {}", pseudonym);

		Stream<DomainResource> resources = fhirClientFactory.getFhirClient().getNewData(pseudonym,
				from == null ? null : new DateWithPrecision(from.getValue(), from.getPrecision()), to.getValue());

		Bundle bundle = toBundle(pseudonym, resources);

		if (logger.isDebugEnabled())
			logger.debug("Created bundle: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));

		return bundle;
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
			entry.setFullUrl("urn:uuid:" + UUID.randomUUID());
			entry.getRequest().setMethod(HTTPVerb.PUT).setUrl(getConditionalUpdateUrl(pseudonym, r));
			entry.setResource(setSubjectOrIdentifier(clean(r), pseudonym));
			return entry;
		}).collect(Collectors.toList());

		b.setEntry(entries);
		return b;
	}

	private DomainResource clean(DomainResource r)
	{
		r.setIdElement(null);
		List<CanonicalType> profiles = r.getMeta().getProfile().stream().filter(
				p -> p.getValue().startsWith("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition"))
				.collect(Collectors.toList());
		r.setMeta(new Meta().setProfile(profiles));

		return r;
	}

	private Resource setSubjectOrIdentifier(Resource resource, String pseudonym)
	{
		Identifier identifier = new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym);

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
				((Condition) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Consent)
			{
				((Consent) resource).setPatient(patientRef);
				return resource;
			}
			else if (resource instanceof DiagnosticReport)
			{
				((DiagnosticReport) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Immunization)
			{
				((Immunization) resource).setPatient(patientRef);
				return resource;
			}
			else if (resource instanceof MedicationStatement)
			{
				((MedicationStatement) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Observation)
			{
				((Observation) resource).setSubject(patientRef);
				return resource;
			}
			else if (resource instanceof Procedure)
			{
				((Procedure) resource).setSubject(patientRef);
				return resource;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
		}
	}

	protected String getConditionalUpdateUrl(String pseudonym, DomainResource resource)
	{
		if (resource instanceof Patient)
			return "Patient?identifier=" + ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|"
					+ pseudonym;
		else if (resource instanceof Condition)
		{
			Condition c = (Condition) resource;
			Optional<CanonicalType> profile = c.getMeta().getProfile().stream()
					.filter(p -> p.getValue()
							.startsWith("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition"))
					.findFirst();
			if (profile.isPresent())
			{
				return "Condition?_profile=" + profile.get().getValue() + "&recorded-date="
						+ c.getRecordedDateElement().getValueAsString() + "&patient:identifier="
						+ ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name()
						+ " not supported, missing NUM profile");
		}
		else if (resource instanceof DiagnosticReport)
		{
			DiagnosticReport dr = (DiagnosticReport) resource;
			Optional<CanonicalType> profile = dr.getMeta().getProfile().stream()
					.filter(p -> p.getValue()
							.startsWith("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition"))
					.findFirst();
			if (profile.isPresent())
			{
				return "DiagnosticReport?_profile=" + profile.get().getValue() + "&date="
						+ dr.getEffectiveDateTimeType().getValueAsString() + "&patient:identifier="
						+ ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name()
						+ " not supported, missing NUM profile");
		}
		else if (resource instanceof Immunization)
		{
			Immunization i = (Immunization) resource;
			Optional<CanonicalType> profile = i.getMeta().getProfile().stream()
					.filter(p -> p.getValue()
							.startsWith("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition"))
					.findFirst();
			if (profile.isPresent())
			{
				return "Immunization?_profile=" + profile.get().getValue() + "&date="
						+ i.getOccurrenceDateTimeType().getValueAsString() + "&patient:identifier="
						+ ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name()
						+ " not supported, missing NUM profile");
		}
		else if (resource instanceof MedicationStatement)
		{
			MedicationStatement ms = (MedicationStatement) resource;
			Optional<CanonicalType> profile = ms.getMeta().getProfile().stream()
					.filter(p -> p.getValue()
							.startsWith("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition"))
					.findFirst();
			if (profile.isPresent())
			{
				return "MedicationStatement?_profile=" + profile.get().getValue() + "&effective="
						+ ms.getEffectiveDateTimeType().getValueAsString() + "&patient:identifier="
						+ ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name()
						+ " not supported, missing NUM profile");
		}

		else if (resource instanceof Observation)
		{
			Observation o = (Observation) resource;
			Optional<CanonicalType> profile = o.getMeta().getProfile().stream()
					.filter(p -> p.getValue()
							.startsWith("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition"))
					.findFirst();
			if (profile.isPresent())
			{
				return "Observation?_profile=" + profile.get().getValue() + "&date="
						+ o.getEffectiveDateTimeType().getValueAsString() + "&patient:identifier="
						+ ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name()
						+ " not supported, missing NUM profile");
		}
		else if (resource instanceof Procedure)
		{
			Procedure pr = (Procedure) resource;
			Optional<CanonicalType> profile = pr.getMeta().getProfile().stream()
					.filter(p -> p.getValue()
							.startsWith("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition"))
					.findFirst();
			if (profile.isPresent())
			{
				return "Procedure?_profile=" + profile.get().getValue() + "&date="
						+ pr.getPerformedDateTimeType().getValueAsString() + "&patient:identifier="
						+ ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name()
						+ " not supported, missing NUM profile");
		}

		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
	}
}
