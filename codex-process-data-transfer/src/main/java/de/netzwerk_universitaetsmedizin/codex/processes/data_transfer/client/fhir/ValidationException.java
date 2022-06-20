package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

public class ValidationException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	private final Resource resource;
	private final OperationOutcome outcome;

	public ValidationException(Resource resource, OperationOutcome outcome)
	{
		super("Validation failed for " + resource.getResourceType().name() + " with id "
				+ resource.getIdElement().getValue());

		this.resource = resource;
		this.outcome = outcome;
	}

	public Resource getResource()
	{
		return resource;
	}

	public OperationOutcome getOutcome()
	{
		return outcome;
	}

	public Bundle getResultBundle()
	{
		/*
		 * TODO should be the transaction-result bundle directly from the FHIR store, creating a bundle with a single
		 * entry as a workaround
		 */
		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTIONRESPONSE);
		BundleEntryComponent entry = bundle.addEntry();
		entry.setFullUrl(resource.getIdElement().getValue());
		entry.getResponse().setOutcome(outcome);
		return bundle;
	}
}
