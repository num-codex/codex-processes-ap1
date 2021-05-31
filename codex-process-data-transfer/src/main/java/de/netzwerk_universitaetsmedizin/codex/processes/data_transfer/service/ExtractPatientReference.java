package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceValues;

public class ExtractPatientReference extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ExtractPatientReference.class);

	public ExtractPatientReference(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper)
	{
		super(clientProvider, taskHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();
		Reference patient = getPatientReference(task);

		if (patient.hasIdentifier() && NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM.equals(patient.getIdentifier().getSystem())
				&& patient.getIdentifier().hasValue())
		{
			execution.setVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE,
					PatientReferenceValues.create(PatientReference.from(patient.getIdentifier())));
			logger.info("Task contains DIC pseudonym {}", patient.getIdentifier().getValue());
		}
		else if (patient.hasReference())
		{
			execution.setVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE,
					PatientReferenceValues.create(PatientReference.from(patient.getReference())));
			logger.info("Task contains absolut patient reference {}", patient.getReference());
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
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT, Reference.class).findFirst()
						.orElseThrow(() -> new RuntimeException("No patient reference input parameter found"));
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}
}
