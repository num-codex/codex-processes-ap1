package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set;

import java.util.Map;

import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.context.FhirContext;

public class KdsMikrobiologieBugFixer implements ValueSetModifier
{

	private final Map<String, String> fixedValueSets = Map.of(
			"https://www.medizininformatik-initiative.de/fhir/modul-mikrobio/ValueSet/mii-vs-mikrobio-empfindlichkeit-phenotyp-loinc",
			"mii-vs-mikrobio-empfindlichkeit-phenotyp-loinc.json",
			"https://www.medizininformatik-initiative.de/fhir/modul-mikrobio/ValueSet/mii-vs-mikrobio-empfindlichkeit-genotyp-loinc",
			"mii-vs-mikrobio-empfindlichkeit.json",
			"https://www.medizininformatik-initiative.de/fhir/modul-mikrobio/ValueSet/mii-vs-mikrobio-mre-klasse-snomedct",
			"mii-vs-mikrobio-mre-klasse-snomedct.json");

	@Override
	public ValueSet modifyPreExpansion(ValueSet vs)
	{

		if (vs.getUrl() != null && vs.getVersion() != null && vs.getVersion().equals("2024.0.0"))
		{
			String fileName = fixedValueSets.get(vs.getUrl());
			if (fileName != null)
			{
				return (ValueSet) FhirContext.forR4().newJsonParser()
						.parseResource(getClass().getResourceAsStream("/bugfix/fhir/ValueSet/" + fileName));
			}
		}

		return vs;
	}
}
