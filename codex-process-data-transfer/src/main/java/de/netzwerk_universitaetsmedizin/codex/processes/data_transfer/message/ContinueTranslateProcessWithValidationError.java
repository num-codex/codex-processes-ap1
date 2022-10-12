package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_RETURN_TARGET;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;

import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import ca.uhn.fhir.context.FhirContext;

public class ContinueTranslateProcessWithValidationError extends AbstractTaskMessageSend
{
	public ContinueTranslateProcessWithValidationError(FhirWebserviceClientProvider clientProvider,
			TaskHelper taskHelper, ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
			FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Target getTarget()
	{
		return (Target) getExecution().getVariable(BPMN_EXECUTION_VARIABLE_RETURN_TARGET);
	}

	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		return Stream.of(dataReferenceParameter(execution));
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
}
