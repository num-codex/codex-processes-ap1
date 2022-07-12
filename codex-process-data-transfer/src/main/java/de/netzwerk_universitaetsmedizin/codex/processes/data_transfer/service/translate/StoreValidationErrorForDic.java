package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNABLE_TO_STORE_ECRYPTED_VALIDATION_ERROR;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_MEDIC;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_NUM_CODEX_CONSORTIUM;

import java.util.Objects;

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
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class StoreValidationErrorForDic extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreValidationErrorForDic.class);

	private final EndpointProvider endpointProvider;

	public StoreValidationErrorForDic(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, EndpointProvider endpointProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.endpointProvider = endpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(endpointProvider, "endpointProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		byte[] encrypted = (byte[]) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);

		Task leadingTask = getLeadingTaskFromExecutionVariables();
		String medicIdentifierValue = leadingTask.getRequester().getIdentifier().getValue();

		String downloadUrl = saveBinary(encrypted, medicIdentifierValue);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_BINARY_URL, Variables.stringValue(downloadUrl));

		Endpoint targetEndpoint = getEndpoint(CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_MEDIC, medicIdentifierValue);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET,
				TargetValues.create(Target.createUniDirectionalTarget(medicIdentifierValue,
						getEndpointIdentifier(targetEndpoint), targetEndpoint.getAddress())));
	}

	protected String saveBinary(byte[] encryptedContent, String gthIdentifierValue)
	{
		Reference securityContext = new Reference();
		securityContext.setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(gthIdentifierValue);
		Binary binary = new Binary().setContentType(MediaType.APPLICATION_OCTET_STREAM)
				.setSecurityContext(securityContext).setData(encryptedContent);

		IdType created = createBinaryResource(binary);
		return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Binary.name(),
				created.getIdPart(), created.getVersionIdPart()).getValue();
	}

	private IdType createBinaryResource(Binary binary)
	{
		try
		{
			return getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().create(binary);
		}
		catch (Exception e)
		{
			logger.debug("Binary to create {}", FhirContext.forR4().newJsonParser().encodeResourceToString(binary));
			logger.warn("Error while creating Binary resource: " + e.getMessage(), e);
			throw new BpmnError(
					CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNABLE_TO_STORE_ECRYPTED_VALIDATION_ERROR,
					"Unable to create Binary resource with encrypted validation error for DIC in local DSF FHIR server");
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
