package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging;

import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLogger
{
	private static final Logger validationLogger = LoggerFactory.getLogger("validation-error-logger");
	private static final Logger errorLogger = LoggerFactory.getLogger("error-logger");

	public void logValidationFailed(IdType taskId)
	{
		validationLogger.debug("Validation of FHIR resources failed, started by Task {}", taskId.getValue());
	}

	public void logValidationFailedLocal(IdType taskId)
	{
		validationLogger.debug("Local validation of FHIR resources failed, started by Task {}", taskId.getValue());
	}

	public void logValidationFailedRemote(IdType taskId)
	{
		validationLogger.debug("Remote validation of FHIR resources failed, started by Task {}", taskId.getValue());
	}

	public void logDataSendFailed(IdType taskId)
	{
		errorLogger.debug("Send process failed, started by Task {}", taskId.getValue());
	}

	public void logDataTranslateFailed(IdType taskId)
	{
		errorLogger.debug("Translate process failed, started by Task {}", taskId.getValue());
	}

	public void logDataReceiveFailed(IdType taskId)
	{
		errorLogger.debug("Receive process failed, started by Task {}", taskId.getValue());
	}
}
