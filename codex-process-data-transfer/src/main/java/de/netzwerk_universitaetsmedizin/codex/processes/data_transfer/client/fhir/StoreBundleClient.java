package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.rest.api.Constants;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;

public class StoreBundleClient extends AbstractComplexFhirClient
{
	public StoreBundleClient(DataStoreClient dataClient, DataLogger dataLogger)
	{
		super(dataClient, dataLogger);
	}

	@Override
	public void storeBundle(Bundle bundle)
	{
		dataClient.getGenericFhirClient().create().resource(bundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();
	}
}
