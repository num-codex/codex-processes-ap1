package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.List;

import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.context.support.IValidationSupport;

public interface ValidationPackageManager
{
	/**
	 * Downloads the given FHIR package and all its dependencies.
	 * 
	 * @param name
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @return unmodifiable list of {@link ValidationPackage}s
	 */
	default List<ValidationPackage> downloadPackageWithDependencies(String name, String version)
	{
		return downloadPackageWithDependencies(new ValidationPackageIdentifier(name, version));
	}

	/**
	 * Downloads the given FHIR package and all its dependencies.
	 * 
	 * @param name
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @return unmodifiable list of {@link ValidationPackage}s
	 */
	List<ValidationPackage> downloadPackageWithDependencies(ValidationPackageIdentifier identifier);

	/**
	 * Will try to generate snapshots for all {@link StructureDefinition}s and expand all {@link ValueSet}s, before
	 * returning a {@link IValidationSupport}.
	 * 
	 * @param validationPackages
	 *            not <code>null</code>
	 * @return validation support for the validator
	 */
	IValidationSupport expandValueSetsAndGenerateStructureDefinitionSnapshots(
			List<ValidationPackage> validationPackages);

	/**
	 * @param validationSupport
	 *            not <code>null</code>
	 * @return {@link BundleValidator} for the given {@link IValidationSupport}
	 */
	BundleValidator createBundleValidator(IValidationSupport validationSupport);

	/**
	 * Downloads the given FHIR package and all its dependencies. Will try to generate snapshots for all
	 * {@link StructureDefinition}s and expand all {@link ValueSet}s, before returning a {@link BundleValidator}.
	 * 
	 * @param name
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @return {@link BundleValidator} for the specified FHIR package
	 */
	default BundleValidator createBundleValidator(String name, String version)
	{
		return createBundleValidator(new ValidationPackageIdentifier(name, version));
	}

	/**
	 * Downloads the given FHIR package and all its dependencies. Will try to generate snapshots for all
	 * {@link StructureDefinition}s and expand all {@link ValueSet}s, before returning a {@link BundleValidator}.
	 * 
	 * @param identifier
	 *            not <code>null</code>
	 * @return {@link BundleValidator} for the specified FHIR package
	 */
	BundleValidator createBundleValidator(ValidationPackageIdentifier identifier);
}