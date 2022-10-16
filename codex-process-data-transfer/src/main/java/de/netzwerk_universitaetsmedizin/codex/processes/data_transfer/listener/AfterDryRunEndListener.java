package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.listener;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_LOCAL_VALIDATION_SUCCESSFUL;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Objects;

import javax.activation.DataHandler;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition;

public class AfterDryRunEndListener implements ExecutionListener, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AfterDryRunEndListener.class);

	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final TaskHelper taskHelper;
	private final FhirWebserviceClientProvider clientProvider;
	private final FhirContext fhirContext;
	private final MailService mailService;

	private final String localOrganizationIdentifierValue;
	private final boolean sendDryRunSuccessMail;

	public AfterDryRunEndListener(TaskHelper taskHelper, FhirWebserviceClientProvider clientProvider,
			FhirContext fhirContext, MailService mailService, String localOrganizationIdentifierValue,
			boolean sendDryRunSuccessMail)
	{
		this.taskHelper = taskHelper;
		this.clientProvider = clientProvider;
		this.fhirContext = fhirContext;
		this.mailService = mailService;

		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
		this.sendDryRunSuccessMail = sendDryRunSuccessMail;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(taskHelper, "taskHelper");
		Objects.requireNonNull(clientProvider, "clientProvider");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(mailService, "mailService");

		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
	}

	@Override
	public void notify(DelegateExecution execution) throws Exception
	{
		if (!sendDryRunSuccessMail)
			return;

		Task task = taskHelper.getLeadingTaskFromExecutionVariables(execution);
		Task finalTask = clientProvider.getLocalWebserviceClient().read(Task.class, task.getIdElement().getIdPart());

		if (!TaskStatus.COMPLETED.equals(finalTask.getStatus()))
		{
			logger.warn("Final Task from DSF FHIR server not in status {} but {}, not sending dry-run success mail",
					TaskStatus.COMPLETED, finalTask.getStatus());
			return;
		}

		if (!isLocalValidationSuccessful(finalTask))
		{
			logger.warn(
					"Final Task from DSF FHIR server missing '{}' output parameter with value 'true', not sending dry-run success mail",
					CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_LOCAL_VALIDATION_SUCCESSFUL);
			return;
		}

		String finalTaskAsXml = fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(finalTask);
		String attachmentFilename = finalTask.getIdElement().getIdPart() + ".xml";

		MimeBodyPart text = new MimeBodyPart();
		text.setText(createMesssage(finalTask, attachmentFilename));

		MimeBodyPart attachment = new MimeBodyPart();
		attachment.setFileName(attachmentFilename);
		attachment.setDataHandler(new DataHandler(
				new ByteArrayDataSource(finalTaskAsXml.getBytes(StandardCharsets.UTF_8), "application/xml")));

		MimeMultipart body = new MimeMultipart();
		body.addBodyPart(text);
		body.addBodyPart(attachment);

		MimeBodyPart message = new MimeBodyPart();
		message.setContent(body);

		mailService.send("Dry-Run Success: " + localOrganizationIdentifierValue, message);
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

	private String createMesssage(Task finalTask, String attachmentFileName)
	{
		StringBuilder b = new StringBuilder();

		b.append("Send process version ");
		b.append(DataTransferProcessPluginDefinition.VERSION);
		b.append(" dry-run at organization with identifier '");
		b.append(localOrganizationIdentifierValue);
		b.append("' successfuly completed on ");
		b.append(DATE_FORMAT.format(finalTask.getMeta().getLastUpdated()));
		b.append(".\n\nTask ressource is attached as ");
		b.append(attachmentFileName);

		return b.toString();
	}
}
