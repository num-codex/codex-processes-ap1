package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_IDAT_MERGE_GRANTED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.ConsentClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.ConsentClientFactory;

public class CheckConsent extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CheckConsent.class);

	private final ConsentClientFactory consentClientFactory;
	private final String idatMergeGrantedOid;
	private final String usageGrantedOid;
	private final String transferGrantedOid;

	public CheckConsent(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ConsentClientFactory consentClientFactory, String idatMergeGrantedOid, String usageGrantedOid,
			String transferGrantedOid)
	{
		super(clientProvider, taskHelper);

		this.consentClientFactory = consentClientFactory;
		this.idatMergeGrantedOid = idatMergeGrantedOid;
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
	protected void doExecute(DelegateExecution execution) throws Exception
	{
		String dicSourceAndPseudonym = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM);
		String patientReference = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE);

		ConsentClient client = consentClientFactory.getConsentClient();
		List<String> consentOids = dicSourceAndPseudonym != null
				? client.getConsentOidsForIdentifierReference(dicSourceAndPseudonym)
				: client.getConsentOidsForAbsoluteReference(patientReference);

		boolean idatMergeGranted = idatMergeGranted(consentOids,
				dicSourceAndPseudonym != null ? dicSourceAndPseudonym : patientReference);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_IDAT_MERGE_GRANTED, Variables.booleanValue(idatMergeGranted));

		boolean usageAndTransferGranted = usageAndTransferGranted(consentOids,
				dicSourceAndPseudonym != null ? dicSourceAndPseudonym : patientReference);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED,
				Variables.booleanValue(usageAndTransferGranted));
	}

	protected boolean usageAndTransferGranted(List<String> consentOids, String patient)
	{
		boolean usageAndTransferGranted = consentOids.contains(usageGrantedOid)
				&& consentOids.contains(transferGrantedOid);

		if (usageAndTransferGranted)
			logger.info("Usage and transfer granted for {}", patient);
		else
			logger.warn("Usage or transfer not granted for {}", patient);

		return usageAndTransferGranted;
	}

	protected boolean idatMergeGranted(List<String> consentOids, String patient)
	{
		boolean idatMergeGranted = consentOids.contains(idatMergeGrantedOid);

		if (idatMergeGranted)
			logger.info("IDAT merge granted for {}", patient);
		else
			logger.warn("IDAT merge not granted for {}", patient);

		return idatMergeGranted;
	}
}
