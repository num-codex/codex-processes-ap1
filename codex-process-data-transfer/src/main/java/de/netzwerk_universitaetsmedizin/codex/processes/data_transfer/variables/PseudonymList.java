package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PseudonymList
{
	private final List<String> pseudonyms = new ArrayList<>();

	@JsonCreator
	public PseudonymList(@JsonProperty("pseudonyms") Collection<String> pseudonyms)
	{
		if (pseudonyms != null)
			this.pseudonyms.addAll(pseudonyms);
	}

	public List<String> getPseudonyms()
	{
		return pseudonyms;
	}
}
