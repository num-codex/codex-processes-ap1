package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;

public class InsertDataIntoCodex extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(InsertDataIntoCodex.class);

	private final FhirClientFactory localFhirStoreClientFactory;

	public InsertDataIntoCodex(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			FhirClientFactory localFhirStoreClientFactory)
	{
		super(clientProvider, taskHelper);

		this.localFhirStoreClientFactory = localFhirStoreClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(localFhirStoreClientFactory, "localFhirStoreClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);

		try
		{
			logger.info("Executing bundle against FHIR store ...");
			localFhirStoreClientFactory.getFhirClient().storeBundle(bundle);
		}
		catch (Exception e)
		{
			logger.warn("Error while executing bundle", e);
			throw e;
		}
	}
}
