package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_INSERT_INTO_CRR_FHIR_REPOSITORY_FAILED;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private final DataStoreClientFactory geccoClientFactory;
	private final DataLogger dataLogger;

	public InsertDataIntoCodex(ProcessPluginApi api, DataStoreClientFactory geccoClientFactory, DataLogger dataLogger)
	{
		super(api);

		this.geccoClientFactory = geccoClientFactory;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(geccoClientFactory, "geccoClientFactory");
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
				logger.info("Executing bundle against FHIR store ...");
				dataLogger.logData("Received bundle", bundle);

				geccoClientFactory.getDataStoreClient().getFhirClient().storeBundle(bundle);

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
}
