package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PatientReferenceList
{
	private final List<PatientReference> references = new ArrayList<>();

	@JsonCreator
	public PatientReferenceList(@JsonProperty("references") Collection<PatientReference> references)
	{
		if (references != null)
			this.references.addAll(references);
	}

	public List<PatientReference> getReferences()
	{
		return Collections.unmodifiableList(references);
	}
}
