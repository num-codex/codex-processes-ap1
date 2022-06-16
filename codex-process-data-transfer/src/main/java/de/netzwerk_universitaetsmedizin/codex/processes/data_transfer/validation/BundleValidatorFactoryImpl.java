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

	private final ValidationPackageManager validationPackageManager;
	private final ValidationPackageIdentifier validationPackageIdentifier;

	private IValidationSupport validationSupport;

	public BundleValidatorFactoryImpl(ValidationPackageManager validationPackageManager,
			ValidationPackageIdentifier validationPackageIdentifier)
	{
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
	public void init()
	{
		if (validationSupport != null)
			return;

		logger.info("Downloading FHIR validation package {} and dependencies", validationPackageIdentifier.toString());
		ValidationPackageWithDepedencies packageWithDependencies = validationPackageManager
				.downloadPackageWithDependencies(validationPackageIdentifier);

		logger.info("Expanding ValueSets and generating StructureDefinition snapshots");
		validationSupport = validationPackageManager
				.expandValueSetsAndGenerateStructureDefinitionSnapshots(packageWithDependencies);
	}

	@Override
	public Optional<BundleValidator> create()
	{
		return Optional.ofNullable(validationSupport).map(validationPackageManager::createBundleValidator);
	}
}
