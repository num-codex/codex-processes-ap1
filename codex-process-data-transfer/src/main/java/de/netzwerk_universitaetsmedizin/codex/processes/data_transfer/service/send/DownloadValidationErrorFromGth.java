package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_RETURN_TARGET;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_VALIDATION_ERROR_FROM_GTH_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_DTS;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_NUM_CODEX_CONSORTIUM;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadValidationErrorFromGth extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadValidationErrorFromGth.class);

	private final EndpointProvider endpointProvider;
	private final String gthIdentifierValue;

	public DownloadValidationErrorFromGth(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, EndpointProvider endpointProvider, String gthIdentifierValue)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.endpointProvider = endpointProvider;
		this.gthIdentifierValue = gthIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(gthIdentifierValue, "gthIdentifierValue");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Endpoint targetEndpoint = getEndpoint(CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_DTS, gthIdentifierValue);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_RETURN_TARGET,
				TargetValues.create(Target.createUniDirectionalTarget(gthIdentifierValue,
						getEndpointIdentifier(targetEndpoint), targetEndpoint.getAddress())));

		Task task = getCurrentTaskFromExecutionVariables(execution);
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
					CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DOWNLOAD_OF_ENCRYPTED_VALIDATION_ERROR_FROM_GTH_FAILED,
					"Unable to download Binary resource with encrypted validation error from GTH");
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

	private Endpoint getEndpoint(String role, String identifier)
	{
		return endpointProvider
				.getFirstConsortiumEndpoint(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_NUM_CODEX_CONSORTIUM,
						CODESYSTEM_HIGHMED_ORGANIZATION_ROLE, role, identifier)
				.get();
	}

	private String getEndpointIdentifier(Endpoint endpoint)
	{
		return endpoint.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER.equals(i.getSystem())).findFirst()
				.map(Identifier::getValue).get();
	}
}
