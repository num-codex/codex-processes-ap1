package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import java.nio.file.Path;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.HapiFhirClientFactory;

public interface FhirClientBuilder
{
	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param clientFactory
	 *            not <code>null</code>
	 * @param searchBundleOverride
	 *            may be <code>null</code>
	 * @return
	 */
	FhirClient build(FhirContext fhirContext, HapiFhirClientFactory clientFactory, Path searchBundleOverride);
}
