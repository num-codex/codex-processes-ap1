package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_INSERT_INTO_CRR_FHIR_REPOSITORY_FAILED;

import java.util.*;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.ValidationException;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ContinueStatus;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class InsertDataIntoCodex extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(InsertDataIntoCodex.class);

	private final DataStoreClientFactory dataClientFactory;
	private final DataLogger dataLogger;
	private final FhirContext fhirContext;
	private final Map<String, String> clientMap;

	public InsertDataIntoCodex(ProcessPluginApi api, DataStoreClientFactory dataClientFactory, DataLogger dataLogger,
			FhirContext fhirContext, Map<String, String> clientConfig)
	{
		super(api);

		this.dataClientFactory = dataClientFactory;
		this.dataLogger = dataLogger;
		this.fhirContext = fhirContext;
		this.clientMap = clientConfig;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dataClientFactory, "dataClientFactory");
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Bundle bundle = variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE);

		try
		{
			try
			{
				logger.info("Executing bundle against FHIR store ... {}", asString(variables.getStartTask()));
				logger.info("Client filter Map {}", clientMap);
				logger.info("Client filter keySet {}", clientMap.keySet());
				dataLogger.logData("Received bundle", bundle);

				dataClientFactory.getDataStoreClient().getFhirClient().storeBundle(bundle);

				execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, ContinueStatus.SUCCESS);
			}
			catch (ValidationException e)
			{
				logger.info("Validation error");
				execution.setVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS, ContinueStatus.VALIDATION_ERROR);
				execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, e.getResultBundle());
			}
		}
		catch (Exception e)
		{
			logger.warn("Unable to insert data into CRR: {} - {}", e.getClass().getName(), e.getMessage());
			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_INSERT_INTO_CRR_FHIR_REPOSITORY_FAILED,
					"Unable to insert data into CRR");
		}
	}

	private String asString(Resource resource)
	{
		return fhirContext.newJsonParser().encodeResourceToString(resource);
	}
}
