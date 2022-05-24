package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;

public class ValidationSupportResources
{
	private final List<CodeSystem> codeSystems = new ArrayList<>();
	private final List<NamingSystem> namingSystems = new ArrayList<>();
	private final List<StructureDefinition> structureDefinitions = new ArrayList<>();
	private final List<ValueSet> valueSets = new ArrayList<>();

	public ValidationSupportResources(List<CodeSystem> codeSystems, List<NamingSystem> namingSystems,
			List<StructureDefinition> structureDefinitions, List<ValueSet> valueSets)
	{
		if (codeSystems != null)
			this.codeSystems.addAll(codeSystems);
		if (namingSystems != null)
			this.namingSystems.addAll(namingSystems);
		if (structureDefinitions != null)
			this.structureDefinitions.addAll(structureDefinitions);
		if (valueSets != null)
			this.valueSets.addAll(valueSets);
	}

	public List<CodeSystem> getCodeSystems()
	{
		return Collections.unmodifiableList(codeSystems);
	}

	public List<NamingSystem> getNamingSystems()
	{
		return Collections.unmodifiableList(namingSystems);
	}

	public List<StructureDefinition> getStructureDefinitions()
	{
		return Collections.unmodifiableList(structureDefinitions);
	}

	public List<ValueSet> getValueSets()
	{
		return Collections.unmodifiableList(valueSets);
	}
}
