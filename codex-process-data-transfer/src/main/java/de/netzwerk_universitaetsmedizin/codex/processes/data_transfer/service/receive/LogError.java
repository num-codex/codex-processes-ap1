package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_CODE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_ERROR_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.ErrorLogger;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class LogError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogError.class);

	private final ErrorOutputParameterGenerator errorOutputParameterGenerator;
	private final ErrorLogger errorLogger;

	public LogError(ProcessPluginApi api, ErrorOutputParameterGenerator errorOutputParameterGenerator,
			ErrorLogger errorLogger)
	{
		super(api);

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
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		logger.debug("Setting Task.status failed, adding error");

		Task task = variables.getStartTask();
		task.setStatus(TaskStatus.FAILED);

		String errorCode = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_CODE);
		String errorMessage = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_MESSAGE);
		String errorSource = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_ERROR_SOURCE);

		if (!CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED.equals(errorCode))
		{
			errorSource = errorSource != null ? errorSource : CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR;
			TaskOutputComponent output = errorOutputParameterGenerator.createError(errorSource, errorCode,
					errorMessage);

			task.addOutput(output);

			if (CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR.equals(errorSource))
				logger.error("Error while executing local process; code: '{}', message: {}", errorCode, errorMessage);
			else
				logger.error("Error while executing process at {}; code: '{}', message: {}", errorSource, errorCode,
						errorMessage);

			errorLogger.logDataReceiveFailed(task.getIdElement().withServerBase(
					api.getFhirWebserviceClientProvider().getLocalWebserviceClient().getBaseUrl(),
					ResourceType.Task.name()));
		}

		variables.updateTask(task);
	}
}
