package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_IDAT_MERGE_GRANTED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED;

import java.util.ArrayList;
import java.util.Collection;
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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;

public class CheckConsent extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CheckConsent.class);

	private final ConsentClientFactory consentClientFactory;

	private final List<String> idatMergeGrantedOids = new ArrayList<>();
	private final List<String> mdatTransferGrantedOids = new ArrayList<>();

	public CheckConsent(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ConsentClientFactory consentClientFactory, Collection<String> idatMergeGrantedOids,
			Collection<String> mdatTransferGrantedOids)
	{
		super(clientProvider, taskHelper);

		this.consentClientFactory = consentClientFactory;

		if (idatMergeGrantedOids != null)
			this.idatMergeGrantedOids.addAll(idatMergeGrantedOids);
		if (mdatTransferGrantedOids != null)
			this.mdatTransferGrantedOids.addAll(mdatTransferGrantedOids);
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
		PatientReference reference = (PatientReference) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE);
		ConsentClient client = consentClientFactory.getConsentClient();

		if (reference.hasIdentifier() && !reference.hasAbsoluteReference())
		{
			String dicSourceAndPseudonym = reference.getIdentifier().getValue();
			List<String> consentOids = client.getConsentOidsForIdentifierReference(dicSourceAndPseudonym);

			boolean usageAndTransferGranted = usageAndTransferGranted(consentOids, dicSourceAndPseudonym);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED,
					Variables.booleanValue(usageAndTransferGranted));
		}
		else if (!reference.hasIdentifier() && reference.hasAbsoluteReference())
		{
			String patientAbsoluteReference = reference.getAbsoluteReference();
			List<String> consentOids = client.getConsentOidsForIdentifierReference(patientAbsoluteReference);

			boolean idatMergeGranted = idatMergeGranted(consentOids, patientAbsoluteReference);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_IDAT_MERGE_GRANTED, Variables.booleanValue(idatMergeGranted));

			boolean usageAndTransferGranted = usageAndTransferGranted(consentOids, patientAbsoluteReference);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED,
					Variables.booleanValue(usageAndTransferGranted));
		}
		else
		{
			throw new IllegalStateException("PatientReference contains identifier and absolute reference");
		}
	}

	protected boolean usageAndTransferGranted(List<String> consentOids, String patientReference)
	{
		boolean mdatTransferGranted = consentOids.containsAll(mdatTransferGrantedOids);

		if (mdatTransferGranted)
			logger.info("MDAT transfer granted for {}", patientReference);
		else
			logger.warn("MDAT transfer not granted for {}", patientReference);

		return mdatTransferGranted;
	}

	protected boolean idatMergeGranted(List<String> consentOids, String patientReference)
	{
		boolean idatMergeGranted = consentOids.containsAll(idatMergeGrantedOids);

		if (idatMergeGranted)
			logger.info("IDAT merge granted for {}", patientReference);
		else
			logger.warn("IDAT merge not granted for {}", patientReference);

		return idatMergeGranted;
	}
}
