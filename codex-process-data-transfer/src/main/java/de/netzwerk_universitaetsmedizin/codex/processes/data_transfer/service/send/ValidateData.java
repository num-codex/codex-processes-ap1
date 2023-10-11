package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_SOURCE_IDS_BY_BUNDLE_UUID;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_ENCRYPTED_BUNDLE_RESOURCES_COUNT;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_LOCAL_VALIDATION_SUCCESSFUL;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.HAPI_USER_DATA_SOURCE_ID_ELEMENT;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.ErrorLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidatorFactory;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class ValidateData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ValidateData.class);

	private final BundleValidatorFactory bundleValidatorSupplier;
	private final ErrorOutputParameterGenerator errorOutputParameterGenerator;
	private final ErrorLogger errorLogger;

	public ValidateData(ProcessPluginApi api, BundleValidatorFactory bundleValidatorSupplier,
			ErrorOutputParameterGenerator errorOutputParameterGenerator, ErrorLogger errorLogger)
	{
		super(api);

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
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Task task = variables.getStartTask();

		if (!bundleValidatorSupplier.isEnabled())
		{
			logger.warn("Validation disabled, skipping validation. Modify configuration to enable validation");

			Bundle bundle = variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE);

			Map<String, String> sourceIdsByBundleUuid = removeValidationResultsCollectSourceIdsIntoMap(variables,
					bundle);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_SOURCE_IDS_BY_BUNDLE_UUID, sourceIdsByBundleUuid);

			addValidationStatusAndBundleEntryCountToTask(task, false, bundle);

			return;
		}

		bundleValidatorSupplier.create().ifPresentOrElse(validator ->
		{
			Bundle bundle = variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE);

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

					addValidationStatusAndBundleEntryCountToTask(task, false, bundle);
				}
				else
				{
					logValidationDetails(bundle);

					long resourcesWithErrorCount = bundle.getEntry().stream().filter(BundleEntryComponent::hasResponse)
							.map(BundleEntryComponent::getResponse).filter(BundleEntryResponseComponent::hasOutcome)
							.map(BundleEntryResponseComponent::getOutcome).filter(r -> r instanceof OperationOutcome)
							.map(o -> (OperationOutcome) o).map(
									o -> o.getIssue().stream()
											.anyMatch(i -> IssueSeverity.FATAL.equals(i.getSeverity())
													|| IssueSeverity.ERROR.equals(i.getSeverity())))
							.filter(b -> b).count();

					addValidationStatusAndBundleEntryCountToTask(task, resourcesWithErrorCount <= 0, bundle);

					if (resourcesWithErrorCount > 0)
					{
						logger.error("Validation of transfer bundle failed, {} resource{} with error",
								resourcesWithErrorCount, resourcesWithErrorCount != 1 ? "s" : "");

						addErrorsToTask(task, bundle);
						errorLogger.logValidationFailedLocal(task.getIdElement().withServerBase(
								api.getFhirWebserviceClientProvider().getLocalWebserviceClient().getBaseUrl(),
								ResourceType.Task.name()));

						throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED,
								"Validation of transfer bundle failed, " + resourcesWithErrorCount + " resource"
										+ (resourcesWithErrorCount != 1 ? "s" : "") + " with error");
					}
					else
					{
						Map<String, String> sourceIdsByBundleUuid = removeValidationResultsCollectSourceIdsIntoMap(
								variables, bundle);
						execution.setVariable(BPMN_EXECUTION_VARIABLE_SOURCE_IDS_BY_BUNDLE_UUID, sourceIdsByBundleUuid);
					}
				}
			}
			else
			{
				logger.warn("Validation result bundle has no entries");
				addValidationStatusAndBundleEntryCountToTask(task, false, bundle);
			}
		}, () ->
		{
			logger.warn(
					"{} not initialized, skipping validation. This is likely due to an error during startup of the process plugin",
					BundleValidatorFactory.class.getSimpleName());
			addValidationStatusAndBundleEntryCountToTask(task, false,
					variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE));
		});
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

	private void addValidationStatusAndBundleEntryCountToTask(Task task, boolean validationSuccessful,
			Bundle transferBundle)
	{
		task.addOutput().setValue(new BooleanType(validationSuccessful)).getType().getCodingFirstRep()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_LOCAL_VALIDATION_SUCCESSFUL);
		task.addOutput().setValue(new UnsignedIntType(transferBundle.getEntry().size())).getType().getCodingFirstRep()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_ENCRYPTED_BUNDLE_RESOURCES_COUNT);
		// TODO
		// updateLeadingTaskInExecutionVariables(execution, task);
	}

	private void addErrorsToTask(Task task, Bundle validationBundle)
	{
		validationBundle.getEntry().stream()
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
		// TODO
		// updateLeadingTaskInExecutionVariables(execution, task);
	}

	private Map<String, String> removeValidationResultsCollectSourceIdsIntoMap(Variables variables, Bundle bundle)
	{
		Map<String, String> sourceIdByBundleUuid = new HashMap<>();
		bundle.getEntry().stream().forEach(e ->
		{
			IdType sourceId = (IdType) e.getUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT);
			sourceIdByBundleUuid.put(e.getFullUrl(), sourceId.getValue());

			e.clearUserData(HAPI_USER_DATA_SOURCE_ID_ELEMENT);
			e.setResponse(null);
		});
		variables.setResource(BPMN_EXECUTION_VARIABLE_BUNDLE, bundle);

		return sourceIdByBundleUuid;
	}
}
