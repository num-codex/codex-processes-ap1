package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import org.highmed.dsf.fhir.validation.ResourceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;

public interface BundleValidator extends ResourceValidator
{
	/**
	 * Validated all bundle entries with a <code>entry.resource</code>. The validation result will be added as a
	 * {@link OperationOutcome} resource to the corresponding <code>entry.response.outcome</code> property.
	 *
	 * @param bundle
	 *            not <code>null</code>
	 * @return given bundle with added <code>entry.response.outcome</code> properties
	 */
	Bundle validate(Bundle bundle);
}
