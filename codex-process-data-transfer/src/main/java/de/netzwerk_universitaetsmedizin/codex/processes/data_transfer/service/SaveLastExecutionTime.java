package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Type;

public class SaveLastExecutionTime extends AbstractServiceDelegate
{
	public SaveLastExecutionTime(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper)
	{
		super(clientProvider, taskHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();

		Optional<InstantType> exportToOutput = getExportToOutput(task);

		Date exportTo = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO);

		if (exportToOutput.isPresent())
			exportToOutput.get().setValue(exportTo);
		else
			task.addOutput(exportToParameter(exportTo));

		getFhirWebserviceClientProvider().getLocalWebserviceClient().update(task);
	}

	private TaskOutputComponent exportToParameter(Date exportTo)
	{
		TaskOutputComponent param = new TaskOutputComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);
		param.setValue(new InstantType(exportTo));
		return param;
	}

	private Optional<InstantType> getExportToOutput(Task task)
	{
		return getOutputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO, InstantType.class).findFirst();
	}

	private <T extends Type> Stream<T> getOutputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getOutput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}
}
