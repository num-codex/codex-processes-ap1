package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;

public class NonValidatingValidationSupport implements IValidationSupport
{
	private static final Logger logger = LoggerFactory.getLogger(NonValidatingValidationSupport.class);

	private final FhirContext fhirContext;
	private final List<String> notValidatedCodeSystemSystems = new ArrayList<>();

	public NonValidatingValidationSupport(FhirContext fhirContext, String... notValidatedCodeSystemSystems)
	{
		this(fhirContext, Arrays.asList(notValidatedCodeSystemSystems));
	}

	public NonValidatingValidationSupport(FhirContext fhirContext, Collection<String> notValidatedCodeSystemSystems)
	{
		this.fhirContext = fhirContext;

		if (notValidatedCodeSystemSystems != null)
			this.notValidatedCodeSystemSystems.addAll(notValidatedCodeSystemSystems);
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
	}

	@Override
	public CodeValidationResult validateCode(ValidationSupportContext theValidationSupportContext,
			ConceptValidationOptions theOptions, String system, String code, String display, String valueSetUrl)
	{
		if (notValidatedCodeSystemSystems.contains(system))
		{
			logger.warn("Not validating code {} from system {} for valueSet {}", code, system, valueSetUrl);
			return new CodeValidationResult().setCode(code).setCodeSystemName(system).setDisplay(display);
		}

		else
			return null;
	}

	@Override
	public boolean isCodeSystemSupported(ValidationSupportContext theValidationSupportContext, String system)
	{
		return notValidatedCodeSystemSystems.contains(system);
	}
}
