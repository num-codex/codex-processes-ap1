package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.highmed.dsf.fhir.validation.ResourceValidatorImpl;
import org.highmed.dsf.fhir.validation.SnapshotGenerator;
import org.highmed.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;
import org.highmed.dsf.fhir.validation.ValidationSupportWithCustomResources;
import org.highmed.dsf.fhir.validation.ValueSetExpander;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.TypeRefComponent;
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

public class ValidationPackageManagerImpl implements InitializingBean, ValidationPackageManager
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationPackageManagerImpl.class);

	public static final List<ValidationPackageIdentifier> PACKAGE_IGNORE = List
			.of(new ValidationPackageIdentifier("hl7.fhir.r4.core", "4.0.1"));

	private final ValidationPackageClient validationPackageClient;
	private final ValueSetExpansionClient valueSetExpansionClient;

	private final ObjectMapper mapper;
	private final FhirContext fhirContext;

	private final BiFunction<FhirContext, IValidationSupport, SnapshotGenerator> internalSnapshotGeneratorFactory;
	private final BiFunction<FhirContext, IValidationSupport, ValueSetExpander> internalValueSetExpanderFactory;

	private final List<ValidationPackageIdentifier> noDownloadPackages = new ArrayList<>();

	public ValidationPackageManagerImpl(ValidationPackageClient validationPackageClient,
			ValueSetExpansionClient valueSetExpansionClient, ObjectMapper mapper, FhirContext fhirContext,
			BiFunction<FhirContext, IValidationSupport, SnapshotGenerator> internalSnapshotGeneratorFactory,
			BiFunction<FhirContext, IValidationSupport, ValueSetExpander> internalValueSetExpanderFactory)
	{
		this(validationPackageClient, valueSetExpansionClient, mapper, fhirContext, internalSnapshotGeneratorFactory,
				internalValueSetExpanderFactory, PACKAGE_IGNORE);
	}

	public ValidationPackageManagerImpl(ValidationPackageClient validationPackageClient,
			ValueSetExpansionClient valueSetExpansionClient, ObjectMapper mapper, FhirContext fhirContext,
			BiFunction<FhirContext, IValidationSupport, SnapshotGenerator> internalSnapshotGeneratorFactory,
			BiFunction<FhirContext, IValidationSupport, ValueSetExpander> internalValueSetExpanderFactory,
			Collection<ValidationPackageIdentifier> noDownloadPackages)
	{
		this.validationPackageClient = validationPackageClient;
		this.valueSetExpansionClient = valueSetExpansionClient;
		this.mapper = mapper;
		this.fhirContext = fhirContext;
		this.internalSnapshotGeneratorFactory = internalSnapshotGeneratorFactory;
		this.internalValueSetExpanderFactory = internalValueSetExpanderFactory;

		if (noDownloadPackages != null)
			this.noDownloadPackages.addAll(noDownloadPackages);
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
	public List<ValidationPackage> downloadPackageWithDependencies(ValidationPackageIdentifier identifier)
	{
		Objects.requireNonNull(identifier, "identifier");

		Map<ValidationPackageIdentifier, ValidationPackage> packagesByNameAndVersion = new HashMap<>();
		downloadPackageWithDependencies(identifier, packagesByNameAndVersion);

		return Collections.unmodifiableList(new ArrayList<>(packagesByNameAndVersion.values()));
	}

	@Override
	public IValidationSupport expandValueSetsAndGenerateStructureDefinitionSnapshots(
			List<ValidationPackage> validationPackages)
	{
		Objects.requireNonNull(validationPackages, "validationPackages");

		validationPackages.forEach(p -> p.parseResources(fhirContext));

		List<ValidationSupportResources> resources = validationPackages.stream()
				.map(ValidationPackage::getValidationSupportResources).collect(Collectors.toList());

		return withSnapshots(resources, withExpandedValueSets(resources));
	}

	@Override
	public BundleValidator createBundleValidator(IValidationSupport validationSupport)
	{
		Objects.requireNonNull(validationSupport, "validationSupport");

		return new BundleValidatorImpl(new ResourceValidatorImpl(fhirContext, validationSupport));
	}

	@Override
	public BundleValidator createBundleValidator(ValidationPackageIdentifier identifier)
	{
		Objects.requireNonNull(identifier, "identifier");

		List<ValidationPackage> vPackages = downloadPackageWithDependencies(identifier);
		IValidationSupport validationSupport = expandValueSetsAndGenerateStructureDefinitionSnapshots(vPackages);
		return createBundleValidator(validationSupport);
	}

	private void downloadPackageWithDependencies(ValidationPackageIdentifier identifier,
			Map<ValidationPackageIdentifier, ValidationPackage> packagesByNameAndVersion)
	{
		if (packagesByNameAndVersion.containsKey(identifier))
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

		ValidationPackageDescriptor descriptor = getDescriptorAndHandleException(vPackage);
		descriptor.getDependencyIdentifiers()
				.forEach(i -> downloadPackageWithDependencies(i, packagesByNameAndVersion));
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

	private List<ValueSet> withExpandedValueSets(List<ValidationSupportResources> resources)
	{
		List<ValueSet> expandedValueSets = new ArrayList<>();
		ValueSetExpander expander = internalValueSetExpanderFactory.apply(fhirContext,
				createSupportChain(fhirContext, resources, Collections.emptyList(), expandedValueSets));

		resources.stream().flatMap(r -> r.getValueSets().stream()).forEach(v ->
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
			logger.error("Error while expanding ValueSet {}|{}: {} - {}", v.getUrl(), v.getVersion(),
					e.getClass().getName(), e.getMessage());
			getOutcome(e).ifPresent(m -> logger.debug("Expansion error response: {}", m));
			logger.debug("ValueSet with error while expanding: {}",
					fhirContext.newJsonParser().encodeResourceToString(v));
		}
		catch (Exception e)
		{
			logger.error("Error while expanding ValueSet {}|{}: {} - {}", v.getUrl(), v.getVersion(),
					e.getClass().getName(), e.getMessage());
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
				logger.warn("Error while expanding ValueSet {}|{}: {}", v.getUrl(), v.getVersion(),
						expansion.getError());
			else
				expandedValueSets.add(expansion.getValueset());
		}
		catch (Exception e)
		{
			logger.warn(
					"Error while expanding ValueSet {}|{}: {} - {}, trying to expand via external terminology server next",
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

	private IValidationSupport withSnapshots(List<ValidationSupportResources> resources,
			List<ValueSet> expandedValueSets)
	{
		Map<String, StructureDefinition> structureDefinitionsByUrl = resources.stream()
				.flatMap(r -> r.getStructureDefinitions().stream())
				.collect(Collectors.toMap(StructureDefinition::getUrl, Function.identity()));

		Map<String, StructureDefinition> snapshots = new HashMap<>();
		ValidationSupportChain supportChain = createSupportChain(fhirContext, resources, snapshots.values(),
				expandedValueSets);

		SnapshotGenerator generator = internalSnapshotGeneratorFactory.apply(fhirContext, supportChain);

		resources.stream().flatMap(r -> r.getStructureDefinitions().stream())
				.filter(s -> s.hasDifferential() && !s.hasSnapshot())
				.forEach(diff -> createSnapshot(structureDefinitionsByUrl, snapshots, generator, diff));

		return supportChain;
	}

	private ValidationSupportChain createSupportChain(FhirContext context, List<ValidationSupportResources> resources,
			Collection<? extends StructureDefinition> snapshots, Collection<? extends ValueSet> expandedValueSets)
	{
		return new ValidationSupportChain(new CodeValidatorForExpandedValueSets(context),
				new InMemoryTerminologyServerValidationSupport(context),
				new ValidationSupportWithCustomResources(context, snapshots, null, expandedValueSets),
				new ValidationSupportChain(resources.stream()
						.map(r -> new ValidationSupportWithCustomResources(context, r.getStructureDefinitions(),
								r.getCodeSystems(), r.getValueSets()))
						.toArray(IValidationSupport[]::new)),
				new DefaultProfileValidationSupport(context), new CommonCodeSystemsTerminologyService(context));
	}

	private void createSnapshot(Map<String, StructureDefinition> structureDefinitionsByUrl,
			Map<String, StructureDefinition> snapshots, SnapshotGenerator generator, StructureDefinition diff)
	{
		if (snapshots.containsKey(diff.getUrl() + "|" + diff.getVersion()))
			return;

		Set<String> dependencies = new HashSet<>();
		Set<String> targetDependencies = new HashSet<>();

		calculateDependencies(diff, structureDefinitionsByUrl, dependencies, targetDependencies);

		logger.debug("Generating snapshot for {}|{}, base {}, dependencies {}, target-dependencies {}", diff.getUrl(),
				diff.getVersion(), diff.getBaseDefinition(),
				dependencies.stream().sorted().collect(Collectors.joining(", ", "[", "]")),
				targetDependencies.stream().sorted().collect(Collectors.joining(", ", "[", "]")));

		if (structureDefinitionsByUrl.containsKey(diff.getBaseDefinition()))
			createSnapshot(structureDefinitionsByUrl, snapshots, generator,
					structureDefinitionsByUrl.get(diff.getBaseDefinition()));

		dependencies.stream().filter(structureDefinitionsByUrl::containsKey).map(structureDefinitionsByUrl::get)
				.forEach(s -> createSnapshot(structureDefinitionsByUrl, snapshots, generator, s));

		targetDependencies.stream().filter(structureDefinitionsByUrl::containsKey).map(structureDefinitionsByUrl::get)
				.forEach(s -> createSnapshot(structureDefinitionsByUrl, snapshots, generator, s));

		try
		{
			SnapshotWithValidationMessages snapshot = generator.generateSnapshot(diff);

			if (snapshot.getMessages().isEmpty())
				snapshots.put(snapshot.getSnapshot().getUrl() + "|" + snapshot.getSnapshot().getVersion(),
						snapshot.getSnapshot());
			else
			{
				snapshot.getMessages().forEach(m ->
				{
					if (EnumSet.of(IssueSeverity.FATAL, IssueSeverity.ERROR, IssueSeverity.WARNING)
							.contains(m.getLevel()))
						logger.warn("{}|{} {}: {}", diff.getUrl(), diff.getVersion(), m.getLevel(), m.toString());
					else
						logger.info("{}|{} {}: {}", diff.getUrl(), diff.getVersion(), m.getLevel(), m.toString());
				});
			}
		}
		catch (Exception e)
		{
			logger.error("Error while generating snapshot for {}|{}: {} - {}", diff.getUrl(), diff.getVersion(),
					e.getClass().getName(), e.getMessage());
		}
	}

	private void calculateDependencies(StructureDefinition structureDefinition,
			Map<String, StructureDefinition> structureDefinitionsByUrl, Set<String> dependencies,
			Set<String> targetDependencies)
	{
		for (ElementDefinition element : structureDefinition.getDifferential().getElement())
		{
			if (element.getType().stream().filter(t -> !t.getProfile().isEmpty() || !t.getTargetProfile().isEmpty())
					.findAny().isPresent())
			{
				for (TypeRefComponent type : element.getType())
				{
					if (!type.getProfile().isEmpty())
					{
						for (CanonicalType profile : type.getProfile())
						{
							dependencies.add(profile.getValue());

							if (structureDefinitionsByUrl.containsKey(profile.getValue()))
								calculateDependencies(structureDefinitionsByUrl.get(profile.getValue()),
										structureDefinitionsByUrl, dependencies, targetDependencies);
						}
					}

					if (!type.getTargetProfile().isEmpty())
						for (CanonicalType targetProfile : type.getTargetProfile())
							targetDependencies.add(targetProfile.getValue());
				}
			}
		}
	}
}
