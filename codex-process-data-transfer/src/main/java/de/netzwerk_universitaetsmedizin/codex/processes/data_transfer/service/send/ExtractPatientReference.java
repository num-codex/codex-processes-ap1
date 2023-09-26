package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceValues;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class ExtractPatientReference extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ExtractPatientReference.class);

	public ExtractPatientReference(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Task task = variables.getStartTask();
		Reference patient = getPatientReference(task);

		if (patient.hasIdentifier() && NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM.equals(patient.getIdentifier().getSystem())
				&& patient.getIdentifier().hasValue())
		{
			variables.setVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE,
					PatientReferenceValues.create(PatientReference.from(patient.getIdentifier())));
			logger.info("Task contains DIC pseudonym {}", patient.getIdentifier().getValue());
		}
		else if (patient.hasReference())
		{
			variables.setVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE,
					PatientReferenceValues.create(PatientReference.from(patient.getReference())));
			logger.info("Task contains absolute patient reference {}", patient.getReference());
		}
		else
		{
			logger.warn(
					"Patient reference input parameter does not contain DIC pseudonym identifier or absolute patient reference");
			throw new RuntimeException(
					"Patient reference input parameter does not contain DIC pseudonym identifier or absolute patient reference");
		}
	}

	private Reference getPatientReference(Task task)
	{
		return api.getTaskHelper()
				.getFirstInputParameterValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
						CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT, Reference.class)
				.orElseThrow(() -> new RuntimeException("No patient reference input parameter found"));
	}
}
