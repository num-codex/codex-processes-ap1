package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging;

import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLogger
{
	private static final Logger validationLogger = LoggerFactory.getLogger("validation-error-logger");

	public void logValidationFailed(IdType taskId)
	{
		validationLogger.debug(
				"Validation of FHIR resources faild during execution of data-send process started by Task {}",
				taskId.getValue());
	}
}
