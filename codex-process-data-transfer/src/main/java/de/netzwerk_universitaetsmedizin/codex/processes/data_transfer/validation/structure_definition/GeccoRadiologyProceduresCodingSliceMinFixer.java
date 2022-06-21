package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition;

import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HAPI snapshot generator adds min=1, if no min value specified in the parent StructureDefinition.
 */
public class GeccoRadiologyProceduresCodingSliceMinFixer implements StructureDefinitionModifier
{
	private static final Logger logger = LoggerFactory.getLogger(GeccoRadiologyProceduresCodingSliceMinFixer.class);

	@Override
	public StructureDefinition modify(StructureDefinition sd)
	{
		if ("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/radiology-procedures"
				.equals(sd.getUrl()) && "1.0.5".equals(sd.getVersion()))
		{
			sd.getDifferential().getElement().stream().filter(
					e -> "Procedure.code.coding".equals(e.getPath()) && e.hasMax() && e.hasSliceName() && !e.hasMin())
					.forEach(e ->
					{
						logger.warn("Adding min=0 to rule with id {} in StructureDefinition {}|{}", e.getId(),
								sd.getUrl(), sd.getVersion());
						e.setMin(0);
					});
		}

		return sd;
	}
}
