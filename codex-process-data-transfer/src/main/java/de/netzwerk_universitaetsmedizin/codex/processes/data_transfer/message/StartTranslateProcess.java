package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY_URL;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DTS_NOT_REACHABLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NUM_PARENT_ORGANIZATION_IDENTIFIER;

import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.constants.NamingSystems;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;

public class StartTranslateProcess extends AbstractTaskMessageSend
{
	private static final Coding DTS_ROLE = new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DTS", null);

	private final String dtsIdentifierValue;

	public StartTranslateProcess(ProcessPluginApi api, String dtsIdentifierValue)
	{
		super(api);

		this.dtsIdentifierValue = dtsIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dtsIdentifierValue, "dtsIdentifierValue");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws Exception
	{
		Endpoint endpoint = api.getEndpointProvider()
				.getEndpoint(NUM_PARENT_ORGANIZATION_IDENTIFIER, dtsIdentifierValue, DTS_ROLE).get();
		String endpointIdentifierValue = NamingSystems.EndpointIdentifier.findFirst(endpoint).map(Identifier::getValue)
				.get();
		String endpointAddress = endpoint.getAddress();

		Target target = variables.createTarget(dtsIdentifierValue, endpointIdentifierValue, endpointAddress);
		variables.setTarget(target);

		super.doExecute(execution, variables);
	}

	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution, Variables variables)
	{
		return Stream.of(pseudonymParameter(execution), dataReferenceParameter(execution));
	}

	private ParameterComponent pseudonymParameter(DelegateExecution execution)
	{
		String pseudonym = ((PatientReference) execution.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE))
				.getIdentifier().getValue();

		Objects.requireNonNull(pseudonym, "pseudonym");

		ParameterComponent param = new ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM);
		param.setValue(new Identifier().setSystem(ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM)
				.setValue(pseudonym));
		return param;
	}

	private ParameterComponent dataReferenceParameter(DelegateExecution execution)
	{
		String binaryReference = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_BINARY_URL);

		Objects.requireNonNull(binaryReference, "binaryReference");

		ParameterComponent param = new ParameterComponent();
		param.getType().addCoding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE);
		param.setValue(new Reference().setReference(binaryReference));
		return param;
	}

	@Override
	protected void handleSendTaskError(DelegateExecution execution, Variables variables, Exception exception,
			String errorMessage)
	{
		throw new BpmnError(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_DTS_NOT_REACHABLE,
				"Error while sending Task to GTH: " + exception.getMessage());
	}
}
