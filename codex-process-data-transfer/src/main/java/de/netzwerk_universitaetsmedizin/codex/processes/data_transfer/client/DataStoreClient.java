package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.nio.file.Path;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClient;

public interface DataStoreClient
{
	String getServerBase();

	FhirContext getFhirContext();

	void testConnection();

	DataStoreFhirClient getFhirClient();

	Path getSearchBundleOverride();

	IGenericClient getGenericFhirClient();

	boolean shouldUseChainedParameterNotLogicalReference();
}
