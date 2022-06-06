package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.Optional;

public interface BundleValidatorFactory
{
	/**
	 * Initializes the {@link BundleValidatorFactory} by downloading all necessary FHIR implementation guides, expanding
	 * ValueSets and generating StructureDefinition snapshots.
	 */
	void init();

	/**
	 * @return {@link Optional#empty()} if this {@link BundleValidatorFactory} was not initialized
	 * @see BundleValidatorFactory#init()
	 */
	Optional<BundleValidator> create();
}
