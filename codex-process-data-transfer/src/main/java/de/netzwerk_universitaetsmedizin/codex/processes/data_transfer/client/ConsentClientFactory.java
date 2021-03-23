package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsentClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(ConsentClientFactory.class);

	private static final String BASE_OID = "2.16.840.1.113883.3.1937.777";

	public ConsentClient getConsentClient()
	{
		return new ConsentClient()
		{
			@Override
			public List<String> getConsentOidsFor(String dicSourceAndPseudonym)
			{
				logger.warn("Returning 'all allowed' OIDs for DIC pseudonym {}", dicSourceAndPseudonym);

				return Arrays.asList(BASE_OID + ".24.5.1.1", BASE_OID + ".24.5.1.34", BASE_OID + ".24.5.1.37");
			}
		};
	}
}
