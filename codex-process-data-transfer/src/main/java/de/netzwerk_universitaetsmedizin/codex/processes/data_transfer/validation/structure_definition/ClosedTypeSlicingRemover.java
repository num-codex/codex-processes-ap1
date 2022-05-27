package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition;

import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.DiscriminatorType;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionSlicingComponent;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionSlicingDiscriminatorComponent;
import org.hl7.fhir.r4.model.ElementDefinition.SlicingRules;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Closed type slicings result in error from the snapshot generator.
 */
public class ClosedTypeSlicingRemover implements StructureDefinitionModifier
{
	private static final Logger logger = LoggerFactory.getLogger(ClosedTypeSlicingRemover.class);

	@Override
	public StructureDefinition modify(StructureDefinition sd)
	{
		sd.getDifferential().getElement().stream().filter(ElementDefinition::hasSlicing).forEach(e ->
		{
			ElementDefinitionSlicingComponent slicing = e.getSlicing();
			if (SlicingRules.OPEN.equals(slicing.getRules()) && slicing.getDiscriminator().size() == 1)
			{
				ElementDefinitionSlicingDiscriminatorComponent discriminator = slicing.getDiscriminator().get(0);
				if (DiscriminatorType.TYPE.equals(discriminator.getType()) && "$this".equals(discriminator.getPath()))
				{
					logger.warn(
							"Removing Type slicing with slicing.rules != closed validation rule with id {} in StructureDefinition {}|{}",
							e.getId(), sd.getUrl(), sd.getVersion());

					e.setSlicing(null);
				}
			}
		});

		return sd;
	}
}