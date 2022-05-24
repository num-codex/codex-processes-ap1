package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiiModuleLabObservationLab10IdentifierRemover implements StructureDefinitionModifier
{
	private static final Logger logger = LoggerFactory.getLogger(MiiModuleLabObservationLab10IdentifierRemover.class);

	@Override
	public StructureDefinition modify(StructureDefinition sd)
	{
		if ("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab"
				.equals(sd.getUrl()) && "1.0".equals(sd.getVersion()))
		{
			Predicate<? super ElementDefinition> toRemove = e -> e.hasPath()
					&& e.getPath().startsWith("Observation.identifier");

			List<ElementDefinition> filteredRules = sd.getDifferential().getElement().stream().filter(toRemove.negate())
					.collect(Collectors.toList());

			logger.warn("Removing validation rules with ids {} in StructureDefinition {}|{}",
					sd.getDifferential().getElement().stream().filter(toRemove).map(ElementDefinition::getId)
							.collect(Collectors.joining(", ", "[", "]")),
					sd.getUrl(), sd.getVersion());

			sd.getDifferential().setElement(filteredRules);
		}

		return sd;
	}
}