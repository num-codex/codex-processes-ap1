package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class LogSuccess extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogSuccess.class);

	public LogSuccess(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		logger.info("All resources successfully added to CRR FHIR repository");
	}
}
