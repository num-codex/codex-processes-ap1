package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_CODE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_RETURN_TARGET;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR;

import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorInputParameterGenerator;

public class ContinueTranslateProcessWithError extends AbstractTaskMessageSend
{
	private final ErrorInputParameterGenerator errorInputParameterGenerator;

	public ContinueTranslateProcessWithError(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext,
			ErrorInputParameterGenerator errorInputParameterGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);

		this.errorInputParameterGenerator = errorInputParameterGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(errorInputParameterGenerator, "errorInputParameterGenerator");
	}

	@Override
	protected Target getTarget(DelegateExecution execution)
	{
		return (Target) execution.getVariable(BPMN_EXECUTION_VARIABLE_RETURN_TARGET);
	}

	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		String errorCode = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_CODE);
		String errorMessage = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE);
		String errorSource = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_SOURCE);

		return Stream.of(errorInputParameterGenerator.createError(errorCode, errorMessage,
				errorSource != null ? errorSource : CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR));
	}
}
