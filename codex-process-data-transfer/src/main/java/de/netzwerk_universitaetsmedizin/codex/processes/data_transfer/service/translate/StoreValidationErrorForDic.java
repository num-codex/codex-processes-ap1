package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreValidationErrorForDic extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreValidationErrorForDic.class);

	public StoreValidationErrorForDic(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		// TODO store validation error for DIC
		logger.debug("TODO store validation error for DIC");
	}
}
