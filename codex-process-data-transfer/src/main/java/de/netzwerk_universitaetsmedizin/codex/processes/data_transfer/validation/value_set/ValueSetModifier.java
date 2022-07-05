package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set;

import org.hl7.fhir.r4.model.ValueSet;

public interface ValueSetModifier
{
	default ValueSet modifyPreExpansion(ValueSet vs)
	{
		return vs;
	}

	default ValueSet modifyPostExpansion(ValueSet vsWithComposition, ValueSet vsWithExpansion)
	{
		return vsWithExpansion;
	}
}
