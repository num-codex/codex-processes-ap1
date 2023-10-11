package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DRY_RUN;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DRY_RUN;

import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class CheckDryRun extends AbstractServiceDelegate
{
	public CheckDryRun(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Task task = variables.getStartTask();

		Optional<Boolean> dryRun = api.getTaskHelper().getFirstInputParameterValue(task,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER, CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DRY_RUN, BooleanType.class)
				.map(BooleanType::getValue);

		variables.setBoolean(BPMN_EXECUTION_VARIABLE_DRY_RUN, dryRun.orElse(Boolean.FALSE));
	}
}
