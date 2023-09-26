package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DECRYPTION_OF_VALIDATION_ERROR_FROM_CRR_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PSEUDONYM_PLACEHOLDER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.RETURN_AAD;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.AesGcmUtil;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class DecryptValidationErrorFromDts extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DecryptValidationErrorFromDts.class);

	public DecryptValidationErrorFromDts(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		try
		{
			decryptValidationError(execution, variables);
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | IOException e)
		{
			logger.warn("Unable to decrypt validation error from CRR: " + e.getMessage(), e);
			throw new BpmnError(
					CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DECRYPTION_OF_VALIDATION_ERROR_FROM_CRR_FAILED,
					"Error while decrypting validation error from CRR");
		}
	}

	private void decryptValidationError(DelegateExecution execution, Variables variables)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException
	{
		String pseudonym = ((PatientReference) variables.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE))
				.getIdentifier().getValue();

		byte[] returnKey = variables.getByteArray(BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY);

		byte[] encrypted = variables.getByteArray(BPMN_EXECUTION_VARIABLE_BUNDLE);
		byte[] decrypted = AesGcmUtil.decrypt(encrypted, RETURN_AAD, new SecretKeySpec(returnKey, "AES"));

		Bundle bundle = fromByteArray(pseudonym, decrypted);

		variables.setResource(BPMN_EXECUTION_VARIABLE_BUNDLE, bundle);
	}

	private Bundle fromByteArray(String pseudonym, byte[] bundle) throws IOException
	{
		String bundleString = new String(bundle, StandardCharsets.UTF_8).replace(PSEUDONYM_PLACEHOLDER, pseudonym);
		return api.getFhirContext().newJsonParser().parseResource(Bundle.class, bundleString);
	}
}
