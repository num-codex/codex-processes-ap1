package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;
import static org.highmed.dsf.bpe.ConstantsBase.OPENEHR_MIMETYPE_JSON;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadDataFromTransferHub extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadDataFromTransferHub.class);

	public DownloadDataFromTransferHub(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper)
	{
		super(clientProvider, taskHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();

		IdType id = getDataReference(task).map(ref -> new IdType(ref)).get();

		FhirWebserviceClient client = getFhirWebserviceClientProvider().getRemoteWebserviceClient(id.getBaseUrl());

		try (InputStream binary = readBinaryResource(client, id.getIdPart()))
		{
			byte[] encrypted = binary.readAllBytes();
			execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, Variables.byteArrayValue(encrypted));
		}
	}

	private Optional<String> getDataReference(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE, Reference.class).findFirst()
						.map(Reference::getReference);
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}


	private InputStream readBinaryResource(FhirWebserviceClient client, String id)
	{
		try
		{
			logger.info("Reading binary from {} with id {}", client.getBaseUrl(), id);
			return client.readBinary(id, MediaType.valueOf(OPENEHR_MIMETYPE_JSON));
		}
		catch (Exception e)
		{
			logger.warn("Error while reading Binary resoruce: " + e.getMessage(), e);
			throw e;
		}
	}
}
