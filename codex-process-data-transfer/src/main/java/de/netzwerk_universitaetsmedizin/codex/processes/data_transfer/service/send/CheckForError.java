package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_CODE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_DIC;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNKNOWN;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_TYPE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_VALIDATION_ERROR;

import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ContinueStatus;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.constants.CodeSystems;
import dev.dsf.bpe.v1.variables.Variables;

public class CheckForError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CheckForError.class);

	private final String resourceVersion;

	public CheckForError(ProcessPluginApi api, String resourceVersion)
	{
		super(api);

		this.resourceVersion = resourceVersion;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(resourceVersion, "resourceVersion");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Task task = variables.getLatestTask();

		// continue OK
		if (taskHasProfile(task, PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND + "|" + resourceVersion))
		{
			execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, ContinueStatus.SUCCESS);
			updateContinueTask(task);
			variables.updateTask(task);
		}

		// continue Validation ERROR
		else if (taskHasProfile(task,
				PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_VALIDATION_ERROR + "|" + resourceVersion))
		{
			execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, ContinueStatus.VALIDATION_ERROR);
			updateContinueTask(task);
			variables.updateTask(task);
		}

		// continue ERROR
		else if (taskHasProfile(task, PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_ERROR + "|" + resourceVersion))
		{
			Optional<ParameterComponent> errorInputParameter = getErrorInputParameter(task);
			String errorCode = getErrorCode(errorInputParameter)
					.orElse(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNKNOWN);
			String errorMessage = getErrorMessage(errorInputParameter).orElse("Unknown Error");
			String errorSource = getErrorSource(errorInputParameter)
					.orElse(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_DIC);

			execution.setVariable(BPMN_EXECUTION_VARIABLE_ERROR_CODE, errorCode);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE, errorMessage);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_ERROR_SOURCE, errorSource);

			execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, ContinueStatus.ERROR);
			updateContinueTask(task);
			variables.updateTask(task);
		}

		// Timeout
		else
			execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, ContinueStatus.TIMEOUT);
	}

	private void updateContinueTask(Task task)
	{
		try
		{
			task.setStatus(TaskStatus.COMPLETED);
			api.getFhirWebserviceClientProvider().getLocalWebserviceClient().update(task);
		}
		catch (Exception e)
		{
			logger.warn("Unable to update continue Task from DTS", e);
		}
	}

	private boolean taskHasProfile(Task task, String profile)
	{
		return task.getMeta().getProfile().stream().anyMatch(p -> profile.equals(p.getValue()));
	}

	private Optional<String> getErrorCode(Optional<ParameterComponent> errorInputParameter)
	{
		return errorInputParameter.map(i -> i.getExtensionByUrl(EXTENSION_ERROR_METADATA))
				.map(e -> e.getExtensionByUrl(EXTENSION_ERROR_METADATA_TYPE)).map(e -> e.getValue())
				.map(v -> (Coding) v).map(Coding::getCode);
	}

	private Optional<String> getErrorSource(Optional<ParameterComponent> errorInputParameter)
	{
		return errorInputParameter.map(i -> i.getExtensionByUrl(EXTENSION_ERROR_METADATA))
				.map(e -> e.getExtensionByUrl(EXTENSION_ERROR_METADATA_SOURCE)).map(e -> e.getValue())
				.map(v -> (Coding) v).map(Coding::getCode);
	}

	private Optional<String> getErrorMessage(Optional<ParameterComponent> errorInputParameter)
	{
		return errorInputParameter.filter(ParameterComponent::hasValue).filter(i -> i.getValue() instanceof StringType)
				.map(i -> (StringType) i.getValue()).map(StringType::getValue);
	}

	private Optional<ParameterComponent> getErrorInputParameter(Task task)
	{
		return api.getTaskHelper().getFirstInputParameterWithExtension(task, CodeSystems.BpmnMessage.error(),
				StringType.class, EXTENSION_ERROR_METADATA);
	}
}
