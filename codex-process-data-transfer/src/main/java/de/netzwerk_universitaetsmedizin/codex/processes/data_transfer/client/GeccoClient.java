package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.nio.file.Path;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.GeccoFhirClient;

public interface GeccoClient
{
	String getServerBase();

	FhirContext getFhirContext();

	void testConnection();

	GeccoFhirClient getFhirClient();

	Path getSearchBundleOverride();

	IGenericClient getGenericFhirClient();

	String getLocalIdentifierValue();

	boolean shouldUseChainedParameterNotLogicalReference();
}
