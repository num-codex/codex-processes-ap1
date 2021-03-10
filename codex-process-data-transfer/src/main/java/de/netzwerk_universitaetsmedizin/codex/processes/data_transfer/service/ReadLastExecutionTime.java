package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

public class ReadLastExecutionTime extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ReadLastExecutionTime.class);

	public ReadLastExecutionTime(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper)
	{
		super(clientProvider, taskHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables();

		Optional<DateTimeType> exportFrom = getExportFromInput(task);
		Date exportFromDate = exportFrom.map(DateTimeType::getValue).orElse(null);
		TemporalPrecisionEnum exportFromPrecision = exportFrom.map(DateTimeType::getPrecision).orElse(null);

		Optional<InstantType> exportTo = getExportToOutput(task);
		Date exportToDate = exportTo.map(InstantType::getValue).orElse(null);
		TemporalPrecisionEnum exportToPrecision = exportTo.map(InstantType::getPrecision).orElse(null);

		Date now = new Date();

		// first run
		if (exportTo.isEmpty())
		{
			// export from not specified
			if (exportFrom.isEmpty())
			{
				logger.debug("export-from: null");
				logger.debug("export-to: {}", now);

				execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO, Variables.dateValue(now));
			}
			else
			{
				logger.debug("export-from: {}, precision: {}", exportFromDate, exportFromPrecision);
				logger.debug("export-to: {}", now);

				execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM, Variables.dateValue(exportFromDate));
				execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION,
						Variables.stringValue(exportFromPrecision.name()));
				execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO, Variables.dateValue(now));
			}
		}
		// second, third, ... run
		else
		{
			// old export-to becomes new export-from
			logger.debug("export-from: {}, precision: {}", exportToDate, exportToPrecision);
			logger.debug("export-to: {}", now);

			execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM, Variables.dateValue(exportToDate));
			execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION,
					Variables.stringValue(exportToPrecision.name()));
			execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO, Variables.dateValue(now));
		}
	}

	private Optional<DateTimeType> getExportFromInput(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM, DateTimeType.class).findFirst();
	}

	private Optional<InstantType> getExportToOutput(Task task)
	{
		return getOutputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO, InstantType.class).findFirst();
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}

	private <T extends Type> Stream<T> getOutputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getOutput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}
}
