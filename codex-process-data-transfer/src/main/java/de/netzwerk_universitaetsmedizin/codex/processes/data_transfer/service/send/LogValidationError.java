package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_SOURCE_IDS_BY_BUNDLE_UUID;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;

public class LogValidationError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogValidationError.class);

	private final ErrorOutputParameterGenerator errorOutputParameterGenerator;

	public LogValidationError(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, ErrorOutputParameterGenerator errorOutputParameterGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.errorOutputParameterGenerator = errorOutputParameterGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(errorOutputParameterGenerator, "errorOutputParameterGenerator");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);

		@SuppressWarnings("unchecked")
		Map<String, String> sourceIdsByBundleUuid = (Map<String, String>) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_SOURCE_IDS_BY_BUNDLE_UUID);

		addErrorsToTask(bundle, sourceIdsByBundleUuid);
	}

	private void addErrorsToTask(Bundle bundle, Map<String, String> sourceIdsByBundleUuid)
	{
		logger.debug("Setting Task.status failed, adding validation errors");

		Task task = getLeadingTaskFromExecutionVariables();
		task.setStatus(TaskStatus.FAILED);

		bundle.getEntry().stream()
				.filter(e -> e.hasResponse() && e.getResponse().hasOutcome()
						&& (e.getResponse().getOutcome() instanceof OperationOutcome)
						&& ((OperationOutcome) e.getResponse().getOutcome()).getIssue().stream()
								.anyMatch(i -> IssueSeverity.FATAL.equals(i.getSeverity())
										|| IssueSeverity.ERROR.equals(i.getSeverity())))
				.forEach(entry ->
				{
					IdType sourceId = Optional.ofNullable(sourceIdsByBundleUuid.get(entry.getFullUrl()))
							.map(IdType::new).orElse(null);
					OperationOutcome outcome = (OperationOutcome) entry.getResponse().getOutcome();

					errorOutputParameterGenerator.createCrrValidationError(sourceId, outcome).forEach(task::addOutput);
				});
	}
}
