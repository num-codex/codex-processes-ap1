package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;

import java.util.Date;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class SaveLastExportTo extends AbstractServiceDelegate
{
	public SaveLastExportTo(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Date lastExportTo = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO);

		Task task = variables.getStartTask();
		task.addOutput(exportToParameter(lastExportTo));
		variables.updateTask(task);
	}

	private TaskOutputComponent exportToParameter(Date exportTo)
	{
		TaskOutputComponent param = new TaskOutputComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);
		param.setValue(new InstantType(exportTo));
		return param;
	}
}
