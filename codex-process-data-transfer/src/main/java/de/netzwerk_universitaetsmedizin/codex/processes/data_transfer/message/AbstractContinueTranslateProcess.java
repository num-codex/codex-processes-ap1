package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NUM_PARENT_ORGANIZATION_IDENTIFIER;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.constants.NamingSystems;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;

public abstract class AbstractContinueTranslateProcess extends AbstractTaskMessageSend
{
	private static final Coding DTS_ROLE = new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DTS", null);

	private final String dtsIdentifierValue;

	public AbstractContinueTranslateProcess(ProcessPluginApi api, String dtsIdentifierValue)
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
		Endpoint targetEndpoint = api.getEndpointProvider()
				.getEndpoint(NUM_PARENT_ORGANIZATION_IDENTIFIER, dtsIdentifierValue, DTS_ROLE).get();
		String targetEndpointIdentifierValue = NamingSystems.EndpointIdentifier.findFirst(targetEndpoint)
				.map(Identifier::getValue).get();
		Target target = variables.createTarget(dtsIdentifierValue, targetEndpointIdentifierValue,
				targetEndpoint.getAddress());
		variables.setTarget(target);

		super.doExecute(execution, variables);
	}
}
