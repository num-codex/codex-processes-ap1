package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class DeleteValidationErrorForDts extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteValidationErrorForDts.class);

	public DeleteValidationErrorForDts(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		String binaryUrl = variables.getString(BPMN_EXECUTION_VARIABLE_BINARY_URL);
		IdType binaryId = new IdType(binaryUrl);

		logger.info("Deleting validation error binary {} permanently", binaryId.getValue());
		api.getFhirWebserviceClientProvider().getLocalWebserviceClient().delete(Binary.class, binaryId.getIdPart());
		api.getFhirWebserviceClientProvider().getLocalWebserviceClient().deletePermanently(Binary.class,
				binaryId.getIdPart());
	}
}
