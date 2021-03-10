package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYMS_LIST;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.Variables.SerializationDataFormats;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;

public class FindNewData extends AbstractServiceDelegate implements InitializingBean
{
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
		Date exportFrom = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM);
		String exportFromPrecisionStr = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION);
		TemporalPrecisionEnum exportFromPrecision = exportFromPrecisionStr == null ? null
				: TemporalPrecisionEnum.valueOf(exportFromPrecisionStr);
		Date exportTo = (Date) execution.getVariable(BPMN_EXECUTION_VARIABLE_EXPORT_TO);

		List<String> pseudonyms = searchForPseudonymsWithNewData(exportFrom, exportFromPrecision, exportTo);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYMS_LIST,
				Variables.objectValue(pseudonyms).serializationDataFormat(SerializationDataFormats.JSON).create());
		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET,
				TargetValues.create(Target.createUniDirectionalTarget(organizationProvider.getLocalIdentifierValue())));
	}

	/**
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportFromPrecision
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	private List<String> searchForPseudonymsWithNewData(Date exportFrom, TemporalPrecisionEnum exportFromPrecision,
			Date exportTo)
	{
		logger.debug("Searching for new data to transfer from {} with precision {} to {}", exportFrom,
				exportFromPrecision, exportTo);

		// TODO implement FHIR search

		// IGenericClient client = localFhirStoreClientFactory.createClient();
		// client.search().forResource(Patient.class).withProfile("profile").lastUpdated(new DateRangeParam().set)

		return Arrays.asList("1234567890");
	}
}
