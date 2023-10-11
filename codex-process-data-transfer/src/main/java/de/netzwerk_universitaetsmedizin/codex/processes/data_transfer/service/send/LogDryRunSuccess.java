package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_LOCAL_VALIDATION_SUCCESSFUL;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class LogDryRunSuccess extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogSuccess.class);

	public LogDryRunSuccess(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Task task = variables.getStartTask();

		if (isLocalValidationSuccessful(task))
		{
			logger.info("Send process dry-run successfully completed");
		}
		else
		{
			logger.warn("Send process dry-run unsuccessful");

			task.setStatus(TaskStatus.FAILED);
			api.getFhirWebserviceClientProvider().getLocalWebserviceClient().update(task);

			variables.updateTask(task);
		}
	}

	private boolean isLocalValidationSuccessful(Task task)
	{
		return task.getOutput().stream().filter(TaskOutputComponent::hasType).filter(o -> o.getType().hasCoding())
				.filter(o -> o.getType().getCoding().stream()
						.anyMatch(c -> CODESYSTEM_NUM_CODEX_DATA_TRANSFER.equals(c.getSystem())
								&& CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_LOCAL_VALIDATION_SUCCESSFUL
										.equals(c.getCode())))
				.filter(TaskOutputComponent::hasValue).filter(o -> o.getValue() instanceof BooleanType)
				.map(o -> (BooleanType) o.getValue()).anyMatch(b -> Boolean.TRUE.equals(b.getValue()));
	}
}
