package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.r4.model.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteValidationErrorForGth extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteValidationErrorForGth.class);

	public DeleteValidationErrorForGth(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		String binaryUrl = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_BINARY_URL);
		IdType binaryId = new IdType(binaryUrl);

		logger.info("Deleting validation error binary {} permanently", binaryId.getValue());
		getFhirWebserviceClientProvider().getLocalWebserviceClient().delete(Binary.class, binaryId.getIdPart());
		getFhirWebserviceClientProvider().getLocalWebserviceClient().deletePermanently(Binary.class,
				binaryId.getIdPart());
	}
}
