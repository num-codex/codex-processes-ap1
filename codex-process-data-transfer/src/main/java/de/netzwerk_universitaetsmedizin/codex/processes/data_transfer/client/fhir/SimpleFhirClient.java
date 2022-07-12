package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import java.util.Date;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DomainResource;

import ca.uhn.fhir.rest.api.Constants;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;

public class SimpleFhirClient extends AbstractFhirClient
{
	/**
	 * @param geccoClient
	 *            not <code>null</code>
	 * @param dataLogger
	 *            not <code>null</code>
	 */
	public SimpleFhirClient(GeccoClient geccoClient, DataLogger dataLogger)
	{
		super(geccoClient, dataLogger);
	}

	@Override
	public void storeBundle(Bundle bundle)
	{
		geccoClient.getGenericFhirClient().transaction().withBundle(bundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();
	}

	@Override
	public Stream<DomainResource> getNewData(String pseudonym, DateWithPrecision exportFrom, Date exportTo)
	{
		Bundle searchBundle = getSearchBundleWithPseudonym(pseudonym, exportFrom, exportTo);

		dataLogger.logData("Executing Search-Bundle", searchBundle);

		Bundle resultBundle = geccoClient.getGenericFhirClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		dataLogger.logData("Search-Bundle result", resultBundle);

		return distinctById(resultBundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof Bundle).map(r -> (Bundle) r)
				.flatMap(this::getDomainResources));
	}
}
