package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
	 * @return validation package with dependencies
	 */
	default ValidationPackageWithDepedencies downloadPackageWithDependencies(String name, String version)
	{
		return downloadPackageWithDependencies(new ValidationPackageIdentifier(name, version));
	}

	/**
	 * Downloads the given FHIR package and all its dependencies.
	 *
	 * @param identifier
	 * @return validation package with dependencies
	 */
	ValidationPackageWithDepedencies downloadPackageWithDependencies(ValidationPackageIdentifier identifier);

	/**
	 * Downloads the given FHIR packages and all its dependencies.
	 *
	 * @param identifiers
	 * @return unmodifiable list of {@link ValidationPackageWithDepedencies}
	 */
	default List<ValidationPackageWithDepedencies> downloadPackagesWithDependencies(
			ValidationPackageIdentifier... identifiers)
	{
		return downloadPackagesWithDependencies(Arrays.asList(identifiers));
	}

	/**
	 * Downloads the given FHIR packages and all its dependencies.
	 *
	 * @param identifiers
	 * @return unmodifiable list of {@link ValidationPackageWithDepedencies}
	 */
	List<ValidationPackageWithDepedencies> downloadPackagesWithDependencies(
			Collection<? extends ValidationPackageIdentifier> identifiers);

	/**
	 * Will try to generate snapshots for all {@link StructureDefinition}s of the root package and its dependencies,
	 * will try to expand all {@link ValueSet}s with binding strength {@link BindingStrength#EXTENSIBLE},
	 * {@link BindingStrength#PREFERRED} or {@link BindingStrength#REQUIRED} used by the {@link StructureDefinition} of
	 * the root package or their dependencies, before returning a {@link IValidationSupport}.
	 *
	 * @param packagesWithDependencies
	 * @return validation support for the validator
	 */
	default IValidationSupport expandValueSetsAndGenerateStructureDefinitionSnapshots(
			ValidationPackageWithDepedencies... packagesWithDependencies)
	{
		return expandValueSetsAndGenerateStructureDefinitionSnapshots(Arrays.asList(packagesWithDependencies));
	}

	/**
	 * Will try to generate snapshots for all {@link StructureDefinition}s of the root package and its dependencies,
	 * will try to expand all {@link ValueSet}s with binding strength {@link BindingStrength#EXTENSIBLE},
	 * {@link BindingStrength#PREFERRED} or {@link BindingStrength#REQUIRED} used by the {@link StructureDefinition} of
	 * the root package or their dependencies, before returning a {@link IValidationSupport}.
	 *
	 * @param packagesWithDependencies
	 *            not <code>null</code>
	 * @return validation support for the validator
	 */
	IValidationSupport expandValueSetsAndGenerateStructureDefinitionSnapshots(
			Collection<? extends ValidationPackageWithDepedencies> packagesWithDependencies);

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
	 * @param validationSupport
	 *            not <code>null</code>
	 * @param packagesWithDependencies
	 * @return {@link BundleValidator} for the given {@link IValidationSupport} and
	 *         {@link ValidationPackageWithDepedencies}
	 */
	default BundleValidator createBundleValidator(IValidationSupport validationSupport,
			ValidationPackageWithDepedencies... packagesWithDependencies)
	{
		return createBundleValidator(validationSupport, Arrays.asList(packagesWithDependencies));
	}

	/**
	 * @param validationSupport
	 *            not <code>null</code>
	 * @param packagesWithDependencies
	 *            not <code>null</code>
	 * @return {@link BundleValidator} for the given {@link IValidationSupport} and
	 *         {@link ValidationPackageWithDepedencies}
	 */
	BundleValidator createBundleValidator(IValidationSupport validationSupport,
			Collection<? extends ValidationPackageWithDepedencies> packagesWithDependencies);

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