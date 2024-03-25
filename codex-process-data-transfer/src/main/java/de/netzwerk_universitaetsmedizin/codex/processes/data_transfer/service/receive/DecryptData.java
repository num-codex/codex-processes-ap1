package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DECRYPTION_OF_DATA_FROM_DIC_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PSEUDONYM_PLACEHOLDER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.RsaAesGcmUtil;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class DecryptData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DecryptData.class);

	private final CrrKeyProvider crrKeyProvider;

	public DecryptData(ProcessPluginApi api, CrrKeyProvider crrKeyProvider)
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
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		try
		{
			decryptData(variables);
		}
		catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException
				| NoSuchAlgorithmException | InvalidAlgorithmParameterException | IOException e)
		{
			logger.warn("Unable to decrypt data from DIC: {}", e.getMessage(), e);
			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DECRYPTION_OF_DATA_FROM_DIC_FAILED,
					"Error while decrypting data from DIC");
		}
	}

	private void decryptData(Variables variables)
			throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException
	{
		Task task = variables.getStartTask();
		Optional<String> pseudonym = getPseudonym(task);

		byte[] encrypted = variables.getByteArray(BPMN_EXECUTION_VARIABLE_BUNDLE);
		byte[] decrypted = RsaAesGcmUtil.decrypt(crrKeyProvider.getPrivateKey(), encrypted);

		byte[] returnKey = new byte[32];
		byte[] bundleData = new byte[decrypted.length - 32];

		System.arraycopy(decrypted, 0, returnKey, 0, 32);
		System.arraycopy(decrypted, 32, bundleData, 0, decrypted.length - 32);

		Bundle bundle = fromByteArray(pseudonym.get(), bundleData);

		variables.setResource(BPMN_EXECUTION_VARIABLE_BUNDLE, bundle);
		variables.setByteArray(BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY, returnKey);
		variables.setString(BPMN_EXECUTION_VARIABLE_PSEUDONYM, pseudonym.get());
	}

	private Optional<String> getPseudonym(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM, Identifier.class).findFirst()
						.map(Identifier::getValue);
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}

	private Bundle fromByteArray(String pseudonym, byte[] bundle) throws IOException
	{
		String bundleString = new String(bundle, StandardCharsets.UTF_8).replace(PSEUDONYM_PLACEHOLDER, pseudonym);
		return api.getFhirContext().newJsonParser().parseResource(Bundle.class, bundleString);
	}
}
