package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PSEUDONYM_PLACEHOLDER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.RsaAesGcmUtil;

public class DecryptData extends AbstractServiceDelegate
{
	private final FhirContext fhirContext;
	private final CrrKeyProvider crrKeyProvider;

	public DecryptData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper, FhirContext fhirContext,
			CrrKeyProvider crrKeyProvider)
	{
		super(clientProvider, taskHelper);

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
		Task task = getCurrentTaskFromExecutionVariables();
		Optional<String> pseudonym = getPseudonym(task);

		byte[] encrypted = (byte[]) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);
		byte[] deencrypted = RsaAesGcmUtil.decrypt(crrKeyProvider.getPrivateKey(), encrypted);

		Bundle bundle = fromByteArray(pseudonym.get(), deencrypted);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, FhirResourceValues.create(bundle));
	}

	private Optional<String> getPseudonym(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM, StringType.class).findFirst()
						.map(StringType::getValue);
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
