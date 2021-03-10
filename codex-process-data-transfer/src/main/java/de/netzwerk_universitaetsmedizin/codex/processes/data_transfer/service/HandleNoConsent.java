package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleNoConsent extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(HandleNoConsent.class);
	
	public HandleNoConsent(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper)
	{
		super(clientProvider, taskHelper);
	}

	@Override
	protected void doExecute(DelegateExecution arg0) throws BpmnError, Exception
	{
		// TODO Auto-generated method stub
		logger.debug("TODO");
	}
}
