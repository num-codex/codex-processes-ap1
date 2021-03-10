package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

public class SaveBusinessKey extends AbstractServiceDelegate
{
	public SaveBusinessKey(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper)
	{
		super(clientProvider, taskHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();
		task.addOutput(businessKeyParameter(execution.getBusinessKey()));

		task = getFhirWebserviceClientProvider().getLocalWebserviceClient().update(task);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_TASK, FhirResourceValues.create(task));
	}

	private TaskOutputComponent businessKeyParameter(String businessKey)
	{
		TaskOutputComponent param = new TaskOutputComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		param.setValue(new StringType(businessKey));

		return param;
	}
}
