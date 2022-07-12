package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYTPED_VALIDATION_ERROR_FROM_CRR_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
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
		Task task = getCurrentTaskFromExecutionVariables();
		IdType id = getDataReference(task).map(ref -> new IdType(ref)).get();

		FhirWebserviceClient client = getFhirWebserviceClientProvider().getWebserviceClient(id.getBaseUrl());

		try (InputStream binary = readBinaryResource(client, id.getIdPart(), id.getVersionIdPart()))
		{
			byte[] encrypted = binary.readAllBytes();
			execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, Variables.byteArrayValue(encrypted));
		}
		catch (Exception e)
		{
			throw new BpmnError(
					CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYTPED_VALIDATION_ERROR_FROM_CRR_FAILED,
					"Unable to download Binary resource with encrypted validation error from CRR");
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

	private InputStream readBinaryResource(FhirWebserviceClient client, String id, String version)
	{
		try
		{
			logger.info("Reading binary from {} with id {}/{}", client.getBaseUrl(), id, version);
			if (version != null && !version.isEmpty())
				return client.readBinary(id, version, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
			else
				return client.readBinary(id, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
		}
		catch (Exception e)
		{
			logger.warn("Error while reading Binary resoruce: " + e.getMessage(), e);
			throw e;
		}
	}
}
