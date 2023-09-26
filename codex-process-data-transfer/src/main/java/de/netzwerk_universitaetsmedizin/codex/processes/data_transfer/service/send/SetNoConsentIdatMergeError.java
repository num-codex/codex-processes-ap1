package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class SetNoConsentIdatMergeError extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(SetNoConsentIdatMergeError.class);

	public SetNoConsentIdatMergeError(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		// TODO set Variable errorCode, errorMessage
		logger.debug("TODO set Variable errorCode, errorMessage");
	}
}
