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
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.RsaAesGcmUtil;

public class EncryptData extends AbstractServiceDelegate
{
	private final FhirContext fhirContext;
	private final CrrKeyProvider crrKeyProvider;

	public EncryptData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper, FhirContext fhirContext,
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

		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_BUNDLE);
		byte[] bundleData = toByteArray(pseudonym.get(), bundle);

		byte[] encrypted = RsaAesGcmUtil.encrypt(crrKeyProvider.getPublicKey(), bundleData);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, Variables.byteArrayValue(encrypted));
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

	private byte[] toByteArray(String pseudonym, Bundle bundle) throws IOException
	{
		String bundleString = fhirContext.newJsonParser().encodeResourceToString(bundle);
		return bundleString.replace(pseudonym, PSEUDONYM_PLACEHOLDER).getBytes(StandardCharsets.UTF_8);
	}
}
