package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mandatory identifier on Observation not compatible with data protection rules and current pseudonymization
 * implementation.
 */
public class ObservationIdentifierRemover implements StructureDefinitionModifier
{
	private static final Logger logger = LoggerFactory.getLogger(ObservationIdentifierRemover.class);

	@Override
	public StructureDefinition modify(StructureDefinition sd)
	{
		if ("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"
				.equals(sd.getUrl())
				|| "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-gas-panel"
						.equals(sd.getUrl()))
		{
			Predicate<? super ElementDefinition> toRemove = e -> e.hasPath()
					&& e.getPath().startsWith("Observation.identifier");

			List<ElementDefinition> filteredRules = sd.getDifferential().getElement().stream().filter(toRemove.negate())
					.collect(Collectors.toList());

			logger.warn("Removing validation rules with ids {} from StructureDefinition {}|{}",
					sd.getDifferential().getElement().stream().filter(toRemove).map(ElementDefinition::getId)
							.collect(Collectors.joining(", ", "[", "]")),
					sd.getUrl(), sd.getVersion());

			sd.getDifferential().setElement(filteredRules);
		}

		return sd;
	}
}