package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;

public class BundleValidatorImpl implements BundleValidator
{
	private static final Logger logger = LoggerFactory.getLogger(BundleValidatorImpl.class);

	private final FhirContext fhirContext;
	private final ResourceValidator delegate;
	private final Set<String> expectedStructureDefinitionUrls;
	private final Set<String> expectedStructureDefinitionUrlsWithVersion;

	public BundleValidatorImpl(FhirContext fhirContext, ValidationPackageWithDepedencies packageWithDependencies,
			ResourceValidator delegate)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");

		Set<StructureDefinition> sds = Objects.requireNonNull(packageWithDependencies, "packageWithDependencies")
				.getValidationSupportResources().getStructureDefinitions().stream()
				.flatMap(sd -> Stream.concat(Stream.of(sd),
						packageWithDependencies.getStructureDefinitionDependencies(sd).stream()))
				.filter(sd -> StructureDefinitionKind.RESOURCE.equals(sd.getKind()))
				.filter(sd -> !sd.hasAbstract() || !sd.getAbstract()).filter(StructureDefinition::hasUrl)
				.collect(Collectors.toSet());

		expectedStructureDefinitionUrls = sds.stream().map(StructureDefinition::getUrl).collect(Collectors.toSet());
		expectedStructureDefinitionUrlsWithVersion = sds.stream().filter(StructureDefinition::hasVersion)
				.map(sd -> sd.getUrl() + "|" + sd.getVersion()).collect(Collectors.toSet());

		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ValidationResult validate(Resource resource)
	{
		Objects.requireNonNull(resource, "resource");

		Set<String> profiles = resource.getMeta().getProfile().stream().map(CanonicalType::getValue)
				.collect(Collectors.toSet());

		if (!Collections.disjoint(profiles, expectedStructureDefinitionUrls)
				|| !Collections.disjoint(profiles, expectedStructureDefinitionUrlsWithVersion))
		{
			// at least one supported profile claimed
			return delegate.validate(resource);
		}
		else
		{
			SingleValidationMessage message = new SingleValidationMessage();
			message.setLocationString(resource.getResourceType().name() + ".meta.profile");

			String messageText;
			if (profiles.isEmpty())
				messageText = "No supported profile claimed";
			else
				messageText = "No supported profile claimed, profile" + (profiles.size() == 1 ? "" : "s") + " "
						+ profiles.stream().sorted().collect(Collectors.joining(", ", "[", "]")) + " not supported";

			message.setMessage(messageText);
			message.setSeverity(ResultSeverityEnum.ERROR);

			logger.debug("Supported profiles {}", expectedStructureDefinitionUrlsWithVersion.stream().sorted()
					.collect(Collectors.joining(", ", "[", "]")));

			return new ValidationResult(fhirContext, List.of(message));
		}
	}

	@Override
	public Bundle validate(Bundle bundle)
	{
		Objects.requireNonNull(bundle, "bundle");

		bundle.getEntry().stream().forEach(this::validateAndSetOutcome);
		return bundle;
	}

	private void validateAndSetOutcome(BundleEntryComponent entry)
	{
		if (entry.hasResource())
		{
			ValidationResult validationResult = validate(entry.getResource());
			entry.getResponse().setOutcome((OperationOutcome) validationResult.toOperationOutcome());
		}
	}
}
