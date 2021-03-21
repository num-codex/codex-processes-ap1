package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.Optional;

public interface FttpClient
{
	String DIC_PSEUDONYM_PATTERN_STRING = "([^/]+)/([^/]+)";
	
	/**
	 * @param dicSourceAndPseudonym
	 *            not <code>null</code>
	 * @return
	 */
	Optional<String> getCrrPseudonym(String dicSourceAndPseudonym);
}
