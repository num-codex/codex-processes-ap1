package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set;

import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissingEntriesIncluder implements ValueSetModifier
{
	private static final Logger logger = LoggerFactory.getLogger(MissingEntriesIncluder.class);

	@Override
	public ValueSet modifyPostExpansion(ValueSet vsWithComposition, ValueSet vsWithExpansion)
	{
		if (vsWithExpansion == null)
			return null;
		if (!vsWithExpansion.hasExpansion())
			return vsWithExpansion;

		if (vsWithComposition.hasCompose()
				&& vsWithComposition.getCompose().getInclude().stream().anyMatch(ConceptSetComponent::hasConcept))
		{
			Set<String> expandedEntries = vsWithExpansion.getExpansion().getContains().stream()
					.map(c -> toEntry(c.getSystem(), c.getCode())).distinct().collect(Collectors.toSet());

			vsWithComposition.getCompose().getInclude().stream().filter(ConceptSetComponent::hasConcept)
					.forEach(include ->
					{
						String system = include.getSystem();
						String version = include.getVersion();

						include.getConcept().forEach(concept ->
						{
							if (!expandedEntries.contains(toEntry(system, concept.getCode()))
									&& !expandedEntries.contains(toEntry(system, concept.getCode())))
							{
								logger.info(
										"Adding missing concept to ValueSet {}|{}: system: '{}', version: '{}', code: '{}', display: '{}'",
										vsWithExpansion.getUrl(), vsWithExpansion.getVersion(), system, version,
										concept.getCode(), concept.getDisplay());

								vsWithExpansion.getExpansion()
										.addContains(new ValueSetExpansionContainsComponent().setSystem(system)
												.setVersion(version).setCode(concept.getCode())
												.setDisplay(concept.getDisplay()));
							}
						});
					});
		}

		return vsWithExpansion;
	}

	private String toEntry(String system, String code)
	{
		return system + code;
	}
}
