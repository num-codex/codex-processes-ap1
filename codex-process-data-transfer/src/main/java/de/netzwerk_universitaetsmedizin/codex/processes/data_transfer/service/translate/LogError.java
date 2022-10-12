package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_CODE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.ErrorLogger;

public class LogError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogError.class);

	private final ErrorOutputParameterGenerator errorOutputParameterGenerator;
	private final ErrorLogger errorLogger;

	public LogError(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, ErrorOutputParameterGenerator errorOutputParameterGenerator,
			ErrorLogger errorLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.errorOutputParameterGenerator = errorOutputParameterGenerator;
		this.errorLogger = errorLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(errorOutputParameterGenerator, "errorOutputParameterGenerator");
		Objects.requireNonNull(errorLogger, "errorLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		logger.debug("Setting Task.status failed, adding error");

		Task task = getLeadingTaskFromExecutionVariables();

		String errorCode = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_CODE);
		String errorMessage = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE);
		String errorSource = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_SOURCE);

		// only fail if local error
		if (errorSource == null || CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH.equals(errorSource))
			task.setStatus(TaskStatus.FAILED);

		if (!CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED.equals(errorCode))
		{
			errorSource = errorSource != null ? errorSource : CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH;
			TaskOutputComponent output = errorOutputParameterGenerator.createError(errorSource, errorCode,
					errorMessage);

			task.addOutput(output);

			if (CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH.equals(errorSource))
				logger.error("Error while executing local process; code: '{}', message: {}", errorCode, errorMessage);
			else // not an error if error source remote
				logger.warn("Error while executing process at {}; code: '{}', message: {}", errorSource, errorCode,
						errorMessage);

			errorLogger.logDataTranslateFailed(getLeadingTaskFromExecutionVariables().getIdElement()
					.withServerBase(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Task.name()));
		}

		updateLeadingTaskInExecutionVariables(task);
	}
}
