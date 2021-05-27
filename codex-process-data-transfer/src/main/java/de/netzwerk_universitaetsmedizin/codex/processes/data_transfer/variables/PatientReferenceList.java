package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PatientReferenceList
{
	private final List<String> identifiers = new ArrayList<>();
	private final List<String> absoluteUrls = new ArrayList<>();

	@JsonCreator
	public PatientReferenceList(@JsonProperty("identifiers") Collection<String> identifiers,
			@JsonProperty("absoluteUrls") Collection<String> absoluteUrls)
	{
		if (identifiers != null)
			this.identifiers.addAll(identifiers);

		if (absoluteUrls != null)
			this.absoluteUrls.addAll(absoluteUrls);
	}

	public List<String> getIdentifiers()
	{
		return identifiers;
	}

	public List<String> getAbsoluteUrls()
	{
		return absoluteUrls;
	}
}
