package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.Enumerations.BindingStrength;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;
import dev.dsf.fhir.validation.ValidationSupportWithCustomResources;
import dev.dsf.fhir.validation.ValueSetExpander;
import jakarta.ws.rs.WebApplicationException;

public class ValidationPackageManagerImpl implements InitializingBean, ValidationPackageManager
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationPackageManagerImpl.class);

	public static final List<ValidationPackageIdentifier> DEFAULT_NO_PACKAGE_DOWNLOAD_LIST = List
			.of(new ValidationPackageIdentifier("hl7.fhir.r4.core", "4.0.1"));

	public static final EnumSet<BindingStrength> DEFAULT_VALUE_SET_BINDING_STRENGTHS = EnumSet
			.allOf(BindingStrength.class);

	private final ValidationPackageClient validationPackageClient;
	private final ValueSetExpansionClient valueSetExpansionClient;

	private final ObjectMapper mapper;
	private final FhirContext fhirContext;

	private final BiFunction<FhirContext, IValidationSupport, SnapshotGenerator> internalSnapshotGeneratorFactory;
	private final BiFunction<FhirContext, IValidationSupport, ValueSetExpander> internalValueSetExpanderFactory;

	private final List<ValidationPackageIdentifier> noDownloadPackages = new ArrayList<>();
	private final EnumSet<BindingStrength> valueSetBindingStrengths;

	public ValidationPackageManagerImpl(ValidationPackageClient validationPackageClient,
			ValueSetExpansionClient valueSetExpansionClient, ObjectMapper mapper, FhirContext fhirContext,
			BiFunction<FhirContext, IValidationSupport, SnapshotGenerator> internalSnapshotGeneratorFactory,
			BiFunction<FhirContext, IValidationSupport, ValueSetExpander> internalValueSetExpanderFactory)
	{
		this(validationPackageClient, valueSetExpansionClient, mapper, fhirContext, internalSnapshotGeneratorFactory,
				internalValueSetExpanderFactory, DEFAULT_NO_PACKAGE_DOWNLOAD_LIST, DEFAULT_VALUE_SET_BINDING_STRENGTHS);
	}

	public ValidationPackageManagerImpl(ValidationPackageClient validationPackageClient,
			ValueSetExpansionClient valueSetExpansionClient, ObjectMapper mapper, FhirContext fhirContext,
			BiFunction<FhirContext, IValidationSupport, SnapshotGenerator> internalSnapshotGeneratorFactory,
			BiFunction<FhirContext, IValidationSupport, ValueSetExpander> internalValueSetExpanderFactory,
			Collection<ValidationPackageIdentifier> noDownloadPackages,
			EnumSet<BindingStrength> valueSetBindingStrengths)
	{
		this.validationPackageClient = validationPackageClient;
		this.valueSetExpansionClient = valueSetExpansionClient;
		this.mapper = mapper;
		this.fhirContext = fhirContext;
		this.internalSnapshotGeneratorFactory = internalSnapshotGeneratorFactory;
		this.internalValueSetExpanderFactory = internalValueSetExpanderFactory;

		if (noDownloadPackages != null)
			this.noDownloadPackages.addAll(noDownloadPackages);

		this.valueSetBindingStrengths = valueSetBindingStrengths;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(validationPackageClient, "validationPackageClient");
		Objects.requireNonNull(valueSetExpansionClient, "valueSetExpansionClient");

		Objects.requireNonNull(mapper, "mapper");
		Objects.requireNonNull(fhirContext, "fhirContext");

		Objects.requireNonNull(internalSnapshotGeneratorFactory, "internalSnapshotGeneratorFactory");
		Objects.requireNonNull(internalValueSetExpanderFactory, "internalValueSetExpanderFactory");
	}

	@Override
	public List<ValidationPackageWithDepedencies> downloadPackagesWithDependencies(
			Collection<? extends ValidationPackageIdentifier> identifiers)
	{
		Map<ValidationPackageIdentifier, ValidationPackage> allPackagesByNameAndVersion = new HashMap<>();

		List<ValidationPackageWithDepedencies> packages = new ArrayList<>();
		for (ValidationPackageIdentifier identifier : identifiers)
		{
			Map<ValidationPackageIdentifier, ValidationPackage> packagesByNameAndVersion = new HashMap<>();
			downloadPackageWithDependencies(identifier, packagesByNameAndVersion, allPackagesByNameAndVersion);
			packages.add(ValidationPackageWithDepedencies.from(packagesByNameAndVersion, identifier));
		}

		return packages;
	}

	@Override
	public ValidationPackageWithDepedencies downloadPackageWithDependencies(ValidationPackageIdentifier identifier)
	{
		Objects.requireNonNull(identifier, "identifier");

		Map<ValidationPackageIdentifier, ValidationPackage> packagesByNameAndVersion = new HashMap<>();
		downloadPackageWithDependencies(identifier, packagesByNameAndVersion, new HashMap<>());

		return ValidationPackageWithDepedencies.from(packagesByNameAndVersion, identifier);
	}

	@Override
	public IValidationSupport expandValueSetsAndGenerateStructureDefinitionSnapshots(
			Collection<? extends ValidationPackageWithDepedencies> packagesWithDependencies)
	{
		List<ValueSet> expandedValueSets = new ArrayList<>();
		for (ValidationPackageWithDepedencies packageWithDependencies : packagesWithDependencies)
		{
			packageWithDependencies.parseResources(fhirContext);
			expandedValueSets.addAll(withExpandedValueSets(packageWithDependencies));
		}

		return withSnapshots(expandedValueSets, packagesWithDependencies);
	}

	@Override
	public BundleValidator createBundleValidator(IValidationSupport validationSupport,
			ValidationPackageWithDepedencies packageWithDependencies)
	{
		Objects.requireNonNull(validationSupport, "validationSupport");
		Objects.requireNonNull(packageWithDependencies, "packageWithDependencies");

		BundleValidatorImpl validator = new BundleValidatorImpl(
				new ResourceValidatorImpl(fhirContext, validationSupport), fhirContext,
				Collections.singletonList(packageWithDependencies));

		return validator;
	}

	@Override
	public BundleValidator createBundleValidator(IValidationSupport validationSupport,
			Collection<? extends ValidationPackageWithDepedencies> packagesWithDependencies)
	{
		Objects.requireNonNull(validationSupport, "validationSupport");

		BundleValidatorImpl validator = new BundleValidatorImpl(
				new ResourceValidatorImpl(fhirContext, validationSupport), fhirContext, packagesWithDependencies);

		return validator;
	}

	@Override
	public BundleValidator createBundleValidator(List<ValidationPackageIdentifier> identifiers)
	{
		Objects.requireNonNull(identifiers, "identifiers");

		List<ValidationPackageWithDepedencies> packageWithDependencies = downloadPackagesWithDependencies(identifiers);
		IValidationSupport validationSupport = expandValueSetsAndGenerateStructureDefinitionSnapshots(
				packageWithDependencies);
		return createBundleValidator(validationSupport, packageWithDependencies);
	}

	@Override
	public BundleValidator createBundleValidator(ValidationPackageIdentifier identifier)
	{
		Objects.requireNonNull(identifier, "identifier");

		ValidationPackageWithDepedencies packageWithDependencies = downloadPackageWithDependencies(identifier);
		IValidationSupport validationSupport = expandValueSetsAndGenerateStructureDefinitionSnapshots(
				packageWithDependencies);
		return createBundleValidator(validationSupport, packageWithDependencies);
	}

	private void downloadPackageWithDependencies(ValidationPackageIdentifier identifier,
			Map<ValidationPackageIdentifier, ValidationPackage> packagesByNameAndVersion,
			Map<ValidationPackageIdentifier, ValidationPackage> allPackagesByNameAndVersion)
	{
		if (allPackagesByNameAndVersion.containsKey(identifier))
		{
			// already downloaded
			return;
		}
		else if (noDownloadPackages.contains(identifier))
		{
			logger.debug("Not downloading package {}", identifier.toString());
			return;
		}

		ValidationPackage vPackage = downloadAndHandleException(identifier);
		packagesByNameAndVersion.put(identifier, vPackage);
		allPackagesByNameAndVersion.put(identifier, vPackage);

		ValidationPackageDescriptor descriptor = getDescriptorAndHandleException(vPackage);
		descriptor.getDependencyIdentifiers().forEach(
				i -> downloadPackageWithDependencies(i, packagesByNameAndVersion, allPackagesByNameAndVersion));
	}

	private ValidationPackage downloadAndHandleException(ValidationPackageIdentifier identifier)
	{
		try
		{
			logger.debug("Downloading validation package {}", identifier);
			return validationPackageClient.download(identifier);
		}
		catch (WebApplicationException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private ValidationPackageDescriptor getDescriptorAndHandleException(ValidationPackage vPackage)
	{
		try
		{
			return vPackage.getDescriptor(mapper);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private List<ValueSet> withExpandedValueSets(ValidationPackageWithDepedencies packageWithDependencies)
	{
		List<ValueSet> expandedValueSets = new ArrayList<>();
		ValueSetExpander expander = internalValueSetExpanderFactory.apply(fhirContext,
				createSupportChain(fhirContext, new ValidationSupportWithCustomResources(fhirContext, null, null, null),
						Collections.singletonList(packageWithDependencies)));

		packageWithDependencies.getValueSetsIncludingDependencies(valueSetBindingStrengths, fhirContext).forEach(v ->
		{
			logger.debug("Expanding ValueSet {}|{}", v.getUrl(), v.getVersion());

			// ValueSet uses filter in compose
			if (v.hasCompose() && (v.getCompose().hasInclude() || v.getCompose().hasExclude())
					&& (v.getCompose().getInclude().stream().anyMatch(ConceptSetComponent::hasFilter)
							|| v.getCompose().getExclude().stream().anyMatch(ConceptSetComponent::hasFilter)))
			{
				expandExternal(expandedValueSets, v);
			}
			else
			{
				// will try external expansion if internal not successful
				expandInternal(expandedValueSets, expander, v);
			}
		});

		return expandedValueSets;
	}

	private void expandExternal(List<ValueSet> expandedValueSets, ValueSet v)
	{
		try
		{
			ValueSet expansion = valueSetExpansionClient.expand(v);
			expandedValueSets.add(expansion);
		}
		catch (WebApplicationException e)
		{
			logger.warn(
					"Error while expanding ValueSet {}|{} externally, this may result in incomplete validation: {} - {}",
					v.getUrl(), v.getVersion(), e.getClass().getName(), e.getMessage());
			getOutcome(e).ifPresent(m -> logger.debug("Expansion error response: {}", m));
			logger.debug("ValueSet with error while expanding: {}",
					fhirContext.newJsonParser().encodeResourceToString(v));
		}
		catch (Exception e)
		{
			logger.warn(
					"Error while expanding ValueSet {}|{} externally, this may result in incomplete validation: {} - {}",
					v.getUrl(), v.getVersion(), e.getClass().getName(), e.getMessage());
			logger.debug("ValueSet with error while expanding: {}",
					fhirContext.newJsonParser().encodeResourceToString(v));
		}
	}

	private void expandInternal(List<ValueSet> expandedValueSets, ValueSetExpander expander, ValueSet v)
	{
		try
		{
			ValueSetExpansionOutcome expansion = expander.expand(v);

			if (expansion.getError() != null)
				logger.warn("Error while expanding ValueSet {}|{} internally: {}", v.getUrl(), v.getVersion(),
						expansion.getError());
			else
				expandedValueSets.add(expansion.getValueset());
		}
		catch (Exception e)
		{
			logger.info(
					"Error while expanding ValueSet {}|{} internally: {} - {}, trying to expand via external terminology server next",
					v.getUrl(), v.getVersion(), e.getClass().getName(), e.getMessage());

			expandExternal(expandedValueSets, v);
		}
	}

	private Optional<String> getOutcome(WebApplicationException e)
	{
		if (e.getResponse().hasEntity())
		{
			String response = e.getResponse().readEntity(String.class);
			return Optional.of(response);
		}
		else
			return Optional.empty();
	}

	private IValidationSupport withSnapshots(List<ValueSet> expandedValueSets,
			Collection<? extends ValidationPackageWithDepedencies> packagesWithDependencies)
	{
		Map<String, StructureDefinition> snapshots = new HashMap<>();

		ValidationSupportWithCustomResources snapshotsAndExpandedValueSets = new ValidationSupportWithCustomResources(
				fhirContext, null, null, expandedValueSets);
		ValidationSupportChain supportChain = createSupportChain(fhirContext, snapshotsAndExpandedValueSets,
				packagesWithDependencies);

		SnapshotGenerator generator = internalSnapshotGeneratorFactory.apply(fhirContext, supportChain);

		for (ValidationPackageWithDepedencies packageWithDependencies : packagesWithDependencies)
		{
			packageWithDependencies.getValidationSupportResources().getStructureDefinitions().stream()
					.filter(s -> s.hasDifferential() && !s.hasSnapshot())
					.forEach(diff -> createSnapshot(packageWithDependencies, snapshotsAndExpandedValueSets, snapshots,
							generator, diff));
		}

		return supportChain;
	}

	private void createSnapshot(ValidationPackageWithDepedencies packageWithDependencies,
			ValidationSupportWithCustomResources snapshotsAndExpandedValueSets,
			Map<String, StructureDefinition> snapshots, SnapshotGenerator generator, StructureDefinition diff)
	{
		if (snapshots.containsKey(diff.getUrl() + "|" + diff.getVersion()))
			return;

		List<StructureDefinition> definitions = new ArrayList<>();
		definitions.addAll(packageWithDependencies.getStructureDefinitionDependencies(diff));
		definitions.add(diff);

		logger.debug("Generating snapshot for {}|{}, base {}, dependencies {}", diff.getUrl(), diff.getVersion(),
				diff.getBaseDefinition(),
				definitions.stream()
						.filter(sd -> !sd.equals(diff) && !sd.getUrl().equals(diff.getBaseDefinition())
								&& !(sd.getUrl() + "|" + sd.getVersion()).equals(diff.getBaseDefinition()))
						.map(sd -> sd.getUrl() + "|" + sd.getVersion()).sorted()
						.collect(Collectors.joining(", ", "[", "]")));

		definitions.stream().filter(sd -> sd.hasDifferential() && !sd.hasSnapshot()
				&& !snapshots.containsKey(sd.getUrl() + "|" + sd.getVersion())).forEach(sd ->
				{
					try
					{
						logger.debug("Generating snapshot for {}|{}", sd.getUrl(), sd.getVersion());
						SnapshotWithValidationMessages snapshot = generator.generateSnapshot(sd);

						if (snapshot.getSnapshot().hasSnapshot())
						{
							snapshots.put(snapshot.getSnapshot().getUrl() + "|" + snapshot.getSnapshot().getVersion(),
									snapshot.getSnapshot());
							snapshotsAndExpandedValueSets.addOrReplace(snapshot.getSnapshot());
						}
						else
							logger.error(
									"Error while generating snapshot for {}|{}: Not snaphsot returned from generator",
									diff.getUrl(), diff.getVersion());

						snapshot.getMessages().forEach(m ->
						{
							if (EnumSet.of(IssueSeverity.FATAL, IssueSeverity.ERROR, IssueSeverity.WARNING)
									.contains(m.getLevel()))
								logger.warn("{}|{} {}: {}", diff.getUrl(), diff.getVersion(), m.getLevel(),
										m.toString());
							else
								logger.info("{}|{} {}: {}", diff.getUrl(), diff.getVersion(), m.getLevel(),
										m.toString());
						});
					}
					catch (Exception e)
					{
						logger.error("Error while generating snapshot for {}|{}: {} - {}", diff.getUrl(),
								diff.getVersion(), e.getClass().getName(), e.getMessage());
					}
				});

		logger.debug("Generating snapshot for {}|{} [Done]", diff.getUrl(), diff.getVersion());
	}

	private ValidationSupportChain createSupportChain(FhirContext context,
			IValidationSupport snapshotsAndExpandedValueSets,
			Collection<? extends ValidationPackageWithDepedencies> packagesWithDependencies)
	{
		return new ValidationSupportChain(new CodeValidatorForExpandedValueSets(context),
				new InMemoryTerminologyServerValidationSupport(context), snapshotsAndExpandedValueSets,
				new ValidationSupportWithCustomResources(context,
						getAll(ValidationPackageWithDepedencies::getAllStructureDefinitions, packagesWithDependencies),
						getAll(ValidationPackageWithDepedencies::getAllCodeSystems, packagesWithDependencies),
						getAll(ValidationPackageWithDepedencies::getAllValueSets, packagesWithDependencies)),
				new DefaultProfileValidationSupport(context), new QuietCommonCodeSystemsTerminologyService(context),
				// TODO remove NonValidatingValidationSupport
				new NonValidatingValidationSupport(context, "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
						"http://fhir.de/CodeSystem/dimdi/icd-10-gm", "http://fhir.de/CodeSystem/bfarm/ops",
						"http://fhir.de/CodeSystem/dimdi/ops", "http://fhir.de/CodeSystem/ifa/pzn"));
	}

	private <V> List<V> getAll(Function<ValidationPackageWithDepedencies, List<V>> mapper,
			Collection<? extends ValidationPackageWithDepedencies> packagesWithDependencies)
	{
		return packagesWithDependencies.stream().map(mapper).flatMap(List::stream).toList();
	}
}
