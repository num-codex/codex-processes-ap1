package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_CODE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNKNOWN;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_TYPE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition.VERSION;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR;

import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
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
		{
			continueStatus = ContinueStatus.SUCCESS;
			updateContinueTask();
		}

		// continue Validation ERROR
		else if (currentTaskHasProfile(
				PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR + "|" + VERSION))
		{
			continueStatus = ContinueStatus.VALIDATION_ERROR;
			updateContinueTask();
		}

		// continue ERROR
		else if (currentTaskHasProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_ERROR + "|" + VERSION))
		{
			Task continueWithErrorTask = getCurrentTaskFromExecutionVariables();

			String errorCode = getErrorCode(continueWithErrorTask)
					.orElse(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNKNOWN);
			String errorMessage = getErrorMessage(continueWithErrorTask).orElse("Unknown Error");
			String errorSource = getErrorSource(continueWithErrorTask)
					.orElse(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH);

			execution.setVariable(BPMN_EXECUTION_VARIABLE_ERROR_CODE, errorCode);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE, errorMessage);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_ERROR_SOURCE, errorSource);

			continueStatus = ContinueStatus.ERROR;
			updateContinueTask();
		}

		// Timeout
		else
			continueStatus = ContinueStatus.TIMEOUT;

		execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, continueStatus);
	}

	private void updateContinueTask()
	{
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

	private Optional<String> getErrorCode(Task task)
	{
		return getErrorInputParameter(task).map(i -> i.getExtensionByUrl(EXTENSION_ERROR_METADATA))
				.map(e -> e.getExtensionByUrl(EXTENSION_ERROR_METADATA_TYPE)).map(e -> e.getValue())
				.map(v -> (Coding) v).map(Coding::getCode);
	}

	private Optional<String> getErrorSource(Task task)
	{
		return getErrorInputParameter(task).map(i -> i.getExtensionByUrl(EXTENSION_ERROR_METADATA))
				.map(e -> e.getExtensionByUrl(EXTENSION_ERROR_METADATA_SOURCE)).map(e -> e.getValue())
				.map(v -> (Coding) v).map(Coding::getCode);
	}

	private Optional<String> getErrorMessage(Task task)
	{
		return getErrorInputParameter(task).filter(ParameterComponent::hasValue)
				.filter(i -> i.getValue() instanceof StringType).map(i -> (StringType) i.getValue())
				.map(StringType::getValue);
	}

	private Optional<ParameterComponent> getErrorInputParameter(Task task)
	{
		if (task != null && task.hasInput())
		{
			return getTaskHelper().getInputParameterWithExtension(task, CODESYSTEM_HIGHMED_BPMN,
					CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR, EXTENSION_ERROR_METADATA).findFirst();
		}
		else
			return Optional.empty();
	}
}
