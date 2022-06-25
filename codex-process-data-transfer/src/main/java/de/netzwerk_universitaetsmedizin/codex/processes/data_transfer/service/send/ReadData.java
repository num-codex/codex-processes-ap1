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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import org.hl7.fhir.r4.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.r4.model.Condition.ConditionStageComponent;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Consent.ConsentVerificationComponent;
import org.hl7.fhir.r4.model.Consent.provisionActorComponent;
import org.hl7.fhir.r4.model.Consent.provisionComponent;
import org.hl7.fhir.r4.model.Consent.provisionDataComponent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportMediaComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationPerformerComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationProtocolAppliedComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationReactionComponent;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.ContactComponent;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Procedure.ProcedureFocalDeviceComponent;
import org.hl7.fhir.r4.model.Procedure.ProcedurePerformerComponent;
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
	private static final String NUM_CODEX_BLOOD_GAS_PANEL = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-gas-panel";

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

	protected Bundle toBundle(String pseudonym, Stream<DomainResource> resourcesStream)
	{
		Bundle b = new Bundle();
		b.setType(BundleType.TRANSACTION);

		List<DomainResource> resources = resourcesStream.collect(Collectors.toList());

		Map<String, DomainResource> resourcesById = resources.stream().collect(
				Collectors.toMap(r -> r.getIdElement().toUnqualifiedVersionless().getValue(), Function.identity()));
		Map<String, UUID> uuidsById = resources.stream().collect(
				Collectors.toMap(r -> r.getIdElement().toUnqualifiedVersionless().getValue(), r -> UUID.randomUUID()));

		List<DomainResource> resourcesWithTemporaryReferences = fixReferences(resources, resourcesById, uuidsById);
		List<BundleEntryComponent> entries = resourcesWithTemporaryReferences.stream().map(r ->
		{
			BundleEntryComponent entry = b.addEntry();

			// storing original resource reference for validation error tracking
			entry.setUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT, getAbsoluteId(r));

			entry.setFullUrl(
					"urn:uuid:" + uuidsById.get(r.getIdElement().toUnqualifiedVersionless().getValue()).toString());
			entry.getRequest().setMethod(HTTPVerb.PUT).setUrl(getConditionalUpdateUrl(pseudonym, r));
			entry.setResource(setSubjectOrIdentifier(clean(r), pseudonym));

			return entry;
		}).collect(Collectors.toList());

		b.setEntry(entries);
		return b;
	}

	private List<DomainResource> fixReferences(List<DomainResource> resources,
			Map<String, DomainResource> resourcesById, Map<String, UUID> uuidsById)
	{
		return resources.stream().map(r -> fixReferences(r, resourcesById, uuidsById)).collect(Collectors.toList());
	}

	private DomainResource fixReferences(DomainResource resource, Map<String, DomainResource> resourcesById,
			Map<String, UUID> uuidsById)
	{
		if (resource instanceof Observation && resource.getMeta().getProfile().stream().map(CanonicalType::getValue)
				.anyMatch(url -> NUM_CODEX_BLOOD_GAS_PANEL.equals(url)
						|| (url != null && url.startsWith(NUM_CODEX_BLOOD_GAS_PANEL + "|"))))
		{
			Observation observation = (Observation) resource;
			List<Reference> oldReferences = observation.getHasMember();
			List<Reference> fixedReferences = new ArrayList<>();

			for (int i = 0; i < oldReferences.size(); i++)
			{
				if (uuidsById.containsKey(oldReferences.get(i).getReference()))
				{
					logger.debug(
							"Replacing reference at Observation.hasMember[{}] from resource {} with bundle temporary id",
							i, resource.getIdElement().getValue());
					fixedReferences.add(oldReferences.get(i).copy()
							.setReference("urn:uuid:" + uuidsById.get(oldReferences.get(i).getReference()).toString()));
				}
				else
					logger.warn("Removing reference at Observation.hasMember[{}] from resource {}", i,
							resource.getIdElement().getValue());
			}

			observation.setHasMember(fixedReferences);
			return observation;
		}

		return resource;
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
		r.setText(null);

		cleanUnsupportedReferences(r);

		return r;
	}

	private <R extends DomainResource> void cleanUnsupportedReference(R resource, String path,
			Function<R, Boolean> hasReference, BiFunction<R, Reference, R> setReference)
	{
		if (hasReference.apply(resource))
		{
			logger.warn("Removing reference at {} from resource {}", path, resource.getIdElement().getValue());
			setReference.apply(resource, (Reference) null);
		}
	}

	private <R extends DomainResource> void cleanUnsupportedReferences(R resource, String path,
			Function<R, Boolean> hasReferences, BiFunction<R, List<Reference>, R> setReferences)
	{
		if (hasReferences.apply(resource))
		{
			logger.warn("Removing reference at {} from resource {}", path, resource.getIdElement().getValue());
			setReferences.apply(resource, (List<Reference>) null);
		}
	}

	private <R extends DomainResource, C> void cleanUnsupportedReferenceFromComponents(R resource, String path,
			Function<R, Boolean> hasComponents, Function<R, List<C>> getComponents, Function<C, Boolean> hasReference,
			BiFunction<C, Reference, C> setReference)
	{
		if (hasComponents.apply(resource))
		{
			List<C> components = getComponents.apply(resource);
			for (int i = 0; i < components.size(); i++)
			{
				C component = components.get(i);
				if (hasReference.apply(component))
				{
					logger.warn("Removing reference at " + path + " from resource {}", i,
							resource.getIdElement().getValue());
					setReference.apply(component, null);
				}
			}
		}
	}

	private <R extends DomainResource, C> void cleanUnsupportedReferencesFromComponents(R resource, String path,
			Function<R, Boolean> hasComponents, Function<R, List<C>> getComponents, Function<C, Boolean> hasReferences,
			BiFunction<C, List<Reference>, C> setReferences)
	{
		if (hasComponents.apply(resource))
		{
			List<C> components = getComponents.apply(resource);
			for (int i = 0; i < components.size(); i++)
			{
				C component = components.get(i);
				if (hasReferences.apply(component))
				{
					logger.warn("Removing references at " + path + " from resource {}", i,
							resource.getIdElement().getValue());
					setReferences.apply(component, null);
				}
			}
		}
	}

	private <R extends DomainResource, C1, C2> void cleanUnsupportedReferencesFromComponentComponents(R resource,
			String path, Function<R, Boolean> hasComponent1, Function<R, C1> getComponent1,
			Function<C1, Boolean> hasComponents2, Function<C1, List<C2>> getComponents2,
			Function<C2, Boolean> hasReference, BiFunction<C2, Reference, C2> setReference)
	{
		if (hasComponent1.apply(resource))
		{
			C1 component1 = getComponent1.apply(resource);
			if (hasComponents2.apply(component1))
			{
				List<C2> components2 = getComponents2.apply(component1);
				for (int i = 0; i < components2.size(); i++)
				{
					C2 component2 = components2.get(i);
					if (hasReference.apply(component2))
					{
						logger.warn("Removing reference at " + path + " from resource {}", i,
								resource.getIdElement().getValue());
						setReference.apply(component2, null);
					}
				}
			}
		}
	}

	private void cleanUnsupportedReferences(DomainResource resource)
	{
		if (resource == null)
		{
			return;
		}
		else if (resource instanceof Patient)
		{
			Patient patient = (Patient) resource;
			cleanUnsupportedReferenceFromComponents(patient, "Patient.contact[{}].organization", Patient::hasContact,
					Patient::getContact, ContactComponent::hasOrganization, ContactComponent::setOrganization);
			cleanUnsupportedReferences(patient, "Patient.generalPractitioner", Patient::hasGeneralPractitioner,
					Patient::setGeneralPractitioner);
			cleanUnsupportedReference(patient, "Patient.managingOrganization", Patient::hasManagingOrganization,
					Patient::setManagingOrganization);
			cleanUnsupportedReferenceFromComponents(patient, "Patient.link[{}].other", Patient::hasLink,
					Patient::getLink, PatientLinkComponent::hasOther, PatientLinkComponent::setOther);
		}
		else if (resource instanceof Condition)
		{
			Condition condition = (Condition) resource;
			cleanUnsupportedReference(condition, "Condition.encounter", Condition::hasEncounter,
					Condition::setEncounter);
			cleanUnsupportedReference(condition, "Condition.recorder", Condition::hasRecorder, Condition::setRecorder);
			cleanUnsupportedReference(condition, "Condition.asserter", Condition::hasAsserter, Condition::setAsserter);
			cleanUnsupportedReferencesFromComponents(condition, "Condition.stage[{}].assessment", Condition::hasStage,
					Condition::getStage, ConditionStageComponent::hasAssessment,
					ConditionStageComponent::setAssessment);
			cleanUnsupportedReferencesFromComponents(condition, "Condition.evidence[{}].detail", Condition::hasEvidence,
					Condition::getEvidence, ConditionEvidenceComponent::hasDetail,
					ConditionEvidenceComponent::setDetail);
		}
		else if (resource instanceof Consent)
		{
			Consent consent = (Consent) resource;
			cleanUnsupportedReferences(consent, "Consent.performer", Consent::hasPerformer, Consent::setPerformer);
			cleanUnsupportedReferences(consent, "Consent.organization", Consent::hasOrganization,
					Consent::setOrganization);
			cleanUnsupportedReferenceFromComponents(consent, "Consent.verification.verifiedWith",
					Consent::hasVerification, Consent::getVerification, ConsentVerificationComponent::hasVerifiedWith,
					ConsentVerificationComponent::setVerifiedWith);
			// provisionComponent and provisionActorComponent class names starts with lower case p
			cleanUnsupportedReferencesFromComponentComponents(consent, "Consent.provision.actor[{}].reference",
					Consent::hasProvision, Consent::getProvision, provisionComponent::hasActor,
					provisionComponent::getActor, provisionActorComponent::hasReference,
					provisionActorComponent::setReference);
			// provisionComponent and provisionDataComponent class names starts with lower case p
			cleanUnsupportedReferencesFromComponentComponents(consent, "Consent.provision.data[{}].reference",
					Consent::hasProvision, Consent::getProvision, provisionComponent::hasData,
					provisionComponent::getData, provisionDataComponent::hasReference,
					provisionDataComponent::setReference);
		}
		else if (resource instanceof DiagnosticReport)
		{
			DiagnosticReport report = (DiagnosticReport) resource;

			cleanUnsupportedReferences(report, "DiagnosticReport.basedOn", DiagnosticReport::hasBasedOn,
					DiagnosticReport::setBasedOn);
			cleanUnsupportedReference(report, "DiagnosticReport.encounter", DiagnosticReport::hasEncounter,
					DiagnosticReport::setEncounter);
			cleanUnsupportedReferences(report, "DiagnosticReport.performer", DiagnosticReport::hasPerformer,
					DiagnosticReport::setPerformer);
			cleanUnsupportedReferences(report, "DiagnosticReport.resultsInterpreter",
					DiagnosticReport::hasResultsInterpreter, DiagnosticReport::setResultsInterpreter);
			cleanUnsupportedReferences(report, "DiagnosticReport.specimen", DiagnosticReport::hasSpecimen,
					DiagnosticReport::setSpecimen);
			cleanUnsupportedReferences(report, "DiagnosticReport.result", DiagnosticReport::hasResult,
					DiagnosticReport::setResult);
			cleanUnsupportedReferences(report, "DiagnosticReport.imagingStudy", DiagnosticReport::hasImagingStudy,
					DiagnosticReport::setImagingStudy);
			cleanUnsupportedReferenceFromComponents(report, "DiagnosticReport.media[{}].link",
					DiagnosticReport::hasMedia, DiagnosticReport::getMedia, DiagnosticReportMediaComponent::hasLink,
					DiagnosticReportMediaComponent::setLink);
		}
		else if (resource instanceof Immunization)
		{
			Immunization immunization = (Immunization) resource;

			cleanUnsupportedReference(immunization, "Immunization.encounter", Immunization::hasEncounter,
					Immunization::setEncounter);
			cleanUnsupportedReference(immunization, "Immunization.location", Immunization::hasLocation,
					Immunization::setLocation);
			cleanUnsupportedReference(immunization, "Immunization.manufacturer", Immunization::hasManufacturer,
					Immunization::setManufacturer);
			cleanUnsupportedReferenceFromComponents(immunization, "Immunization.performer.actor",
					Immunization::hasPerformer, Immunization::getPerformer, ImmunizationPerformerComponent::hasActor,
					ImmunizationPerformerComponent::setActor);
			cleanUnsupportedReferences(immunization, "Immunization.reasonReference", Immunization::hasReasonReference,
					Immunization::setReasonReference);
			cleanUnsupportedReferenceFromComponents(immunization, "Immunization.reaction.detail",
					Immunization::hasReaction, Immunization::getReaction, ImmunizationReactionComponent::hasDetail,
					ImmunizationReactionComponent::setDetail);
			cleanUnsupportedReferenceFromComponents(immunization, "Immunization.protocolApplied.authority",
					Immunization::hasProtocolApplied, Immunization::getProtocolApplied,
					ImmunizationProtocolAppliedComponent::hasAuthority,
					ImmunizationProtocolAppliedComponent::setAuthority);
		}
		else if (resource instanceof MedicationStatement)
		{
			MedicationStatement medication = (MedicationStatement) resource;
			cleanUnsupportedReferences(medication, "MedicationStatement.basedOn", MedicationStatement::hasBasedOn,
					MedicationStatement::setBasedOn);
			cleanUnsupportedReferences(medication, "MedicationStatement.partOf", MedicationStatement::hasPartOf,
					MedicationStatement::setPartOf);
			cleanUnsupportedReference(medication, "MedicationStatement.medicationReference",
					MedicationStatement::hasMedicationReference, MedicationStatement::setMedication);
			cleanUnsupportedReference(medication, "MedicationStatement.context", MedicationStatement::hasContext,
					MedicationStatement::setContext);
			cleanUnsupportedReference(medication, "MedicationStatement.informationSource",
					MedicationStatement::hasInformationSource, MedicationStatement::setInformationSource);
			cleanUnsupportedReferences(medication, "MedicationStatement.derivedFrom",
					MedicationStatement::hasDerivedFrom, MedicationStatement::setDerivedFrom);
			cleanUnsupportedReferences(medication, "MedicationStatement.reasonReference",
					MedicationStatement::hasReasonReference, MedicationStatement::setReasonReference);
		}
		else if (resource instanceof Observation)
		{
			Observation observation = (Observation) resource;

			cleanUnsupportedReferences(observation, "Observation.basedOn", Observation::hasBasedOn,
					Observation::setBasedOn);
			cleanUnsupportedReferences(observation, "Observation.partOf", Observation::hasPartOf,
					Observation::setPartOf);
			cleanUnsupportedReferences(observation, "Observation.focus", Observation::hasFocus, Observation::setFocus);
			cleanUnsupportedReference(observation, "Observation.encounter", Observation::hasEncounter,
					Observation::setEncounter);
			cleanUnsupportedReferences(observation, "Observation.performer", Observation::hasPerformer,
					Observation::setPerformer);
			cleanUnsupportedReference(observation, "Observation.specimen", Observation::hasSpecimen,
					Observation::setSpecimen);
			cleanUnsupportedReference(observation, "Observation.device", Observation::hasDevice,
					Observation::setDevice);

			// Do not remove blood-gas-panel member references
			if (!resource.getMeta().getProfile().stream().map(CanonicalType::getValue)
					.anyMatch(url -> NUM_CODEX_BLOOD_GAS_PANEL.equals(url)
							|| (url != null && url.startsWith(NUM_CODEX_BLOOD_GAS_PANEL + "|"))))
			{
				cleanUnsupportedReferences(observation, "Observation.hasMember", Observation::hasHasMember,
						Observation::setHasMember);
			}

			cleanUnsupportedReferences(observation, "Observation.derivedFrom", Observation::hasDerivedFrom,
					Observation::setDerivedFrom);
		}
		else if (resource instanceof Procedure)
		{
			Procedure procedure = (Procedure) resource;

			cleanUnsupportedReferences(procedure, "Procedure.basedOn", Procedure::hasBasedOn, Procedure::setBasedOn);
			cleanUnsupportedReferences(procedure, "Procedure.partOf", Procedure::hasPartOf, Procedure::setPartOf);
			cleanUnsupportedReference(procedure, "Procedure.encounter", Procedure::hasEncounter,
					Procedure::setEncounter);
			cleanUnsupportedReference(procedure, "Procedure.recorder", Procedure::hasRecorder, Procedure::setRecorder);
			cleanUnsupportedReference(procedure, "Procedure.asserter", Procedure::hasAsserter, Procedure::setAsserter);
			cleanUnsupportedReferenceFromComponents(procedure, "", Procedure::hasPerformer, Procedure::getPerformer,
					ProcedurePerformerComponent::hasActor, ProcedurePerformerComponent::setActor);
			cleanUnsupportedReferenceFromComponents(procedure, "", Procedure::hasPerformer, Procedure::getPerformer,
					ProcedurePerformerComponent::hasOnBehalfOf, ProcedurePerformerComponent::setOnBehalfOf);
			cleanUnsupportedReference(procedure, "Procedure.location", Procedure::hasLocation, Procedure::setLocation);
			cleanUnsupportedReferences(procedure, "Procedure.reasonReference", Procedure::hasReasonReference,
					Procedure::setReasonReference);
			cleanUnsupportedReferences(procedure, "Procedure.report", Procedure::hasReport, Procedure::setReport);
			cleanUnsupportedReferences(procedure, "Procedure.complicationDetail", Procedure::hasComplicationDetail,
					Procedure::setComplicationDetail);
			cleanUnsupportedReferenceFromComponents(procedure, "Procedure.focalDevice.manipulated",
					Procedure::hasFocalDevice, Procedure::getFocalDevice, ProcedureFocalDeviceComponent::hasManipulated,
					ProcedureFocalDeviceComponent::setManipulated);
			cleanUnsupportedReferences(procedure, "Procedure.usedReference", Procedure::hasUsedReference,
					Procedure::setUsedReference);
		}
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
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
				updateUrl = updateUrl + "&effective=" + ms.getEffectiveDateTimeType().getValueAsString();

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
