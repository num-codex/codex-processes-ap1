package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NUM_PARENT_ORGANIZATION_IDENTIFIER;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.constants.NamingSystems;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;

public abstract class AbstractContinueSendProcess extends AbstractTaskMessageSend
{
	private static final Coding DIC_ROLE = new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DIC", null);

	public AbstractContinueSendProcess(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws Exception
	{
		Task task = variables.getStartTask();

		String organizationIdentifierValue = task.getRequester().getIdentifier().getValue();
		Endpoint endpoint = api.getEndpointProvider()
				.getEndpoint(NUM_PARENT_ORGANIZATION_IDENTIFIER, organizationIdentifierValue, DIC_ROLE).get();
		String endpointIdentifierValue = NamingSystems.EndpointIdentifier.findFirst(endpoint).map(Identifier::getValue)
				.get();
		String endpointAddress = endpoint.getAddress();

		Target target = variables.createTarget(organizationIdentifierValue, endpointIdentifierValue, endpointAddress);
		variables.setTarget(target);

		super.doExecute(execution, variables);
	}
}
