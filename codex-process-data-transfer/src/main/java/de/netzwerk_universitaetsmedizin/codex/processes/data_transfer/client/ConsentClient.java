package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.List;

public interface ConsentClient
{
	List<String> getConsentOidsFor(String dicPseudonym);
}
