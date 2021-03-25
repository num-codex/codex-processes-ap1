package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYMS_LIST;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PseudonymList;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PseudonymListValues;

public class FindNewData extends AbstractServiceDelegate implements InitializingBean
{
	@SuppressWarnings("serial")
	private static class DateWithPrecision extends Date
	{
		private final TemporalPrecisionEnum precision;

		DateWithPrecision(Date exportFrom, TemporalPrecisionEnum precision)
		{
			super(exportFrom.getTime());
			this.precision = precision;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(FindNewData.class);

	private final OrganizationProvider organizationProvider;
	private final FhirClientFactory localFhirStoreClientFactory;

	public FindNewData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			OrganizationProvider organizationProvider, FhirClientFactory localFhirStoreClientFactory)
	{
		super(clientProvider, taskHelper);

		this.organizationProvider = organizationProvider;
		this.localFhirStoreClientFactory = localFhirStoreClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(localFhirStoreClientFactory, "localFhirStoreClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Optional<DateWithPrecision> exportFrom = getExportFrom(execution);
		Date exportTo = new Date();

		PseudonymList pseudonyms = searchForPseudonymsWithNewData(exportFrom.orElse(null), exportTo);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM, Variables.dateValue(exportFrom.orElse(null)));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION,
				Variables.stringValue(exportFrom.map(p -> p.precision).map(Enum::name).orElse(null)));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO, Variables.dateValue(exportTo));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO, Variables.dateValue(exportTo));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYMS_LIST, PseudonymListValues.create(pseudonyms));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET,
				TargetValues.create(Target.createUniDirectionalTarget(organizationProvider.getLocalIdentifierValue())));
	}

	protected Optional<DateWithPrecision> getExportFrom(DelegateExecution execution)
	{
		Date lastExportTo = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO);

		if (lastExportTo != null)
			return Optional.of(new DateWithPrecision(lastExportTo, TemporalPrecisionEnum.MILLI));

		Optional<DateTimeType> exportFromInput = getExportFromInput(getCurrentTaskFromExecutionVariables());
		return exportFromInput.map(d -> new DateWithPrecision(d.getValue(), d.getPrecision()));
	}

	private Optional<DateTimeType> getExportFromInput(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM, DateTimeType.class).findFirst();
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}

	protected PseudonymList searchForPseudonymsWithNewData(DateWithPrecision exportFrom, Date exportTo)
	{
		logger.debug("Searching for new data to transfer from {} with precision {} to {}", exportFrom,
				exportFrom.precision, exportTo);

		// TODO implement FHIR search

		// IGenericClient client = localFhirStoreClientFactory.createClient();
		// client.search().forResource(Patient.class).withProfile("profile").lastUpdated(new DateRangeParam().set)

		return new PseudonymList(Arrays.asList("source/original"));
	}
}
