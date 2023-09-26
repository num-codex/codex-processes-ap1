package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_SOURCE_IDS_BY_BUNDLE_UUID;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.ErrorLogger;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class LogValidationError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogValidationError.class);

	private final ErrorOutputParameterGenerator errorOutputParameterGenerator;
	private final ErrorLogger errorLogger;

	public LogValidationError(ProcessPluginApi api, ErrorOutputParameterGenerator errorOutputParameterGenerator,
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
		Task task = variables.getStartTask();

		logger.warn("Validation error while adding resources to CRR FHIR repository");

		Bundle bundle = variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE);

		@SuppressWarnings("unchecked")
		Map<String, String> sourceIdsByBundleUuid = (Map<String, String>) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_SOURCE_IDS_BY_BUNDLE_UUID);

		errorLogger.logValidationFailedRemote(task.getIdElement().withServerBase(
				api.getFhirWebserviceClientProvider().getLocalWebserviceClient().getBaseUrl(),
				ResourceType.Task.name()));
		logValidationDetails(bundle, sourceIdsByBundleUuid);

		addErrorsToTaskAndSetStatusFailed(task, bundle, sourceIdsByBundleUuid);
		api.getFhirWebserviceClientProvider().getLocalWebserviceClient().update(task);
		variables.updateTask(task);
	}

	private void addErrorsToTaskAndSetStatusFailed(Task task, Bundle bundle, Map<String, String> sourceIdsByBundleUuid)
	{
		logger.debug("Setting Task.status failed, adding validation errors");

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

	private void logValidationDetails(Bundle bundle, Map<String, String> sourceIdsByBundleUuid)
	{
		bundle.getEntry().stream().filter(e -> e.hasResponse() && e.getResponse().hasOutcome()
				&& (e.getResponse().getOutcome() instanceof OperationOutcome)).forEach(entry ->
				{
					IdType sourceId = Optional.ofNullable(sourceIdsByBundleUuid.get(entry.getFullUrl()))
							.map(IdType::new).orElse(null);
					OperationOutcome outcome = (OperationOutcome) entry.getResponse().getOutcome();

					outcome.getIssue().forEach(i -> logValidationDetails(sourceId, i));
				});
	}

	private void logValidationDetails(IdType sourceId, OperationOutcomeIssueComponent i)
	{
		if (i.getSeverity() != null)
		{
			switch (i.getSeverity())
			{
				case FATAL:
				case ERROR:
					logger.error(
							"CRR validation error for {}{}: {}", sourceId.getValue(), i.getLocation().stream()
									.map(StringType::getValue).findFirst().map(l -> " location " + l).orElse(""),
							i.getDiagnostics());
					break;
				case WARNING:
					logger.warn(
							"CRR validation warning for {}{}: {}", sourceId.getValue(), i.getLocation().stream()
									.map(StringType::getValue).findFirst().map(l -> " location " + l).orElse(""),
							i.getDiagnostics());
					break;
				case INFORMATION:
				case NULL:
				default:
					logger.info(
							"CRR validation info for {}{}: {}", sourceId.getValue(), i.getLocation().stream()
									.map(StringType::getValue).findFirst().map(l -> " location " + l).orElse(""),
							i.getDiagnostics());
					break;
			}
		}
		else
		{
			logger.info(
					"Validation info for {}{}: {}", sourceId.getValue(), i.getLocation().stream()
							.map(StringType::getValue).findFirst().map(l -> " location " + l).orElse(""),
					i.getDiagnostics());
		}
	}

}
