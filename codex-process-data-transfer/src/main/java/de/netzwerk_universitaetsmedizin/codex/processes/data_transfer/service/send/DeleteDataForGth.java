package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;

public class DeleteDataForGth extends AbstractServiceDelegate
{
	// private static final Logger logger = LoggerFactory.getLogger(DeleteDataForGth.class);

	public DeleteDataForGth(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		// String binaryUrl = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_BINARY_URL);
		// IdType binaryId = new IdType(binaryUrl);
		//
		// logger.info("Deleting gecco data binary {} permanently", binaryId.getValue());
		// getFhirWebserviceClientProvider().getLocalWebserviceClient().delete(Binary.class, binaryId.getIdPart());
		// getFhirWebserviceClientProvider().getLocalWebserviceClient().deletePermanently(Binary.class,
		// binaryId.getIdPart());

		// TODO implement delete, cave possible task update with reference to delete binary
	}
}
