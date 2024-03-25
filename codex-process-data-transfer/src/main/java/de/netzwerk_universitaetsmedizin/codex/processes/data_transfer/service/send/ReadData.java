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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
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
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.DiagnosisComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationPerformerComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationProtocolAppliedComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationReactionComponent;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Medication.MedicationIngredientComponent;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationAdministration.MedicationAdministrationPerformerComponent;
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
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class ReadData extends AbstractServiceDelegate
{
	private static final String NUM_CODEX_STRUCTURE_DEFINITION_PREFIX = "https://www.netzwerk-universitaetsmedizin.de";
	private static final String MII_STRUCTURED_DEFINITION_PREFIX = "https://www.medizininformatik-initiative.de";

	// private static final String NUM_CODEX_DO_NOT_RESUSCITAT_ORDER =
	// "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/do-not-resuscitate-order";

	private static final Logger logger = LoggerFactory.getLogger(ReadData.class);

	private final DataStoreClientFactory dataStoreClientFactory;
	private final DataLogger dataLogger;

	public ReadData(ProcessPluginApi api, DataStoreClientFactory dataStoreClientFactory, DataLogger dataLogger)
	{
		super(api);

		this.dataStoreClientFactory = dataStoreClientFactory;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dataStoreClientFactory, "dataStoreClientFactory");
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		String pseudonym = getPseudonym(execution)
				.orElseThrow(() -> new IllegalStateException("Patient reference does not contain identifier"));

		Task task = variables.getStartTask();
		DateTimeType exportFrom = getExportFrom(task).orElse(null);
		InstantType exportTo = getExportTo(task).orElse(new InstantType(new Date()));

		Bundle bundle = readDataAndCreateBundle(pseudonym, exportFrom, exportTo);
		variables.setResource(BPMN_EXECUTION_VARIABLE_BUNDLE, bundle);
	}

	protected Bundle readDataAndCreateBundle(String pseudonym, DateTimeType from, InstantType to)
	{
		logger.info("Reading data for DIC pseudonym {}", pseudonym);

		Stream<DomainResource> resources = dataStoreClientFactory.getDataStoreClient().getFhirClient().getNewData(
				pseudonym, from == null ? null : new DateWithPrecision(from.getValue(), from.getPrecision()),
				to.getValue());

		Bundle bundle = toBundle(pseudonym, resources);

		dataLogger.logData("Created bundle", bundle);

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
		return api.getTaskHelper().getFirstInputParameterValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM, DateTimeType.class);
	}

	private Optional<InstantType> getExportTo(Task task)
	{
		return api.getTaskHelper().getFirstInputParameterValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO, InstantType.class);
	}

	protected Bundle toBundle(String pseudonym, Stream<DomainResource> resourcesStream)
	{
		Bundle b = new Bundle();
		b.setType(BundleType.TRANSACTION);

		List<DomainResource> resources = resourcesStream.collect(Collectors.toList());

		Map<String, DomainResource> resourcesById = resources.stream().collect(
				Collectors.toMap(r -> r.getIdElement().toUnqualifiedVersionless().getValue(), Function.identity()));
		Map<String, String> uuidsById = resources.stream()
				.collect(Collectors.toMap(r -> r.getIdElement().toUnqualifiedVersionless().getValue(),
						r -> "urn:uuid:" + UUID.randomUUID().toString()));


		List<DomainResource> resourcesWithTemporaryReferences = fixReferences(resources, resourcesById, uuidsById);
		List<BundleEntryComponent> entries = resourcesWithTemporaryReferences.stream().map(r ->
		{
			BundleEntryComponent entry = b.addEntry();

			// storing original resource reference for validation error tracking
			entry.setUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT, getAbsoluteId(r));

			entry.setFullUrl(uuidsById.get(r.getIdElement().toUnqualifiedVersionless().getValue()).toString());
			entry.getRequest().setMethod(HTTPVerb.POST).setUrl(r.getResourceType().name());
			entry.setResource(setSubjectOrIdentifier(clean(r), pseudonym));

			return entry;
		}).collect(Collectors.toList());

		b.setEntry(entries);
		return b;
	}

	private List<DomainResource> fixReferences(List<DomainResource> resources,
			Map<String, DomainResource> resourcesById, Map<String, String> uuidsById)
	{
		return resources.stream().map(r -> fixReferences(r, resourcesById, uuidsById)).collect(Collectors.toList());
	}

	// not fixing subject reference here
	private DomainResource fixReferences(DomainResource resource, Map<String, DomainResource> resourcesById,
			Map<String, String> uuidsById)
	{
		if (resource instanceof Encounter e)
		{
			fixReferenceFromComponents(e, uuidsById, "Encounter.diagnosis[{}].condition", Encounter::hasDiagnosis,
					Encounter::getDiagnosis, DiagnosisComponent::hasCondition, DiagnosisComponent::getCondition,
					DiagnosisComponent::setCondition);
			fixReference(e, uuidsById, "Encounter.partOf", Encounter::hasPartOf, Encounter::getPartOf,
					Encounter::setPartOf);
		}
		else if (resource instanceof Condition c)
		{
			fixReference(c, uuidsById, "Condition.encounter", Condition::hasEncounter, Condition::getEncounter,
					Condition::setEncounter);
		}
		else if (resource instanceof MedicationAdministration ma)
		{
			fixReference(ma, uuidsById, "MedicationAdministration.context", MedicationAdministration::hasContext,
					MedicationAdministration::getContext, MedicationAdministration::setContext);
			fixReference(ma, uuidsById, "MedicationAdministration.medicationReference",
					MedicationAdministration::hasMedicationReference, MedicationAdministration::getMedicationReference,
					MedicationAdministration::setMedication);
		}
		else if (resource instanceof MedicationStatement ms)
		{
			fixReference(ms, uuidsById, "MedicationStatement.context", MedicationStatement::hasContext,
					MedicationStatement::getContext, MedicationStatement::setContext);
			fixReference(ms, uuidsById, "MedicationStatement.medicationReference",
					MedicationStatement::hasMedicationReference, MedicationStatement::getMedicationReference,
					MedicationStatement::setMedication);
		}
		else if (resource instanceof Observation o)
		{
			fixReference(o, uuidsById, "Observation.encounter", Observation::hasEncounter, Observation::getEncounter,
					Observation::setEncounter);
			fixReferences(o, uuidsById, "Observation.hasMember", Observation::hasHasMember, Observation::getHasMember,
					Observation::setHasMember);
			fixReference(o, uuidsById, "Observation.specimen", Observation::hasSpecimen, Observation::getSpecimen,
					Observation::setSpecimen);
		}
		else if (resource instanceof Procedure p)
		{
			fixReference(p, uuidsById, "Procedure.encounter", Procedure::hasEncounter, Procedure::getEncounter,
					Procedure::setEncounter);
		}
		else if (resource instanceof Specimen s)
		{
			fixReferences(s, uuidsById, "Specimen.parent", Specimen::hasParent, Specimen::getParent,
					Specimen::setParent);
		}


		return resource;
	}

	private <R extends DomainResource> R fixReference(R resource, Map<String, String> uuidsById, String path,
			Function<R, Boolean> hasReference, Function<R, Reference> getReference,
			BiFunction<R, Reference, R> setReference)
	{
		if (hasReference.apply(resource))
		{
			Reference oldReference = getReference.apply(resource);

			if (uuidsById.containsKey(oldReference.getReference()))
			{
				logger.debug("Replacing reference at {} from resource {} with bundle temporary id in transport bundle",
						path, getAbsoluteId(resource).getValue());
				setReference.apply(resource, new Reference(uuidsById.get(oldReference.getReference())));
			}
			else if (oldReference.hasReference() && oldReference.getReference() == null && oldReference
					.getReferenceElement_().hasExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason"))
			{
				logger.debug("Not removing empty reference at {} with data-absent-reason extension", path);
			}
			else if (!oldReference.getResource().isEmpty())
			{
				String internalId = "#" + UUID.randomUUID();
				Reference fixedReference = new Reference(internalId);
				IBaseResource oldContainedResource = clean((DomainResource) oldReference.getResource());
				oldContainedResource.setId(internalId);
				fixedReference.setResource(oldContainedResource);
				setReference.apply(resource, fixedReference);
				logger.debug(
						"Replacing reference to contained resource at {} from resource {} with bundle temporary id in transport bundle",
						path, getAbsoluteId(resource).getValue());
			}
			else
			{
				logger.warn("Removing reference at {} from resource {} in transport bundle", path,
						getAbsoluteId(resource).getValue());
				setReference.apply(resource, null);
			}
		}

		return resource;
	}

	private <R extends DomainResource> R fixReferences(R resource, Map<String, String> uuidsById, String path,
			Function<R, Boolean> hasReference, Function<R, List<Reference>> getReferences,
			BiFunction<R, List<Reference>, R> setReferences)
	{
		if (hasReference.apply(resource))
		{
			List<Reference> oldReferences = getReferences.apply(resource);
			List<Reference> fixedReferences = new ArrayList<>();

			for (int i = 0; i < oldReferences.size(); i++)
			{
				Reference oldReference = oldReferences.get(i);

				if (uuidsById.containsKey(oldReference.getReference()))
				{
					logger.debug(
							"Replacing reference at {}[{}] from resource {} with bundle temporary id in transport bundle",
							path, i, getAbsoluteId(resource).getValue());
					fixedReferences.add(new Reference(uuidsById.get(oldReference.getReference())));
				}
				else if (oldReference.hasReference() && oldReference.getReference() == null
						&& oldReference.getReferenceElement_()
								.hasExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason"))
				{
					logger.debug("Not removing empty reference at {}[{}] with data-absent-reason extension", path, i);
					fixedReferences.add(oldReference);
				}
				else if (!oldReference.getResource().isEmpty())
				{
					String internalId = "#" + UUID.randomUUID();
					Reference fixedReference = new Reference(internalId);
					IBaseResource oldContainedResource = clean((DomainResource) oldReference.getResource());
					oldContainedResource.setId(internalId);
					fixedReference.setResource(oldContainedResource);
					fixedReferences.add(fixedReference);
					logger.debug(
							"Replacing reference to contained resource at {}[{}] from resource {} with bundle temporary id in transport bundle",
							path, i, getAbsoluteId(resource).getValue());
				}
				else
				{
					logger.warn("Removing reference at {}[{}] from resource {} in transport bundle", path, i,
							getAbsoluteId(resource).getValue());
				}
			}

			setReferences.apply(resource, fixedReferences);
		}

		return resource;
	}

	private <R extends DomainResource, C> R fixReferenceFromComponents(R resource, Map<String, String> uuidsById,
			String path, Function<R, Boolean> hasComponents, Function<R, List<C>> getComponents,
			Function<C, Boolean> hasReference, Function<C, Reference> getReference,
			BiFunction<C, Reference, C> setReference)
	{
		if (hasComponents.apply(resource))
		{
			List<C> components = getComponents.apply(resource);
			for (int i = 0; i < components.size(); i++)
			{
				C component = components.get(i);
				Reference oldReference = getReference.apply(component);

				if (oldReference.hasReference() && oldReference.getReference() != null
						&& uuidsById.containsKey(oldReference.getReference()))
				{
					logger.debug(
							"Replacing reference at {}[{}] from resource {} with bundle temporary id in transport bundle",
							path, i, getAbsoluteId(resource).getValue());
					setReference.apply(component, new Reference(uuidsById.get(oldReference.getReference())));
				}
				else if ((oldReference.hasReference() && oldReference.getReference() == null
						&& oldReference.getReferenceElement_()
								.hasExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason"))
						|| oldReference.hasExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason"))
				{
					logger.debug(
							"Not removing empty reference at {}[{}] with data-absent-reason extension from resource {} in transport bundle",
							path, i, getAbsoluteId(resource).getValue());
				}
				else
				{
					logger.warn("Removing reference at {}[{}] from resource {} in transport bundle", path, i,
							getAbsoluteId(resource).getValue());
					setReference.apply(component, null);
				}
			}
		}

		return resource;
	}

	private IdType getAbsoluteId(DomainResource r)
	{
		if (r == null)
			return null;

		return r.getIdElement().isAbsolute() ? r.getIdElement()
				: r.getIdElement().withServerBase(dataStoreClientFactory.getServerBase(), r.getResourceType().name());
	}

	private DomainResource clean(DomainResource r)
	{
		cleanUnsupportedReferences(r);

		r.setIdElement(null);
		List<CanonicalType> profiles = r.getMeta().getProfile().stream().filter(CanonicalType::hasValue)
				.filter(p -> p.getValue().startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX)
						|| p.getValue().startsWith(MII_STRUCTURED_DEFINITION_PREFIX))
				.collect(Collectors.toList());
		r.setMeta(new Meta().setProfile(profiles));
		r.setText(null);

		return r;
	}

	private <R extends DomainResource> void cleanUnsupportedReference(R resource, String path,
			Function<R, Boolean> hasReference, BiFunction<R, Reference, R> setReference)
	{
		if (hasReference.apply(resource))
		{
			logger.warn("Removing reference at {} from resource {} in transport bundle", path,
					getAbsoluteId(resource).getValue());
			setReference.apply(resource, (Reference) null);
		}
	}

	private <R extends DomainResource> void cleanUnsupportedReferences(R resource, String path,
			Function<R, Boolean> hasReferences, BiFunction<R, List<Reference>, R> setReferences)
	{
		if (hasReferences.apply(resource))
		{
			logger.warn("Removing reference at {} from resource {} in transport bundle", path,
					getAbsoluteId(resource).getValue());
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
					logger.warn("Removing reference at {}[{}] from resource {} in transport bundle", path, i,
							getAbsoluteId(resource).getValue());
					setReference.apply(component, null);
				}
			}
		}
	}

	private <R extends DomainResource, C> void cleanUnsupportedReferenceFromComponent(R resource, String path,
			Function<R, Boolean> hasComponents, Function<R, C> getComponents, Function<C, Boolean> hasReference,
			BiFunction<C, Reference, C> setReference)
	{
		if (hasComponents.apply(resource))
		{
			C component = getComponents.apply(resource);
			if (hasReference.apply(component))
			{
				logger.warn("Removing reference at {} from resource {} in transport bundle", path,
						getAbsoluteId(resource).getValue());
				setReference.apply(component, null);
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
					logger.warn("Removing references at {}[{}] from resource {} in transport bundle", path, i,
							getAbsoluteId(resource).getValue());
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
						logger.warn("Removing reference at {}[{}] from resource {} in transport bundle", path, i,
								getAbsoluteId(resource).getValue());
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
		else if (resource instanceof Patient p)
		{
			cleanUnsupportedReferenceFromComponents(p, "Patient.contact[{}].organization", Patient::hasContact,
					Patient::getContact, ContactComponent::hasOrganization, ContactComponent::setOrganization);
			cleanUnsupportedReferences(p, "Patient.generalPractitioner", Patient::hasGeneralPractitioner,
					Patient::setGeneralPractitioner);
			cleanUnsupportedReference(p, "Patient.managingOrganization", Patient::hasManagingOrganization,
					Patient::setManagingOrganization);
			cleanUnsupportedReferenceFromComponents(p, "Patient.link[{}].other", Patient::hasLink, Patient::getLink,
					PatientLinkComponent::hasOther, PatientLinkComponent::setOther);
		}
		else if (resource instanceof Condition c)
		{
			cleanUnsupportedReference(c, "Condition.recorder", Condition::hasRecorder, Condition::setRecorder);
			cleanUnsupportedReference(c, "Condition.asserter", Condition::hasAsserter, Condition::setAsserter);
			cleanUnsupportedReferencesFromComponents(c, "Condition.stage[{}].assessment", Condition::hasStage,
					Condition::getStage, ConditionStageComponent::hasAssessment,
					ConditionStageComponent::setAssessment);
			cleanUnsupportedReferencesFromComponents(c, "Condition.evidence[{}].detail", Condition::hasEvidence,
					Condition::getEvidence, ConditionEvidenceComponent::hasDetail,
					ConditionEvidenceComponent::setDetail);
		}
		else if (resource instanceof Consent c)
		{
			cleanUnsupportedReferences(c, "Consent.performer", Consent::hasPerformer, Consent::setPerformer);
			cleanUnsupportedReferences(c, "Consent.organization", Consent::hasOrganization, Consent::setOrganization);
			cleanUnsupportedReferenceFromComponents(c, "Consent.verification[{}].verifiedWith",
					Consent::hasVerification, Consent::getVerification, ConsentVerificationComponent::hasVerifiedWith,
					ConsentVerificationComponent::setVerifiedWith);
			// provisionComponent and provisionActorComponent class names starts with lower case p
			cleanUnsupportedReferencesFromComponentComponents(c, "Consent.provision.actor[{}].reference",
					Consent::hasProvision, Consent::getProvision, provisionComponent::hasActor,
					provisionComponent::getActor, provisionActorComponent::hasReference,
					provisionActorComponent::setReference);
			// provisionComponent and provisionDataComponent class names starts with lower case p
			cleanUnsupportedReferencesFromComponentComponents(c, "Consent.provision.data[{}].reference",
					Consent::hasProvision, Consent::getProvision, provisionComponent::hasData,
					provisionComponent::getData, provisionDataComponent::hasReference,
					provisionDataComponent::setReference);
		}
		else if (resource instanceof DiagnosticReport dr)
		{
			cleanUnsupportedReferences(dr, "DiagnosticReport.basedOn", DiagnosticReport::hasBasedOn,
					DiagnosticReport::setBasedOn);
			cleanUnsupportedReferences(dr, "DiagnosticReport.performer", DiagnosticReport::hasPerformer,
					DiagnosticReport::setPerformer);
			cleanUnsupportedReferences(dr, "DiagnosticReport.resultsInterpreter",
					DiagnosticReport::hasResultsInterpreter, DiagnosticReport::setResultsInterpreter);
			cleanUnsupportedReferences(dr, "DiagnosticReport.specimen", DiagnosticReport::hasSpecimen,
					DiagnosticReport::setSpecimen);
			cleanUnsupportedReferences(dr, "DiagnosticReport.result", DiagnosticReport::hasResult,
					DiagnosticReport::setResult);
			cleanUnsupportedReferences(dr, "DiagnosticReport.imagingStudy", DiagnosticReport::hasImagingStudy,
					DiagnosticReport::setImagingStudy);
			cleanUnsupportedReferenceFromComponents(dr, "DiagnosticReport.media[{}].link", DiagnosticReport::hasMedia,
					DiagnosticReport::getMedia, DiagnosticReportMediaComponent::hasLink,
					DiagnosticReportMediaComponent::setLink);
		}
		else if (resource instanceof Encounter e)
		{
			cleanUnsupportedReferences(e, "Encounter.episodeOfCare", Encounter::hasEpisodeOfCare,
					Encounter::setEpisodeOfCare);
			cleanUnsupportedReferences(e, "Encounter.basedOn", Encounter::hasBasedOn, Encounter::setBasedOn);
			cleanUnsupportedReferenceFromComponents(e, "Encounter.participant[{}].individual",
					Encounter::hasParticipant, Encounter::getParticipant, EncounterParticipantComponent::hasIndividual,
					EncounterParticipantComponent::setIndividual);
			cleanUnsupportedReferences(e, "Encounter.appointment", Encounter::hasAppointment,
					Encounter::setAppointment);
			cleanUnsupportedReferences(e, "Encounter.reasonReference", Encounter::hasReasonReference,
					Encounter::setReasonReference);
			cleanUnsupportedReferences(e, "Encounter.account", Encounter::hasAccount, Encounter::setAccount);
			cleanUnsupportedReferenceFromComponent(e, "Encounter.hospitalization.origin", Encounter::hasHospitalization,
					Encounter::getHospitalization, EncounterHospitalizationComponent::hasOrigin,
					EncounterHospitalizationComponent::setOrigin);
			cleanUnsupportedReferenceFromComponent(e, "Encounter.hospitalization.destination",
					Encounter::hasHospitalization, Encounter::getHospitalization,
					EncounterHospitalizationComponent::hasDestination,
					EncounterHospitalizationComponent::setDestination);
			cleanUnsupportedReferenceFromComponents(e, "Encounter.location[{}].location", Encounter::hasLocation,
					Encounter::getLocation, EncounterLocationComponent::hasLocation,
					EncounterLocationComponent::setLocation);
			cleanUnsupportedReference(e, "Encounter.serviceProvider", Encounter::hasServiceProvider,
					Encounter::setServiceProvider);
		}
		else if (resource instanceof Immunization i)
		{
			cleanUnsupportedReference(i, "Immunization.encounter", Immunization::hasEncounter,
					Immunization::setEncounter);
			cleanUnsupportedReference(i, "Immunization.location", Immunization::hasLocation, Immunization::setLocation);
			cleanUnsupportedReference(i, "Immunization.manufacturer", Immunization::hasManufacturer,
					Immunization::setManufacturer);
			cleanUnsupportedReferenceFromComponents(i, "Immunization.performer[{}].actor", Immunization::hasPerformer,
					Immunization::getPerformer, ImmunizationPerformerComponent::hasActor,
					ImmunizationPerformerComponent::setActor);
			cleanUnsupportedReferences(i, "Immunization.reasonReference", Immunization::hasReasonReference,
					Immunization::setReasonReference);
			cleanUnsupportedReferenceFromComponents(i, "Immunization.reaction[{}].detail", Immunization::hasReaction,
					Immunization::getReaction, ImmunizationReactionComponent::hasDetail,
					ImmunizationReactionComponent::setDetail);
			cleanUnsupportedReferenceFromComponents(i, "Immunization.protocolApplied[{}].authority",
					Immunization::hasProtocolApplied, Immunization::getProtocolApplied,
					ImmunizationProtocolAppliedComponent::hasAuthority,
					ImmunizationProtocolAppliedComponent::setAuthority);
		}
		else if (resource instanceof Medication m)
		{
			cleanUnsupportedReference(m, "Medication.manufacturer", Medication::hasManufacturer,
					Medication::setManufacturer);
			cleanUnsupportedReferenceFromComponents(m, "Medication.ingredient[{}].itemReference",
					Medication::hasIngredient, Medication::getIngredient,
					MedicationIngredientComponent::hasItemReference, MedicationIngredientComponent::setItem);
		}
		else if (resource instanceof MedicationAdministration ma)
		{
			cleanUnsupportedReferences(ma, "MedicationAdministration.partOf", MedicationAdministration::hasPartOf,
					MedicationAdministration::setPartOf);
			cleanUnsupportedReferences(ma, "MedicationAdministration.supportingInformation",
					MedicationAdministration::hasSupportingInformation,
					MedicationAdministration::setSupportingInformation);
			cleanUnsupportedReferenceFromComponents(ma, "MedicationAdministration.performer[{}].performer",
					MedicationAdministration::hasPerformer, MedicationAdministration::getPerformer,
					MedicationAdministrationPerformerComponent::hasActor,
					MedicationAdministrationPerformerComponent::setActor);
			cleanUnsupportedReferences(ma, "MedicationAdministration.reasonReference",
					MedicationAdministration::hasReasonReference, MedicationAdministration::setReasonReference);
			cleanUnsupportedReference(ma, "MedicationAdministration.request", MedicationAdministration::hasRequest,
					MedicationAdministration::setRequest);
			cleanUnsupportedReferences(ma, "MedicationAdministration.device", MedicationAdministration::hasDevice,
					MedicationAdministration::setDevice);
			cleanUnsupportedReferences(ma, "MedicationAdministration.eventHistory",
					MedicationAdministration::hasEventHistory, MedicationAdministration::setEventHistory);
		}
		else if (resource instanceof MedicationStatement ms)
		{
			cleanUnsupportedReferences(ms, "MedicationStatement.basedOn", MedicationStatement::hasBasedOn,
					MedicationStatement::setBasedOn);
			cleanUnsupportedReferences(ms, "MedicationStatement.partOf", MedicationStatement::hasPartOf,
					MedicationStatement::setPartOf);
			cleanUnsupportedReference(ms, "MedicationStatement.informationSource",
					MedicationStatement::hasInformationSource, MedicationStatement::setInformationSource);
			cleanUnsupportedReferences(ms, "MedicationStatement.derivedFrom", MedicationStatement::hasDerivedFrom,
					MedicationStatement::setDerivedFrom);
			cleanUnsupportedReferences(ms, "MedicationStatement.reasonReference",
					MedicationStatement::hasReasonReference, MedicationStatement::setReasonReference);
		}
		else if (resource instanceof Observation o)
		{
			cleanUnsupportedReferences(o, "Observation.basedOn", Observation::hasBasedOn, Observation::setBasedOn);
			cleanUnsupportedReferences(o, "Observation.partOf", Observation::hasPartOf, Observation::setPartOf);
			cleanUnsupportedReferences(o, "Observation.focus", Observation::hasFocus, Observation::setFocus);
			cleanUnsupportedReferences(o, "Observation.performer", Observation::hasPerformer,
					Observation::setPerformer);
			cleanUnsupportedReference(o, "Observation.device", Observation::hasDevice, Observation::setDevice);
			cleanUnsupportedReferences(o, "Observation.derivedFrom", Observation::hasDerivedFrom,
					Observation::setDerivedFrom);
		}
		else if (resource instanceof Procedure p)
		{
			cleanUnsupportedReferences(p, "Procedure.basedOn", Procedure::hasBasedOn, Procedure::setBasedOn);
			cleanUnsupportedReferences(p, "Procedure.partOf", Procedure::hasPartOf, Procedure::setPartOf);
			cleanUnsupportedReference(p, "Procedure.recorder", Procedure::hasRecorder, Procedure::setRecorder);
			cleanUnsupportedReference(p, "Procedure.asserter", Procedure::hasAsserter, Procedure::setAsserter);
			cleanUnsupportedReferenceFromComponents(p, "Procedure.performer[{}].actor", Procedure::hasPerformer,
					Procedure::getPerformer, ProcedurePerformerComponent::hasActor,
					ProcedurePerformerComponent::setActor);
			cleanUnsupportedReferenceFromComponents(p, "Procedure.performer[{}].onBehalfOf", Procedure::hasPerformer,
					Procedure::getPerformer, ProcedurePerformerComponent::hasOnBehalfOf,
					ProcedurePerformerComponent::setOnBehalfOf);
			cleanUnsupportedReference(p, "Procedure.location", Procedure::hasLocation, Procedure::setLocation);
			cleanUnsupportedReferences(p, "Procedure.reasonReference", Procedure::hasReasonReference,
					Procedure::setReasonReference);
			cleanUnsupportedReferences(p, "Procedure.report", Procedure::hasReport, Procedure::setReport);
			cleanUnsupportedReferences(p, "Procedure.complicationDetail", Procedure::hasComplicationDetail,
					Procedure::setComplicationDetail);
			cleanUnsupportedReferenceFromComponents(p, "Procedure.focalDevice[{}].manipulated",
					Procedure::hasFocalDevice, Procedure::getFocalDevice, ProcedureFocalDeviceComponent::hasManipulated,
					ProcedureFocalDeviceComponent::setManipulated);
			cleanUnsupportedReferences(p, "Procedure.usedReference", Procedure::hasUsedReference,
					Procedure::setUsedReference);
		}
		else if (resource instanceof Specimen s)
		{
			cleanUnsupportedReferences(s, "Specimen.request", Specimen::hasRequest, Specimen::setRequest);
			cleanUnsupportedReferenceFromComponent(s, "Specimen.collection.collector", Specimen::hasCollection,
					Specimen::getCollection, Specimen.SpecimenCollectionComponent::hasCollector,
					Specimen.SpecimenCollectionComponent::setCollector);
			cleanUnsupportedReferencesFromComponents(s, "Specimen.processing[{}].additive", Specimen::hasProcessing,
					Specimen::getProcessing, Specimen.SpecimenProcessingComponent::hasAdditive,
					Specimen.SpecimenProcessingComponent::setAdditive);
			cleanUnsupportedReferenceFromComponents(s, "Specimen.container[{}].additiveReference",
					Specimen::hasContainer, Specimen::getContainer,
					Specimen.SpecimenContainerComponent::hasAdditiveReference,
					Specimen.SpecimenContainerComponent::setAdditive);
		}
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
	}

	private Resource setSubjectOrIdentifier(Resource resource, String pseudonym)
	{
		Identifier identifier = new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym);
		identifier.getType().getCodingFirstRep().setSystem(IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_SYSTEM)
				.setCode(IDENTIFIER_NUM_CODEX_DIC_PSEUDONYM_TYPE_CODE);

		if (resource instanceof Patient p)
		{
			p.setIdentifier(Collections.singletonList(identifier));
		}
		else if (resource instanceof Medication m)
		{
			m.setIdentifier(Collections.emptyList());
		}
		else
		{
			Reference patientRef = new Reference().setIdentifier(identifier);

			if (resource instanceof Condition c)
			{
				c.setIdentifier(Collections.emptyList());
				c.setSubject(patientRef);
			}
			else if (resource instanceof Consent c)
			{
				c.setIdentifier(Collections.emptyList());
				c.setPatient(patientRef);
			}
			else if (resource instanceof DiagnosticReport dr)
			{
				dr.setIdentifier(Collections.emptyList());
				dr.setSubject(patientRef);
			}
			else if (resource instanceof Encounter e)
			{
				e.setIdentifier(Collections.emptyList());
				e.setSubject(patientRef);
			}
			else if (resource instanceof Immunization i)
			{
				i.setIdentifier(Collections.emptyList());
				i.setPatient(patientRef);
			}
			else if (resource instanceof MedicationAdministration ma)
			{
				ma.setIdentifier(Collections.emptyList());
				ma.setSubject(patientRef);
			}
			else if (resource instanceof MedicationStatement ms)
			{
				ms.setIdentifier(Collections.emptyList());
				ms.setSubject(patientRef);
			}
			else if (resource instanceof Observation o)
			{
				o.setIdentifier(Collections.emptyList());
				o.setSubject(patientRef);
			}
			else if (resource instanceof Procedure p)
			{
				p.setIdentifier(Collections.emptyList());
				p.setSubject(patientRef);
			}
			else if (resource instanceof Specimen s)
			{
				s.setIdentifier(Collections.emptyList());
				s.setAccessionIdentifier(null);
				s.setSubject(patientRef);
			}
			else
				throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
		}

		if (resource instanceof DomainResource d)
			d.getContained().forEach(r -> setSubjectOrIdentifier(r, pseudonym));
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");

		return resource;
	}

	// protected String getConditionalUpdateUrl(String pseudonym, DomainResource resource)
	// {
	// String patientIdentifier = ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM + "|" + pseudonym;
	//
	// if (resource instanceof Patient)
	// {
	// return "Patient?identifier=" + patientIdentifier;
	// }
	// else if (resource instanceof Condition c)
	// {
	// String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));
	//
	// String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Condition.name(), profileUrl,
	// patientIdentifier);
	//
	// if (c.hasRecordedDateElement() && c.getRecordedDateElement().getValueAsString() != null)
	// updateUrl = updateUrl + "&recorded-date=" + c.getRecordedDateElement().getValueAsString();
	//
	// if (c.hasCategory() && c.getCategoryFirstRep().hasCoding())
	// updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(c.getCategoryFirstRep().getCodingFirstRep());
	//
	// if (c.hasCode() && c.getCode().hasCoding())
	// updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(c.getCode().getCodingFirstRep());
	//
	// if (c.hasBodySite() && c.getBodySiteFirstRep().hasCoding())
	// updateUrl = updateUrl + "&body-site=" + getCodingUpdateUrl(c.getBodySiteFirstRep().getCodingFirstRep());
	//
	// return updateUrl;
	// }
	// else if (resource instanceof Consent c)
	// {
	// String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));
	//
	// if (NUM_CODEX_DO_NOT_RESUSCITAT_ORDER.equals(profileUrl))
	// {
	// boolean scopePresent = c.getScope().getCoding().stream().filter(Coding::hasSystem)
	// .filter(co -> "http://terminology.hl7.org/CodeSystem/consentscope".equals(co.getSystem()))
	// .filter(Coding::hasCode).anyMatch(co -> "adr".equals(co.getCode()));
	// boolean categoryPresent = c.getCategory().stream().flatMap(coc -> coc.getCoding().stream())
	// .filter(Coding::hasSystem)
	// .filter(co -> "http://terminology.hl7.org/CodeSystem/consentcategorycodes"
	// .equals(co.getSystem()))
	// .filter(Coding::hasCode).anyMatch(co -> "dnr".equals(co.getCode()));
	//
	// if (scopePresent && categoryPresent)
	// return getBaseConditionalUpdateUrl(ResourceType.Consent.name(), profileUrl, patientIdentifier)
	// + "&scope=http://terminology.hl7.org/CodeSystem/consentscope|adr"
	// + "&category=http://terminology.hl7.org/CodeSystem/consentcategorycodes|dnr";
	//
	// else
	// throw new RuntimeException("Resource of type Consent with profile " + profileUrl
	// + " is missing scope: http://terminology.hl7.org/CodeSystem/consentscope|adr and/or category:
	// http://terminology.hl7.org/CodeSystem/consentcategorycodes|dnr");
	// }
	// else
	// throw new RuntimeException("Resource of type Consent with profile " + profileUrl + " not supported");
	// }
	// else if (resource instanceof DiagnosticReport dr)
	// {
	// String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));
	//
	// String updateUrl = getBaseConditionalUpdateUrl(ResourceType.DiagnosticReport.name(), profileUrl,
	// patientIdentifier);
	//
	// if (dr.hasEffectiveDateTimeType() && dr.getEffectiveDateTimeType().getValueAsString() != null)
	// updateUrl = updateUrl + "&date=" + dr.getEffectiveDateTimeType().getValueAsString();
	//
	// if (dr.hasCategory() && dr.getCategoryFirstRep().hasCoding())
	// updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(dr.getCategoryFirstRep().getCodingFirstRep());
	//
	// if (dr.hasCode() && dr.getCode().hasCoding())
	// updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(dr.getCode().getCodingFirstRep());
	//
	// return updateUrl;
	// }
	// else if (resource instanceof Immunization i)
	// {
	// String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));
	//
	// String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Immunization.name(), profileUrl,
	// patientIdentifier);
	//
	// if (i.hasOccurrenceDateTimeType() && i.getOccurrenceDateTimeType().getValueAsString() != null)
	// updateUrl = updateUrl + "&date=" + i.getOccurrenceDateTimeType().getValueAsString();
	//
	// if (i.hasVaccineCode() && i.getVaccineCode().hasCoding())
	// updateUrl = updateUrl + "&vaccine-code=" + i.getVaccineCode().getCodingFirstRep();
	//
	// return updateUrl;
	// }
	// else if (resource instanceof MedicationStatement ms)
	// {
	// String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));
	//
	// String updateUrl = getBaseConditionalUpdateUrl(ResourceType.MedicationStatement.name(), profileUrl,
	// patientIdentifier);
	//
	// if (ms.hasEffectiveDateTimeType() && ms.getEffectiveDateTimeType().getValueAsString() != null)
	// updateUrl = updateUrl + "&effective=" + ms.getEffectiveDateTimeType().getValueAsString();
	//
	// if (ms.hasMedicationCodeableConcept() && ms.getMedicationCodeableConcept().hasCoding())
	// updateUrl = updateUrl + "&code="
	// + getCodingUpdateUrl(ms.getMedicationCodeableConcept().getCodingFirstRep());
	//
	// return updateUrl;
	// }
	// else if (resource instanceof Observation o)
	// {
	// String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX)
	// || MII_LAB_STRUCTURED_DEFINITION.equals(v));
	//
	// String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Observation.name(), profileUrl,
	// patientIdentifier);
	//
	// if (o.hasEffectiveDateTimeType() && o.getEffectiveDateTimeType().getValueAsString() != null)
	// updateUrl = updateUrl + "&date=" + o.getEffectiveDateTimeType().getValueAsString();
	//
	// if (o.hasCategory() && o.getCategoryFirstRep().hasCoding())
	// updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(o.getCategoryFirstRep().getCodingFirstRep());
	//
	// if (o.hasCode() && o.getCode().hasCoding())
	// updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(o.getCode().getCodingFirstRep());
	//
	// return updateUrl;
	// }
	// else if (resource instanceof Procedure p)
	// {
	// String profileUrl = getProfileUrl(resource, v -> v.startsWith(NUM_CODEX_STRUCTURE_DEFINITION_PREFIX));
	//
	// String updateUrl = getBaseConditionalUpdateUrl(ResourceType.Procedure.name(), profileUrl,
	// patientIdentifier);
	//
	// if (p.hasPerformedDateTimeType() && p.getPerformedDateTimeType().getValueAsString() != null)
	// updateUrl = updateUrl + "&date=" + p.getPerformedDateTimeType().getValueAsString();
	//
	// if (p.hasCategory() && p.getCategory().hasCoding())
	// updateUrl = updateUrl + "&category=" + getCodingUpdateUrl(p.getCategory().getCodingFirstRep());
	//
	// if (p.hasCode() && p.getCode().hasCoding())
	// updateUrl = updateUrl + "&code=" + getCodingUpdateUrl(p.getCode().getCodingFirstRep());
	//
	// return updateUrl;
	//
	// }
	// else
	// throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
	// }
	//
	// private String getProfileUrl(DomainResource resource, Predicate<String> filter)
	// {
	// return resource.getMeta().getProfile().stream().map(CanonicalType::getValue).filter(filter).findFirst()
	// .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getResourceType().name()
	// + " not supported, missing NUM or MII profile"));
	// }
	//
	// private String getBaseConditionalUpdateUrl(String resourceName, String profileUrl, String patientIdentifier)
	// {
	// return resourceName + "?_profile=" + profileUrl + "&patient:identifier=" + patientIdentifier;
	// }
	//
	// private String getCodingUpdateUrl(Coding coding)
	// {
	// return coding.getSystem() + "|" + coding.getCode();
	// }
}
