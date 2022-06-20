package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition.VERSION;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ContinueStatus;

public class CheckForError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CheckForError.class);

	public CheckForError(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		ContinueStatus continueStatus;

		// continue OK
		if (currentTaskHasProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE + "|" + VERSION))
			continueStatus = ContinueStatus.SUCCESS;

		// continue Validation ERROR
		else if (currentTaskHasProfile(
				PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR + "|" + VERSION))
			continueStatus = ContinueStatus.VALIDATION_ERROR;

		// continue ERROR / Timeout
		else
			continueStatus = ContinueStatus.VALIDATION_ERROR;

		execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, continueStatus);

		try
		{
			Task continueTask = getCurrentTaskFromExecutionVariables();
			continueTask.setStatus(TaskStatus.COMPLETED);
			getFhirWebserviceClientProvider().getLocalWebserviceClient().update(continueTask);
		}
		catch (Exception e)
		{
			logger.warn("Unable to update continue Task from CRR", e);
		}
	}

	private boolean currentTaskHasProfile(String profile)
	{
		Task currentTask = getCurrentTaskFromExecutionVariables();
		return currentTask.getMeta().getProfile().stream().anyMatch(p -> profile.equals(p.getValue()));
	}
}
