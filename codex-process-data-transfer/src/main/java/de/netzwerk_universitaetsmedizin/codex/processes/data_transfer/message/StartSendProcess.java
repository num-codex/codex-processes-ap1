package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;

import java.util.Date;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;

public class StartSendProcess extends AbstractTaskMessageSend
{
	private static final Logger logger = LoggerFactory.getLogger(StartSendProcess.class);

	public StartSendProcess(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		return Stream.of(pseudonymParameter(execution), exportFromParameter(execution), exportToParameter(execution))
				.filter(p -> p != null);
	}

	private ParameterComponent pseudonymParameter(DelegateExecution execution)
	{
		String pseudonym = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM);

		ParameterComponent param = new ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM);
		param.setValue(new Identifier().setSystem(ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIZ_PSEUDONYM)
				.setValue(pseudonym));
		return param;
	}

	private ParameterComponent exportFromParameter(DelegateExecution execution)
	{
		Date exportFrom = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM);
		String exportFromPrecisionStr = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION);
		TemporalPrecisionEnum exportFromPrecision = exportFromPrecisionStr == null ? null
				: TemporalPrecisionEnum.valueOf(exportFromPrecisionStr);

		if (exportFrom != null && exportFromPrecision != null)
		{
			ParameterComponent param = new ParameterComponent();
			param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
					.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM);
			param.setValue(new DateTimeType(exportFrom, exportFromPrecision));
			return param;
		}
		else
		{
			logger.warn("Export from not specified, export from date unbounded");
			return null;
		}
	}

	private ParameterComponent exportToParameter(DelegateExecution execution)
	{
		Date exportTo = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO);

		ParameterComponent param = new ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);
		param.setValue(new InstantType(exportTo));
		return param;
	}
}
