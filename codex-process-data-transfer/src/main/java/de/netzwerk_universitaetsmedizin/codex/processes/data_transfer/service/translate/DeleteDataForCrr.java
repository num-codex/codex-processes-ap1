package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ContinueStatus;

public class DeleteDataForCrr extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteDataForCrr.class);

	public DeleteDataForCrr(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		ContinueStatus continueStatus = (ContinueStatus) execution.getVariable(BPMN_EXECUTION_VARIABLE_CONTINUE_STATUS);
		logger.info("Continue status: {}", continueStatus);

		String binaryUrl = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_BINARY_URL);
		IdType binaryId = new IdType(binaryUrl);

		logger.info("Deleting gecco data binary {} permanently", binaryId.getValue());
		getFhirWebserviceClientProvider().getLocalWebserviceClient().delete(Binary.class, binaryId.getIdPart());
		getFhirWebserviceClientProvider().getLocalWebserviceClient().deletePermanently(Binary.class,
				binaryId.getIdPart());
	}
}
