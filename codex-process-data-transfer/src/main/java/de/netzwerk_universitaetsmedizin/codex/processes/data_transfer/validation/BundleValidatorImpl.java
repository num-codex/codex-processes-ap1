package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.Objects;

import org.highmed.dsf.fhir.validation.ResourceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.validation.ValidationResult;

public class BundleValidatorImpl implements BundleValidator, InitializingBean
{
	private final ResourceValidator delegate;

	public BundleValidatorImpl(ResourceValidator delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ValidationResult validate(Resource resource)
	{
		Objects.requireNonNull(resource, "resource");

		return delegate.validate(resource);
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
