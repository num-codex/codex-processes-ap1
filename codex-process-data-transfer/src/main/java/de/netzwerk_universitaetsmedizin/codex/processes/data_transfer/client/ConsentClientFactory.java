package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsentClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(ConsentClientFactory.class);

	private static final String BASE_OID = "2.16.840.1.113883.3.1937.777.24.5.3";
	private static final List<String> ALL_POLICIES = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 32, 33, 34, 35, 36)
			.mapToObj(i -> BASE_OID + "." + i).collect(Collectors.toList());

	public ConsentClient getConsentClient()
	{
		return new ConsentClient()
		{
			@Override
			public List<String> getConsentOidsForIdentifierReference(String dicSourceAndPseudonym)
			{
				logger.warn("Returning 'all allowed' OIDs for DIC pseudonym {}", dicSourceAndPseudonym);
				return ALL_POLICIES;
			}

			@Override
			public List<String> getConsentOidsForAbsoluteReference(String reference)
			{
				logger.warn("Returning 'all allowed' OIDs for absolute patient reference {}", reference);
				return ALL_POLICIES;
			}
		};
	}
}
