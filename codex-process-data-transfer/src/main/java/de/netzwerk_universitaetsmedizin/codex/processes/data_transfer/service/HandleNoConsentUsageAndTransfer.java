package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleNoConsentUsageAndTransfer extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(HandleNoConsentUsageAndTransfer.class);

	public HandleNoConsentUsageAndTransfer(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution arg0) throws BpmnError, Exception
	{
		// TODO Auto-generated method stub
		logger.debug("TODO");
	}
}
