package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.support.IValidationSupport;

public class BundleValidatorFactoryImpl implements BundleValidatorFactory, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(BundleValidatorFactoryImpl.class);

	private final boolean validationEnabled;
	private final ValidationPackageManager validationPackageManager;
	private final ValidationPackageIdentifier validationPackageIdentifier;

	private IValidationSupport validationSupport;
	private ValidationPackageWithDepedencies packageWithDependencies;

	public BundleValidatorFactoryImpl(boolean validationEnabled, ValidationPackageManager validationPackageManager,
			ValidationPackageIdentifier validationPackageIdentifier)
	{
		this.validationEnabled = validationEnabled;
		this.validationPackageManager = validationPackageManager;
		this.validationPackageIdentifier = validationPackageIdentifier;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(validationPackageManager, "validationPackageManager");
		Objects.requireNonNull(validationPackageIdentifier, "validationPackageIdentifier");
	}

	@Override
	public boolean isEnabled()
	{
		return validationEnabled;
	}

	@Override
	public void init()
	{
		if (validationSupport != null)
			return;

		logger.info("Downloading FHIR validation package {} and dependencies", validationPackageIdentifier.toString());
		packageWithDependencies = validationPackageManager.downloadPackageWithDependencies(validationPackageIdentifier);

		logger.info("Expanding ValueSets and generating StructureDefinition snapshots");
		validationSupport = validationPackageManager
				.expandValueSetsAndGenerateStructureDefinitionSnapshots(packageWithDependencies);
	}

	@Override
	public Optional<BundleValidator> create()
	{
		if (validationPackageManager == null || validationSupport == null)
			return Optional.empty();
		else
			return Optional
					.of(validationPackageManager.createBundleValidator(validationSupport, packageWithDependencies));
	}
}
