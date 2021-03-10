package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIZ_PSEUDONYM;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ReadData.class);

	public ReadData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper)
	{
		super(clientProvider, taskHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();

		Optional<String> pseudonym = getPseudonym(task);
		Optional<DateTimeType> exportFrom = getExportFrom(task);
		Optional<InstantType> exportTo = getExportTo(task);

		if (pseudonym.isEmpty())
			throw new IllegalStateException("No pseudonym in Task");
		if (exportTo.isEmpty())
			throw new IllegalStateException("No export-to in Task");

		Bundle bundle = readDataAndCreateBundle(pseudonym.get(), exportFrom.orElse(null), exportTo.get());

		execution.setVariable(BPMN_EXECUTION_VARIABLE_BUNDLE, FhirResourceValues.create(bundle));
	}

	protected Bundle readDataAndCreateBundle(String pseudonym, DateTimeType from, InstantType to)
	{
		logger.warn("Read Data not implemented, simmulating Data-Set for pseudonym {}", pseudonym);

		Bundle b = new Bundle();
		b.setType(BundleType.TRANSACTION);

		Patient p = new Patient();
		p.addIdentifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIZ_PSEUDONYM).setValue(pseudonym);

		BundleEntryComponent pEntry = b.addEntry();
		pEntry.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
		pEntry.setResource(p);
		pEntry.getRequest().setMethod(HTTPVerb.PUT)
				.setUrl("Patient?identifier=" + NAMING_SYSTEM_NUM_CODEX_DIZ_PSEUDONYM + "|" + pseudonym);

		return b;
	}

	private Optional<String> getPseudonym(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM, StringType.class).findFirst()
						.map(StringType::getValue);
	}

	private Optional<DateTimeType> getExportFrom(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM, DateTimeType.class).findFirst();
	}

	private Optional<InstantType> getExportTo(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO, InstantType.class).findFirst();
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}
}
