package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;

public class InsertDataIntoCodex extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(InsertDataIntoCodex.class);

	private final FhirClientFactory fhirClientFactory;
	private final FhirContext fhirContext;

	public InsertDataIntoCodex(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FhirContext fhirContext, FhirClientFactory fhirClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.fhirContext = fhirContext;

		this.fhirClientFactory = fhirClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(fhirClientFactory, "fhirClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);

		try
		{
			logger.info("Executing bundle against FHIR store ...");
			if (logger.isDebugEnabled())
				logger.debug("Received bundle: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));

			fhirClientFactory.getFhirClient().storeBundle(bundle);
		}
		catch (Exception e)
		{
			logger.warn("Error while executing bundle", e);
			throw e;
		}
	}
}
