package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import java.util.Date;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.Constants;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;

public class SimpleFhirClient extends AbstractFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(SimpleFhirClient.class);

	/**
	 * @param geccoClient
	 *            not <code>null</code>
	 */
	public SimpleFhirClient(GeccoClient geccoClient)
	{
		super(geccoClient);
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

		if (logger.isDebugEnabled())
			logger.debug("Executing Search-Bundle: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(searchBundle));

		Bundle resultBundle = geccoClient.getGenericFhirClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(resultBundle));

		return distinctById(resultBundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof Bundle).map(r -> (Bundle) r)
				.flatMap(this::getDomainResources));
	}
}
