package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DECRYPTION_OF_GECCO_DATA_FROM_DIC_FAILED;
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
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.RsaAesGcmUtil;

public class DecryptData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DecryptData.class);

	private final FhirContext fhirContext;
	private final CrrKeyProvider crrKeyProvider;

	public DecryptData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		try
		{
			decryptGeccoData(execution);
		}
		catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException
				| NoSuchAlgorithmException | InvalidAlgorithmParameterException | IOException e)
		{
			logger.warn("Unable to decrypt GECCO data from DIC: " + e.getMessage(), e);
			throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DECRYPTION_OF_GECCO_DATA_FROM_DIC_FAILED,
					"Error while decrypting GECCO data from DIC");
		}
	}

	private void decryptGeccoData(DelegateExecution execution)
			throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException
	{
		Task task = getCurrentTaskFromExecutionVariables(execution);
		Optional<String> pseudonym = getPseudonym(task);

		byte[] encrypted = (byte[]) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);
		byte[] decrypted = RsaAesGcmUtil.decrypt(crrKeyProvider.getPrivateKey(), encrypted);

		byte[] returnKey = new byte[32];
		byte[] bundleData = new byte[decrypted.length - 32];

		System.arraycopy(decrypted, 0, returnKey, 0, 32);
		System.arraycopy(decrypted, 32, bundleData, 0, decrypted.length - 32);

		Bundle bundle = fromByteArray(pseudonym.get(), bundleData);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, FhirResourceValues.create(bundle));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_AES_RETURN_KEY, Variables.byteArrayValue(returnKey));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM, Variables.stringValue(pseudonym.get()));
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
		return fhirContext.newJsonParser().parseResource(Bundle.class, bundleString);
	}
}
