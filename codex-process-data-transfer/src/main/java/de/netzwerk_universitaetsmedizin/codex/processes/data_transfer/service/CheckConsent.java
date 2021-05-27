package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
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
		String dicSourceAndPseudonym = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM);

		boolean usageAndTransferGranted = usageAndTrransferGranted(dicSourceAndPseudonym);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED,
				Variables.booleanValue(usageAndTransferGranted));
	}

	protected boolean usageAndTrransferGranted(String dicSourceAndPseudonym)
	{
		List<String> consentOids = consentClientFactory.getConsentClient().getConsentOidsFor(dicSourceAndPseudonym);

		boolean usageAndTransferGranted = consentOids.contains(usageGrantedOid)
				&& consentOids.contains(transferGrantedOid);

		if (usageAndTransferGranted)
			logger.info("Usage and transfer granted for DIC pseudonym {}", dicSourceAndPseudonym);
		else
			logger.warn("Usage or transfer not granted for DIC pseudonym {}", dicSourceAndPseudonym);

		return usageAndTransferGranted;
	}
}
