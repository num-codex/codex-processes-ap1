package de.netzwerk_universitaetsmedizin.codex.processes.fhir.profile;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_GECCO_DATA_FROM_DIC_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_GECCO_DATA_FROM_GTH_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DRY_RUN;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_ENCRYPTED_BUNDLE_SIZE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_ERROR_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_VALIDATION_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_VALIDATION_ERROR_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_ERROR_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_DATA_RECEIVE_PROCESS_URI_AND_LATEST_VERSION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI_AND_LATEST_VERSION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI_AND_LATEST_VERSION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_RECEIVE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_RECEIVE_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_SEND;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_SEND_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER_MESSAGE_NAME;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition.DATE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition.VERSION;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.highmed.dsf.fhir.validation.ResourceValidator;
import org.highmed.dsf.fhir.validation.ResourceValidatorImpl;
import org.highmed.dsf.fhir.validation.ValidationSupportRule;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorInputParameterGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;

public class TaskProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(TaskProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(VERSION, DATE,
			Arrays.asList("highmed-task-base-0.5.0.xml", "num-codex-extension-error-metadata.xml",
					"num-codex-task-start-data-trigger.xml", "num-codex-task-stop-data-trigger.xml",
					"num-codex-task-start-data-send.xml", "num-codex-task-continue-data-send.xml",
					"num-codex-task-continue-data-send-with-validation-error.xml",
					"num-codex-task-continue-data-send-with-error.xml", "num-codex-task-start-data-translate.xml",
					"num-codex-task-continue-data-translate.xml",
					"num-codex-task-continue-data-translate-with-validation-error.xml",
					"num-codex-task-continue-data-translate-with-error.xml", "num-codex-task-start-data-receive.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml",
					"num-codex-data-transfer.xml", "num-codex-data-transfer-error-source.xml",
					"num-codex-data-transfer-error.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml",
					"num-codex-data-transfer.xml", "num-codex-data-transfer-error-source.xml",
					"num-codex-data-transfer-error.xml"));

	private ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	private void logTask(Task task)
	{
		logger.debug("task: {}",
				validationRule.getFhirContext().newXmlParser().setPrettyPrint(true).encodeResourceToString(task));
	}

	@Test
	public void testTaskStartDataTriggerValid() throws Exception
	{
		Task task = createValidTaskStartDataTrigger();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataTriggerValidWithExportFrom() throws Exception
	{
		Task task = createValidTaskStartDataTrigger();
		task.addInput().setValue(new DateTimeType(new Date())).getType().addCoding()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM);
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataTriggerWithOutputValid() throws Exception
	{
		Task task = createValidTaskStartDataTrigger();
		task.addOutput().setValue(new InstantType(new Date())).getType().addCoding()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStartDataTrigger()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		return task;
	}

	@Test
	public void testTaskStopDataTriggerValid() throws Exception
	{
		Task task = createValidTaskStopDataTrigger();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStopDataTrigger()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		return task;
	}

	@Test
	public void testTaskStartDataSendValidWithIdentifierReference() throws Exception
	{
		Task task = createValidTaskStartDataSendWithIdentifierReference();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataSendValidWithBusinessKey() throws Exception
	{
		Task task = createValidTaskStartDataSendWithIdentifierReference();
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataSendValidWithExportFrom() throws Exception
	{
		Task task = createValidTaskStartDataSendWithIdentifierReference();
		task.addInput().setValue(new DateTimeType(new Date(), TemporalPrecisionEnum.DAY)).getType().addCoding()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM);
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataSendValidWithValidationError() throws Exception
	{
		Task task = createValidTaskStartDataSendWithIdentifierReference();
		task.setStatus(TaskStatus.FAILED);

		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue().setSeverity(IssueSeverity.ERROR).addLocation("Patient.identifier[0].system");
		new ErrorOutputParameterGenerator()
				.createMeDicValidationError(new IdType("http://gecco.fhir.server/fhir", "Patient", "42", null), outcome)
				.forEach(task::addOutput);

		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStartDataSendWithIdentifierReference()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_START_DATA_SEND);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_START_DATA_SEND_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		// business-key optional
		// task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
		// .setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput()
				.setValue(new Reference().setIdentifier(
						new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue("source/original")))
				.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT);
		task.addInput().setValue(new InstantType(new Date())).getType().addCoding()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);

		return task;
	}

	@Test
	public void testTaskStartDataSendValidWithAbsoluteReference() throws Exception
	{
		Task task = createValidTaskStartDataSendWithAbsoluteReference();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStartDataSendWithAbsoluteReference()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_START_DATA_SEND);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_START_DATA_SEND_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		// business-key optional
		// task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
		// .setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput().setValue(new Reference("https://localhost/fhir/Patient/1")).getType().addCoding()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT);
		task.addInput().setValue(new InstantType(new Date())).getType().addCoding()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);

		return task;
	}

	@Test
	public void testTaskStartDataSendDryRun() throws Exception
	{
		Task task = createValidTaskStartDataSendWithIdentifierReference();
		task.addInput().setValue(new BooleanType(true)).getType().addCoding()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DRY_RUN);
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataTranslateValid() throws Exception
	{
		Task task = createValidTaskStartDataTranslate();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataTranslateValidWithEncryptedBundleSizeOutput() throws Exception
	{
		Task task = createValidTaskStartDataTranslate();

		task.addOutput().setValue(new IntegerType(12345678)).getType().getCodingFirstRep()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_ENCRYPTED_BUNDLE_SIZE);

		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataTranslateNotValidWithEncryptedBundleSizeOutput() throws Exception
	{
		Task task = createValidTaskStartDataTranslate();

		task.addOutput().setValue(new StringType("12345678")).getType().getCodingFirstRep()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_ENCRYPTED_BUNDLE_SIZE);

		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(1, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataTranslateValidWithError() throws Exception
	{
		Task task = createValidTaskStartDataTranslate();
		task.setStatus(TaskStatus.FAILED);

		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue().setSeverity(IssueSeverity.ERROR).addLocation("Patient.identifier[0].system");
		new ErrorOutputParameterGenerator().createError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_VALIDATION_FAILED,
				"Validation failed while inserting into CRR");

		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStartDataTranslate()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput()
				.setValue(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue("source/original"))
				.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM);
		task.addInput().setValue(new Reference(new IdType("https://dic", "Binary", UUID.randomUUID().toString(), "1")))
				.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE);

		return task;
	}

	@Test
	public void testTaskStartDataReceiveValid() throws Exception
	{
		Task task = createValidTaskStartDataReceive();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataReceiveValidWithValidationError() throws Exception
	{
		Task task = createValidTaskStartDataReceive();

		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue().setSeverity(IssueSeverity.ERROR).addLocation("Patient.identifier[0].system");
		new ErrorOutputParameterGenerator().createCrrValidationError(outcome).forEach(task::addOutput);

		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStartDataReceive()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_START_DATA_RECEIVE);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_RECEIVE_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_CRR");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_START_DATA_RECEIVE_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput()
				.setValue(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM).setValue("target/original"))
				.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM);
		task.addInput().setValue(new Reference(new IdType("https://gth", "Binary", UUID.randomUUID().toString(), "1")))
				.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE);

		return task;
	}

	@Test
	public void testTaskContinueDataSend() throws Exception
	{
		Task task = createValidTaskContinueDataSend();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskContinueDataSend()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		return task;
	}

	@Test
	public void testTaskContinueDataSendWithValidationError() throws Exception
	{
		Task task = createValidTaskContinueDataSendWithValidationError();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskContinueDataSendWithValidationError()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_VALIDATION_ERROR);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput()
				.setValue(new StringType(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_VALIDATION_ERROR_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput().setValue(new Reference(new IdType("https://gth", "Binary", UUID.randomUUID().toString(), "1")))
				.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE);

		return task;
	}

	@Test
	public void testTaskContinueDataSendWithError() throws Exception
	{
		Task task = createValidTaskContinueDataSendWithError();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskContinueDataSendWithError()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_ERROR);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_SEND_WITH_ERROR_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		ParameterComponent error = new ErrorInputParameterGenerator().createGthError(
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_GECCO_DATA_FROM_DIC_FAILED,
				"Test Error Message");
		task.addInput(error);

		return task;
	}

	@Test
	public void testTaskContinueDataTranslate() throws Exception
	{
		Task task = createValidTaskContinueDataTranslate();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskContinueDataTranslate()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_CRR");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		return task;
	}

	@Test
	public void testTaskContinueDataTranslateWithValidationError() throws Exception
	{
		Task task = createValidTaskContinueDataTranslateWithValidationError();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskContinueDataTranslateWithValidationError()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_CRR");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.addInput()
				.setValue(new StringType(
						PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_VALIDATION_ERROR_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput().setValue(new Reference(new IdType("https://crr", "Binary", UUID.randomUUID().toString(), "1")))
				.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE);

		return task;
	}

	@Test
	public void testTaskContinueDataTranslateWithError() throws Exception
	{
		Task task = createValidTaskContinueDataTranslateWithError();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskContinueDataTranslateWithError()
	{
		Task task = new Task();
		task.getMeta().addProfile(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_ERROR);
		task.setInstantiatesUri(PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_CRR");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_GTH");
		task.addInput().setValue(new StringType(PROFILE_NUM_CODEX_TASK_CONTINUE_DATA_TRANSLATE_WITH_ERROR_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		ParameterComponent error = new ErrorInputParameterGenerator().createCrrError(
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_GECCO_DATA_FROM_GTH_FAILED,
				"Test Error Message");
		task.addInput(error);

		return task;
	}
}
