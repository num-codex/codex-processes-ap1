package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNABLE_TO_STORE_ECRYPTED_DATA;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.constants.NamingSystems;
import dev.dsf.bpe.v1.variables.Variables;
import jakarta.ws.rs.core.MediaType;

public class StoreDataForDts extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreDataForDts.class);

	private final String dtsIdentifierValue;

	public StoreDataForDts(ProcessPluginApi api, String dtsIdentifierValue)
	{
		super(api);

		this.dtsIdentifierValue = dtsIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dtsIdentifierValue, "dtsIdentifierValue");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		byte[] encrypted = variables.getByteArray(BPMN_EXECUTION_VARIABLE_BUNDLE);
		String downloadUrl = saveBinaryForDts(encrypted);
		variables.setString(BPMN_EXECUTION_VARIABLE_BINARY_URL, downloadUrl);
	}

	protected String saveBinaryForDts(byte[] encryptedContent)
	{
		Reference securityContext = new Reference();
		securityContext.setType(ResourceType.Organization.name())
				.setIdentifier(NamingSystems.OrganizationIdentifier.withValue(dtsIdentifierValue));
		Binary binary = new Binary().setContentType(MediaType.APPLICATION_OCTET_STREAM)
				.setSecurityContext(securityContext).setData(encryptedContent);

		IdType created = createBinaryResource(binary);
		return new IdType(api.getFhirWebserviceClientProvider().getLocalWebserviceClient().getBaseUrl(),
				ResourceType.Binary.name(), created.getIdPart(), created.getVersionIdPart()).getValue();
	}

	private IdType createBinaryResource(Binary binary)
	{
		try
		{
			return api.getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().create(binary);
		}
		catch (Exception e)
		{
			logger.debug("Binary to create {}", FhirContext.forR4().newJsonParser().encodeResourceToString(binary));
			logger.warn("Error while creating Binary resource: " + e.getMessage(), e);

			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_UNABLE_TO_STORE_ECRYPTED_DATA,
					"Unable to create Binary resource with encrypted data for DTS in local DSF FHIR server");
		}
	}
}
