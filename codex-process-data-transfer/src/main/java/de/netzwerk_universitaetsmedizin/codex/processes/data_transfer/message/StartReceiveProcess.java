package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_CRR_NOT_REACHABLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;

import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;

public class StartReceiveProcess extends AbstractTaskMessageSend
{
	private static final Logger logger = LoggerFactory.getLogger(StartReceiveProcess.class);

	public StartReceiveProcess(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		return Stream.of(pseudonymParameter(execution), dataReferenceParameter(execution));
	}

	@Override
	protected void sendTask(DelegateExecution execution, Target target, String instantiatesUri, String messageName,
			String businessKey, String profile, Stream<ParameterComponent> additionalInputParameters)
	{
		String crrBusinessKey = createAndSaveAlternativeBusinessKey(execution);

		logger.debug("DIC businessKey {}, CRR businessKey {}", businessKey, crrBusinessKey);

		super.sendTask(execution, target, instantiatesUri, messageName, crrBusinessKey, profile,
				additionalInputParameters);
	}

	private ParameterComponent pseudonymParameter(DelegateExecution execution)
	{
		String pseudonym = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM);

		Objects.requireNonNull(pseudonym, "pseudonym");

		ParameterComponent param = new ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM);
		param.setValue(new Identifier().setSystem(ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM)
				.setValue(pseudonym));
		return param;
	}

	private ParameterComponent dataReferenceParameter(DelegateExecution execution)
	{
		String binaryReference = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_BINARY_URL);

		Objects.requireNonNull(binaryReference, "binaryReference");

		ParameterComponent param = new ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE);
		param.setValue(new Reference().setReference(binaryReference));
		return param;
	}

	@Override
	protected void handleSendTaskError(DelegateExecution execution, Exception exception, String errorMessage)
	{
		throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_CRR_NOT_REACHABLE,
				"Error while sending Task to CRR: " + exception.getMessage());
	}
}
