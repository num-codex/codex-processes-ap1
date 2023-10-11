package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_DATA_FROM_DTS_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;

import java.io.InputStream;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.FhirWebserviceClient;
import jakarta.ws.rs.core.MediaType;

public class DownloadDataFromDts extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadDataFromDts.class);

	public DownloadDataFromDts(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Task task = variables.getStartTask();
		IdType id = getDataReference(task).get();

		try (InputStream binary = readBinaryResource(id))
		{
			byte[] encrypted = binary.readAllBytes();
			variables.setByteArray(BPMN_EXECUTION_VARIABLE_BUNDLE, encrypted);
		}
		catch (Exception e)
		{
			logger.warn("Error while reading Binary resoruce: " + e.getMessage(), e);

			throw new BpmnError(
					CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_DATA_FROM_DTS_FAILED,
					"Unable to download Binary resource with encrypted data from DTS");
		}
	}

	private Optional<IdType> getDataReference(Task task)
	{
		return api.getTaskHelper()
				.getFirstInputParameterValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
						CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE, Reference.class)
				.map(Reference::getReference).map(IdType::new);
	}

	private InputStream readBinaryResource(IdType binaryId) throws Exception
	{
		final String id = binaryId.getIdPart();
		final String version = binaryId.getVersionIdPart();

		FhirWebserviceClient client = api.getFhirWebserviceClientProvider().getWebserviceClient(binaryId.getBaseUrl());

		logger.info("Reading binary from {} with id {}/{}", client.getBaseUrl(), id, version);
		if (version != null && !version.isEmpty())
			return client.readBinary(id, version, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
		else
			return client.readBinary(id, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
	}
}
