package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadValidationErrorFromCrr extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadValidationErrorFromCrr.class);

	public DownloadValidationErrorFromCrr(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		// TODO download validation error from CRR
		logger.debug("TODO download validation error from CRR");
	}
}