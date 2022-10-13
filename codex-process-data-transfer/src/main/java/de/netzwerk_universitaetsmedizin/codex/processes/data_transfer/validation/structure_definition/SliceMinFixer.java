package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition;

import java.util.Objects;

import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HAPI snapshot generator adds bad min value to slices if min is not explicitly defined and slicing definition is not
 * part of the profile and path does not end with .value[x]
 */
public class SliceMinFixer implements StructureDefinitionModifier
{
	private static final Logger logger = LoggerFactory.getLogger(SliceMinFixer.class);

	@Override
	public StructureDefinition modify(StructureDefinition sd)
	{
		sd.getDifferential().getElement().stream()
				// slice with max but no min definition
				.filter(e -> e.hasSliceName() && !e.hasMin() && e.hasMax())
				// no fix needed for rules with path ending in .value[x]
				.filter(e -> e.hasPath() && !e.getPath().endsWith(".value[x]"))
				// matching slicing definition not part of this profile (defined in base)
				.filter(e -> !sd.getDifferential().getElement().stream()
						.anyMatch(e1 -> Objects.equals(e.getPath(), e1.getPath()) && e1.hasSlicing()))
				.forEach(e ->
				{
					logger.info("Adding min=0 to rule with id {} in StructureDefinition {}|{}", e.getId(), sd.getUrl(),
							sd.getVersion(), sd.getBaseDefinition(), sd.getDifferential().getElement().stream()
									.anyMatch(e1 -> Objects.equals(e.getPath(), e1.getPath()) && e1.hasSlicing()));
					e.setMin(0);
				});

		return sd;
	}
}
