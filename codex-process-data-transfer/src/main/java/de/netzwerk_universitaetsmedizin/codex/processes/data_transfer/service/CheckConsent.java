package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.ConsentClientFactory;

public class CheckConsent extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CheckConsent.class);

	private final ConsentClientFactory consentClientFactory;
	private final String usageGrantedOid;
	private final String transferGrantedOid;

	public CheckConsent(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ConsentClientFactory consentClientFactory, String usageGrantedOid, String transferGrantedOid)
	{
		super(clientProvider, taskHelper);

		this.consentClientFactory = consentClientFactory;
		this.usageGrantedOid = usageGrantedOid;
		this.transferGrantedOid = transferGrantedOid;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(consentClientFactory, "consentClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();

		boolean usageAndTransferGranted = getPseudonym(task).map(this::usageAndTrransferGranted).orElse(false);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED,
				Variables.booleanValue(usageAndTransferGranted));
	}

	protected boolean usageAndTrransferGranted(String dicPseudonym)
	{
		List<String> consentOids = consentClientFactory.getConsentClient().getConsentOidsFor(dicPseudonym);

		boolean usageAndTransferGranted = consentOids.contains(usageGrantedOid)
				&& consentOids.contains(transferGrantedOid);

		if (usageAndTransferGranted)
			logger.info("Usage and transfer granted for pseudonym {}", dicPseudonym);
		else
			logger.warn("Usage or transfer not granted for pseudonym {}", dicPseudonym);

		return usageAndTransferGranted;
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
}
