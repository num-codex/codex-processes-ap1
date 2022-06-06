package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_TYPE_VALUE_VALIDATION_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.HAPI_USER_DATA_SOURCE_ID_ELEMENT;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.ErrorLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidatorFactory;

public class ValidateData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ValidateData.class);

	private final BundleValidatorFactory bundleValidatorSupplier;
	private final ErrorOutputParameterGenerator errorOutputParameterGenerator;
	private final ErrorLogger errorLogger;

	public ValidateData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, BundleValidatorFactory bundleValidatorSupplier,
			ErrorOutputParameterGenerator errorOutputParameterGenerator, ErrorLogger errorLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.bundleValidatorSupplier = bundleValidatorSupplier;
		this.errorOutputParameterGenerator = errorOutputParameterGenerator;
		this.errorLogger = errorLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(bundleValidatorSupplier, "bundleValidatorSupplier");
		Objects.requireNonNull(errorOutputParameterGenerator, "errorOutputParameterGenerator");
		Objects.requireNonNull(errorLogger, "errorLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		bundleValidatorSupplier.create().ifPresentOrElse(validator ->
		{
			Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);

			logger.info("Validating bundle with {} entr{}", bundle.getEntry().size(),
					bundle.getEntry().size() == 1 ? "y" : "ies");

			bundle = validator.validate(bundle);

			if (bundle.hasEntry())
			{
				if (bundle.getEntry().stream().anyMatch(e -> !e.hasResponse() || !e.getResponse().hasOutcome()
						|| !(e.getResponse().getOutcome() instanceof OperationOutcome)))
				{
					logger.warn(
							"Validation result bundle has entries wihout response.outcome instance of OperationOutcome");
				}
				else
				{
					logValidationDetails(bundle);

					if (bundle.getEntry().stream().map(e -> (OperationOutcome) e.getResponse().getOutcome())
							.flatMap(o -> o.getIssue().stream())
							.anyMatch(i -> IssueSeverity.FATAL.equals(i.getSeverity())
									|| IssueSeverity.ERROR.equals(i.getSeverity())))
					{
						logger.error("Validation of transfer bundle failed");

						addErrorsToTaskAndSetFailed(bundle);
						errorLogger.logValidationFailed(getLeadingTaskFromExecutionVariables().getIdElement()
								.withServerBase(getFhirWebserviceClientProvider().getLocalBaseUrl(),
										getLeadingTaskFromExecutionVariables().getIdElement().getResourceType()));

						throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_TYPE_VALUE_VALIDATION_FAILED);
					}
					else
					{
						removeValidationResultsAndUserData(bundle);
					}
				}
			}
			else
			{
				logger.warn("Validation result bundle has no entries");
			}
		}, () ->
		{
			logger.warn(
					"{} not initialized, skipping validation. This is likley due to an error during startup of the process plugin.",
					BundleValidatorFactory.class.getSimpleName());
		});

		// TODO maybe check only one pseudonym used
	}

	private void logValidationDetails(Bundle bundle)
	{
		bundle.getEntry().stream().filter(e -> e.hasResponse() && e.getResponse().hasOutcome()
				&& (e.getResponse().getOutcome() instanceof OperationOutcome)).forEach(entry ->
				{
					IdType sourceId = (IdType) entry.getUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT);
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
							"Validation error for {}{}: {}", sourceId.getValue(), i.getLocation().stream()
									.map(StringType::getValue).findFirst().map(l -> " location " + l).orElse(""),
							i.getDiagnostics());
					break;
				case WARNING:
					logger.warn(
							"Validation warning for {}{}: {}", sourceId.getValue(), i.getLocation().stream()
									.map(StringType::getValue).findFirst().map(l -> " location " + l).orElse(""),
							i.getDiagnostics());
					break;
				case INFORMATION:
				case NULL:
				default:
					logger.info(
							"Validation info for {}{}: {}", sourceId.getValue(), i.getLocation().stream()
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

	private void addErrorsToTaskAndSetFailed(Bundle bundle)
	{
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
					IdType sourceId = (IdType) entry.getUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT);
					OperationOutcome outcome = (OperationOutcome) entry.getResponse().getOutcome();

					errorOutputParameterGenerator.createMeDicValidationError(sourceId, outcome)
							.forEach(task::addOutput);
				});
	}

	private void removeValidationResultsAndUserData(Bundle bundle)
	{
		bundle.getEntry().stream().forEach(e ->
		{
			e.clearUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT);
			e.setResponse(null);
		});
		execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, FhirResourceValues.create(bundle));
	}
}
