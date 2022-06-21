package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_STOP_TIMER;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartTimer extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StartTimer.class);

	public StartTimer(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		logger.debug("Setting variable '{}' to false", BPMN_EXECUTION_VARIABLE_STOP_TIMER);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_STOP_TIMER, Variables.booleanValue(false));
	}
}
