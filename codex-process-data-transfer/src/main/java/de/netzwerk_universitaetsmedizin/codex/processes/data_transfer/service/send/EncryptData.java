package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_ECRYPTION_OF_DATA_FOR_CRR_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_ENCRYPTED_BUNDLE_SIZE;
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
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.AesGcmUtil;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.RsaAesGcmUtil;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;

public class EncryptData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(EncryptData.class);

	private final CrrKeyProvider crrKeyProvider;

	public EncryptData(ProcessPluginApi api, CrrKeyProvider crrKeyProvider)
	{
		super(api);

		this.crrKeyProvider = crrKeyProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(crrKeyProvider, "crrKeyProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution, dev.dsf.bpe.v1.variables.Variables variables)
			throws BpmnError, Exception
	{
		String pseudonym = ((PatientReference) variables.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE))
				.getIdentifier().getValue();
		Bundle bundle = variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE);

		try
		{
			byte[] bundleData = toByteArray(pseudonym, bundle);
			byte[] returnKey = AesGcmUtil.generateAES256Key().getEncoded();

			byte[] data = new byte[returnKey.length + bundleData.length];
			System.arraycopy(returnKey, 0, data, 0, returnKey.length);
			System.arraycopy(bundleData, 0, data, returnKey.length, bundleData.length);

			byte[] encrypted = RsaAesGcmUtil.encrypt(crrKeyProvider.getPublicKey(), data);

			variables.setByteArray(BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY, returnKey);
			variables.setByteArray(BPMN_EXECUTION_VARIABLE_BUNDLE, encrypted);

			Task task = variables.getStartTask();
			task = addEncryptedBundleSizeToTask(task, encrypted.length);
			variables.updateTask(task);
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
				| ShortBufferException | IOException e)
		{
			logger.warn("Unable to encrypt GECCO data for CRR: " + e.getMessage(), e);
			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_ECRYPTION_OF_DATA_FOR_CRR_FAILED,
					"Unable to encrypt GECCO data for CRR");
		}
	}

	private Task addEncryptedBundleSizeToTask(Task task, int encryptedSize)
	{
		task.addOutput().setValue(new UnsignedIntType(encryptedSize)).getType().getCodingFirstRep()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_ENCRYPTED_BUNDLE_SIZE);
		return task;
	}

	private byte[] toByteArray(String pseudonym, Bundle bundle) throws IOException
	{
		String bundleString = api.getFhirContext().newJsonParser().encodeResourceToString(bundle);

		return bundleString.replace(pseudonym, PSEUDONYM_PLACEHOLDER)
				.replace(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM, NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM)
				.getBytes(StandardCharsets.UTF_8);
	}
}
