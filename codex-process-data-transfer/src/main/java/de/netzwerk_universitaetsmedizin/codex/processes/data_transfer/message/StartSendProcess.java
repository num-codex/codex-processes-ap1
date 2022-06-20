package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;

public class StartSendProcess extends AbstractTaskMessageSend
{
	private static final Logger logger = LoggerFactory.getLogger(StartSendProcess.class);

	public StartSendProcess(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected void sendTask(Target target, String instantiatesUri, String messageName, String businessKey,
			String profile, Stream<ParameterComponent> additionalInputParameters)
	{
		// can't use same business key as trigger process
		super.sendTask(target, instantiatesUri, messageName, UUID.randomUUID().toString(), profile,
				additionalInputParameters);
	}

	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		return Stream.of(referenceParameter(execution), exportFromParameter(execution), exportToParameter(execution))
				.filter(Objects::nonNull);
	}

	private ParameterComponent referenceParameter(DelegateExecution execution)
	{
		PatientReference patientReference = (PatientReference) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE);

		if (patientReference.hasAbsoluteReference())
			return getAbsoluteReferenceParameter(patientReference.getAbsoluteReference());
		else if (patientReference.hasIdentifier())
			return getIdentifierReferenceParameter(patientReference.getIdentifier());
		else
			throw new IllegalStateException("Patient reference does not contain identifier or absolute reference");
	}

	private Task.ParameterComponent getAbsoluteReferenceParameter(String absoluteReference)
	{
		Task.ParameterComponent param = new Task.ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT);
		param.setValue(new Reference(absoluteReference));

		return param;
	}

	private Task.ParameterComponent getIdentifierReferenceParameter(Identifier identifier)
	{
		Task.ParameterComponent param = new Task.ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT);
		param.setValue(new Reference().setIdentifier(identifier).setType(ResourceType.Patient.name()));

		return param;
	}

	private ParameterComponent exportFromParameter(DelegateExecution execution)
	{
		Date exportFrom = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM);
		String exportFromPrecisionStr = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION);
		TemporalPrecisionEnum exportFromPrecision = exportFromPrecisionStr == null ? null
				: TemporalPrecisionEnum.valueOf(exportFromPrecisionStr);

		if (exportFrom != null && exportFromPrecision != null)
		{
			ParameterComponent param = new ParameterComponent();
			param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
					.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM);
			param.setValue(new DateTimeType(exportFrom, exportFromPrecision));
			return param;
		}
		else
		{
			logger.warn("Export from not specified, export from date unbounded");
			return null;
		}
	}

	private ParameterComponent exportToParameter(DelegateExecution execution)
	{
		Date exportTo = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO);

		ParameterComponent param = new ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);
		param.setValue(new InstantType(exportTo));
		return param;
	}
}
