package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import org.hl7.fhir.r4.model.Enumerations.BindingStrength;
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
	default ValidationPackageWithDepedencies downloadPackageWithDependencies(String name, String version)
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
	ValidationPackageWithDepedencies downloadPackageWithDependencies(ValidationPackageIdentifier identifier);

	/**
	 * Will try to generate snapshots for all {@link StructureDefinition}s of the root package and its dependencies,
	 * will try to expand all {@link ValueSet}s with binding strength {@link BindingStrength#EXTENSIBLE},
	 * {@link BindingStrength#PREFERRED} or {@link BindingStrength#REQUIRED} used by the {@link StructureDefinition} of
	 * the root package or their dependencies, before returning a {@link IValidationSupport}.
	 * 
	 * @param packageWithDependencies
	 *            not <code>null</code>
	 * @return validation support for the validator
	 */
	IValidationSupport expandValueSetsAndGenerateStructureDefinitionSnapshots(
			ValidationPackageWithDepedencies packageWithDependencies);

	/**
	 * @param validationSupport
	 *            not <code>null</code>
	 * @param packageWithDependencies
	 *            not <code>null</code>
	 * @return {@link BundleValidator} for the given {@link IValidationSupport} and
	 *         {@link ValidationPackageWithDepedencies}
	 */
	BundleValidator createBundleValidator(IValidationSupport validationSupport,
			ValidationPackageWithDepedencies packageWithDependencies);

	/**
	 * Downloads the given FHIR package and all its dependencies. Will try to generate snapshots for all
	 * {@link StructureDefinition}s of the specified (root) package and its dependencies, will try to expand all
	 * {@link ValueSet}s with binding strength {@link BindingStrength#EXTENSIBLE}, {@link BindingStrength#PREFERRED} or
	 * {@link BindingStrength#REQUIRED} used by the {@link StructureDefinition} of the specified (root) package or their
	 * dependencies, before returning a {@link IValidationSupport}.
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
	 * {@link StructureDefinition}s of the specified (root) package and its dependencies, will try to expand all
	 * {@link ValueSet}s with binding strength {@link BindingStrength#EXTENSIBLE}, {@link BindingStrength#PREFERRED} or
	 * {@link BindingStrength#REQUIRED} used by the {@link StructureDefinition} of the specified (root) package or their
	 * dependencies, before returning a {@link IValidationSupport}.
	 * 
	 * @param identifier
	 *            not <code>null</code>
	 * @return {@link BundleValidator} for the specified FHIR package
	 */
	BundleValidator createBundleValidator(ValidationPackageIdentifier identifier);
}