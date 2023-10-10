package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE_LIST;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceList;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceListValues;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class FindNewData extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FindNewData.class);

	private final DataStoreClientFactory dataClientFactory;

	public FindNewData(ProcessPluginApi api, DataStoreClientFactory dataClientFactory)
	{
		super(api);

		this.dataClientFactory = dataClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dataClientFactory, "dataClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Optional<DateWithPrecision> exportFrom = getExportFrom(execution, variables);
		Date exportTo = new Date();

		PatientReferenceList patientReferenceList = searchForPatientReferencesWithNewData(exportFrom.orElse(null),
				exportTo);

		variables.setDate(BPMN_EXECUTION_VARIABLE_EXPORT_FROM, exportFrom.orElse(null));
		variables.setString(BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION,
				exportFrom.map(DateWithPrecision::getPrecision).map(Enum::name).orElse(null));

		variables.setDate(BPMN_EXECUTION_VARIABLE_EXPORT_TO, exportTo);
		variables.setDate(BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO, exportTo);
		variables.setVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE_LIST,
				PatientReferenceListValues.create(patientReferenceList));

		variables.setTarget(
				variables.createTarget(api.getOrganizationProvider().getLocalOrganizationIdentifierValue().get(),
						api.getEndpointProvider().getLocalEndpointIdentifierValue().get(),
						api.getEndpointProvider().getLocalEndpointAddress()));
	}

	protected Optional<DateWithPrecision> getExportFrom(DelegateExecution execution, Variables variables)
	{
		Date lastExportTo = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO);

		if (lastExportTo != null)
			return Optional.of(new DateWithPrecision(lastExportTo, TemporalPrecisionEnum.MILLI));

		Optional<DateTimeType> exportFromInput = getExportFromInput(variables.getStartTask());
		return exportFromInput.map(d -> new DateWithPrecision(d.getValue(), d.getPrecision()));
	}

	private Optional<DateTimeType> getExportFromInput(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM, DateTimeType.class).findFirst();
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(ParameterComponent::hasValue).filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}

	protected PatientReferenceList searchForPatientReferencesWithNewData(DateWithPrecision exportFrom, Date exportTo)
	{
		logger.debug("Searching for new data to transfer from {} with precision {} to {}", exportFrom,
				exportFrom == null ? null : exportFrom.getPrecision(), exportTo);

		DataStoreFhirClient fhirClient = dataClientFactory.getDataStoreClient().getFhirClient();

		PatientReferenceList references = fhirClient.getPatientReferencesWithNewData(exportFrom, exportTo);

		logger.info("Found {} patient{} with changes to transport", references.getReferences().size(),
				references.getReferences().size() != 1 ? "s" : "");

		return references;
	}
}
