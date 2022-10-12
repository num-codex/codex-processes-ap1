package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging;

import java.util.Objects;

import org.highmed.dsf.bpe.service.MailService;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class ErrorLogger implements InitializingBean
{
	private static final Logger validationLogger = LoggerFactory.getLogger("validation-error-logger");
	private static final Logger errorLogger = LoggerFactory.getLogger("error-logger");

	private final MailService mailService;

	private final boolean sendValidationFailedMail;
	private final boolean sendProcessFailedMail;

	public ErrorLogger(MailService mailService, boolean sendValidationFailedMail, boolean sendProcessFailedMail)
	{
		this.mailService = mailService;

		this.sendValidationFailedMail = sendValidationFailedMail;
		this.sendProcessFailedMail = sendProcessFailedMail;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(mailService, "mailService");
	}

	public void logValidationFailed(IdType taskId)
	{
		validationLogger.debug("Validation of FHIR resources failed in process started by {}",
				taskId.toVersionless().getValue());

		if (sendValidationFailedMail)
			mailService.send("Validation Error",
					"Validation of FHIR resources failed in process started by " + taskId.toVersionless().getValue());
	}

	public void logValidationFailedLocal(IdType taskId)
	{
		validationLogger.debug("Local validation of FHIR resources failed in process started by {}",
				taskId.toVersionless().getValue());

		if (sendValidationFailedMail)
			mailService.send("Validation Error", "Local validation of FHIR resources failed in process started by "
					+ taskId.toVersionless().getValue());
	}

	public void logValidationFailedRemote(IdType taskId)
	{
		validationLogger.debug("Remote validation of FHIR resources failed in process started by {}",
				taskId.toVersionless().getValue());

		if (sendValidationFailedMail)
			mailService.send("Validation Error", "Remote validation of FHIR resources failed in process started by "
					+ taskId.toVersionless().getValue());
	}

	public void logDataSendFailed(IdType taskId)
	{
		errorLogger.debug("Send process failed started by {}", taskId.toVersionless().getValue());

		if (sendProcessFailedMail)
			mailService.send("Proccess Failed", "Send process failed started by " + taskId.toVersionless().getValue());
	}

	public void logDataTranslateFailed(IdType taskId)
	{
		errorLogger.debug("Translate process failed started by {}", taskId.toVersionless().getValue());

		if (sendProcessFailedMail)
			mailService.send("Proccess Failed",
					"Translate process failed started by " + taskId.toVersionless().getValue());
	}

	public void logDataReceiveFailed(IdType taskId)
	{
		errorLogger.debug("Receive process failed started by {}", taskId.toVersionless().getValue());

		if (sendProcessFailedMail)
			mailService.send("Proccess Failed",
					"Receive process failed started by " + taskId.toVersionless().getValue());
	}
}
