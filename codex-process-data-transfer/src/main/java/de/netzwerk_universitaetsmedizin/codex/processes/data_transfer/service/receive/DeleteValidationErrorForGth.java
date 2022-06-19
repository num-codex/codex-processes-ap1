package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteValidationErrorForGth extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteValidationErrorForGth.class);

	public DeleteValidationErrorForGth(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		// TODO delete validation error for GTH
		logger.debug("TODO delete validation error for GTH");
	}
}
