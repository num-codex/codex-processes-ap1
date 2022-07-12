package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_ECRYPTION_OF_GECCO_DATA_FOR_CRR_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PSEUDONYM_PLACEHOLDER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.pseudonymization.crypto.AesGcmUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.RsaAesGcmUtil;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;

public class EncryptData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(EncryptData.class);

	private final FhirContext fhirContext;
	private final CrrKeyProvider crrKeyProvider;

	public EncryptData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FhirContext fhirContext, CrrKeyProvider crrKeyProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.fhirContext = fhirContext;
		this.crrKeyProvider = crrKeyProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(crrKeyProvider, "crrKeyProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		String pseudonym = ((PatientReference) execution.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE))
				.getIdentifier().getValue();
		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);

		try
		{
			byte[] bundleData = toByteArray(pseudonym, bundle);
			byte[] returnKey = AesGcmUtil.generateAES256Key().getEncoded();

			byte[] data = new byte[returnKey.length + bundleData.length];
			System.arraycopy(returnKey, 0, data, 0, returnKey.length);
			System.arraycopy(bundleData, 0, data, returnKey.length, bundleData.length);

			byte[] encrypted = RsaAesGcmUtil.encrypt(crrKeyProvider.getPublicKey(), data);

			execution.setVariable(BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY, Variables.byteArrayValue(returnKey));
			execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, Variables.byteArrayValue(encrypted));
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
				| ShortBufferException | IOException e)
		{
			logger.warn("Unable to encrypt GECCO data for CRR: " + e.getMessage(), e);
			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_ECRYPTION_OF_GECCO_DATA_FOR_CRR_FAILED,
					"Unable to encrypt GECCO data for CRR");
		}
	}

	private byte[] toByteArray(String pseudonym, Bundle bundle) throws IOException
	{
		String bundleString = fhirContext.newJsonParser().encodeResourceToString(bundle);

		return bundleString.replace(pseudonym, PSEUDONYM_PLACEHOLDER)
				.replace(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM, NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM)
				.getBytes(StandardCharsets.UTF_8);
	}
}
