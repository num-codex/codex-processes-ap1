package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mandatory identifier on resources other then Patient not compatible with data protection rules and current
 * pseudonymization implementation.
 */
public class IdentifierRemover implements StructureDefinitionModifier
{
	private static final Logger logger = LoggerFactory.getLogger(IdentifierRemover.class);

	@Override
	public StructureDefinition modify(StructureDefinition sd)
	{
		String type = sd.getType();

		if (StructureDefinitionKind.RESOURCE.equals(sd.getKind()) && type != null
				&& !ResourceType.Patient.name().equals(type))
		{
			Predicate<? super ElementDefinition> toRemove = e -> e.hasPath()
					&& e.getPath().startsWith(type + ".identifier");

			List<ElementDefinition> filteredRules = sd.getDifferential().getElement().stream().filter(toRemove.negate())
					.collect(Collectors.toList());

			if (filteredRules.size() < sd.getDifferential().getElement().size())
			{
				logger.info("Removing validation rules with ids {} from StructureDefinition {}|{}",
						sd.getDifferential().getElement().stream().filter(toRemove).map(ElementDefinition::getId)
								.collect(Collectors.joining(", ", "[", "]")),
						sd.getUrl(), sd.getVersion());

				sd.getDifferential().setElement(filteredRules);
			}
		}

		return sd;
	}
}