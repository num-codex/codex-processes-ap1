package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import java.util.stream.Stream;

import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class ContinueSendProcessWithError extends AbstractTaskMessageSend
{
	private static final Logger logger = LoggerFactory.getLogger(ContinueSendProcessWithError.class);

	public ContinueSendProcessWithError(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected void sendTask(Target target, String instantiatesUri, String messageName, String businessKey,
			String profile, Stream<ParameterComponent> additionalInputParameters)
	{

		// TODO implement continue send with error
		logger.debug("implement continue send with error");

		super.sendTask(target, instantiatesUri, messageName, businessKey, profile, additionalInputParameters);
	}
}
