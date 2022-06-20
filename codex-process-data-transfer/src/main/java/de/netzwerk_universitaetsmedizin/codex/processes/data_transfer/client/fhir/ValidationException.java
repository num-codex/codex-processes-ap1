package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.OperationOutcome;

public class ValidationException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	private final String resourceType;
	private final String fullUrl;
	private final OperationOutcome outcome;

	public ValidationException(String resourceType, String fullUrl, OperationOutcome outcome)
	{
		super("Validation failed for " + resourceType + " with id " + fullUrl);

		this.resourceType = resourceType;
		this.fullUrl = fullUrl;
		this.outcome = outcome;
	}

	public String getResourceType()
	{
		return resourceType;
	}

	public String getFullUrl()
	{
		return fullUrl;
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
		entry.setFullUrl(fullUrl);
		entry.getResponse().setOutcome(outcome);
		return bundle;
	}
}
