package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.highmed.dsf.fhir.json.ObjectMapperFactory;
import org.highmed.dsf.fhir.validation.SnapshotGenerator;
import org.highmed.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;
import org.highmed.dsf.fhir.validation.ValidationSupportWithCustomResources;
import org.highmed.dsf.fhir.validation.ValueSetExpanderImpl;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;

public class ValidateDataLearningTest
{
	private static final Logger logger = LoggerFactory.getLogger(ValidateDataLearningTest.class);

	private static final Path cacheFolder = Paths.get("target");
	private static final FhirContext fhirContext = FhirContext.forR4();
	private static final ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(fhirContext);

	@Test
	public void testDownloadTagGz() throws Exception
	{
		ValidationPackageClient client = new ValidationPackageClientJersey("https://packages.simplifier.net");

		ValidationPackage validationPackage = client.download("de.gecco", "1.0.5");

		validationPackage.getEntries().forEach(e ->
		{
			if ("package/package.json".equals(e.getFileName()))
				logger.debug(new String(e.getContent(), StandardCharsets.UTF_8));
		});

		ValidationPackageDescriptor descriptor = validationPackage.getDescriptor(mapper);
		logger.debug(descriptor.getName() + "/" + descriptor.getVersion() + ":");
		descriptor.getDependencies().forEach((k, v) -> logger.debug("\t" + k + "/" + v));
	}

	@Test
	public void testDownloadWithDependencies() throws Exception
	{
		ValidationPackageClient validationPackageClient = new ValidationPackageClientJersey(
				"https://packages.simplifier.net");
		ValidationPackageClient validationPackageClientWithCache = new ValidationPackageClientWithFileSystemCache(
				cacheFolder, mapper, validationPackageClient);

		ValueSetExpansionClient valueSetExpansionClient = new ValueSetExpansionClientJersey(
				"https://r4.ontoserver.csiro.au/fhir", mapper, fhirContext);
		ValueSetExpansionClient valueSetExpansionClientWithCache = new ValueSetExpansionClientWithFileSystemCache(
				cacheFolder, fhirContext, valueSetExpansionClient);

		ValidationPackageManager manager = new ValidationPackageManagerImpl(validationPackageClientWithCache,
				valueSetExpansionClientWithCache, mapper, fhirContext, PluginSnapshotGeneratorImpl::new,
				ValueSetExpanderImpl::new);

		List<ValidationPackage> validationPackages = manager.downloadPackageWithDependencies("de.gecco", "1.0.5");
		validationPackages.forEach(p ->
		{
			logger.debug(p.getName() + "/" + p.getVersion());
			p.parseResources(fhirContext);
		});
	}

	@Test
	public void testValidate() throws Exception
	{
		ValidationPackageClient validationPackageClient = new ValidationPackageClientJersey(
				"https://packages.simplifier.net");
		ValidationPackageClient validationPackageClientWithCache = new ValidationPackageClientWithFileSystemCache(
				cacheFolder, mapper, validationPackageClient);
		ValueSetExpansionClient valueSetExpansionClient = new ValueSetExpansionClientJersey(
				"https://r4.ontoserver.csiro.au/fhir", mapper, fhirContext);
		ValueSetExpansionClient valueSetExpansionClientWithCache = new ValueSetExpansionClientWithFileSystemCache(
				cacheFolder, fhirContext, valueSetExpansionClient);
		ValidationPackageManager manager = new ValidationPackageManagerImpl(validationPackageClientWithCache,
				valueSetExpansionClientWithCache, mapper, fhirContext,
				(fc, vs) -> new PluginSnapshotGeneratorWithFileSystemCache(cacheFolder, fc,
						new PluginSnapshotGeneratorImpl(fc, vs)),
				(fc, vs) -> new ValueSetExpanderWithFileSystemCache(cacheFolder, fc, new ValueSetExpanderImpl(fc, vs)));

		BundleValidator validator = manager.createBundleValidator("de.gecco", "1.0.5");

		logger.debug("---------- executing validation tests ----------");

		Path bundleFolder = Paths.get("src/test/resources/fhir/Bundle");
		String[] bundles = { "dic_fhir_store_demo_bf_large.json", "dic_fhir_store_demo_bf.json",
				"dic_fhir_store_demo_psn_large.json", "dic_fhir_store_demo_psn.json" };
		Arrays.stream(bundles).map(bundleFolder::resolve).forEach(validateWith(validator));
	}

	private Consumer<Path> validateWith(BundleValidator validator)
	{
		return file ->
		{
			logger.debug("----- {} -----", file.toString());

			Bundle bundle;
			try
			{
				byte[] bundleData = Files.readAllBytes(file);
				bundle = fhirContext.newJsonParser().parseResource(Bundle.class,
						new String(bundleData, StandardCharsets.UTF_8));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}

			Bundle validationResult = validator.validate(bundle);

			logger.info("Validation result bundle: {}",
					fhirContext.newJsonParser().encodeResourceToString(validationResult));

			for (int i = 0; i < validationResult.getEntry().size(); i++)
			{
				BundleEntryComponent entry = validationResult.getEntry().get(i);
				OperationOutcome outcome = (OperationOutcome) entry.getResponse().getOutcome();

				final int index = i;
				outcome.getIssue().forEach(issue ->
				{
					if (OperationOutcome.IssueSeverity.FATAL.equals(issue.getSeverity()))
						logger.error(
								"Bundle index {} fatal validation error ({}): {}", index, issue.getLocation().stream()
										.map(StringType::getValue).collect(Collectors.joining(", ")),
								issue.getDiagnostics());
					else if (OperationOutcome.IssueSeverity.ERROR.equals(issue.getSeverity()))
						logger.error(
								"Bundle index {} validation error ({}): {}", index, issue.getLocation().stream()
										.map(StringType::getValue).collect(Collectors.joining(", ")),
								issue.getDiagnostics());
					else if (OperationOutcome.IssueSeverity.WARNING.equals(issue.getSeverity()))
						logger.warn(
								"Bundle index {} validation warning ({}): {}", index, issue.getLocation().stream()
										.map(StringType::getValue).collect(Collectors.joining(", ")),
								issue.getDiagnostics());
					else if (issue.hasLocation())
						logger.info(
								"Bundle index {} validation info ({}): {}", index, issue.getLocation().stream()
										.map(StringType::getValue).collect(Collectors.joining(", ")),
								issue.getDiagnostics());
					else
						logger.info("Bundle index {} validation info: {}", index, issue.getDiagnostics());
				});
			}
		};
	}

	@Test
	public void testGenerateSnapshots() throws Exception
	{
		ValidationPackageClient validationPackageClient = new ValidationPackageClientJersey(
				"https://packages.simplifier.net");
		ValidationPackageClient validationPackageClientWithCache = new ValidationPackageClientWithFileSystemCache(
				cacheFolder, mapper, validationPackageClient);
		ValueSetExpansionClient valueSetExpansionClient = new ValueSetExpansionClientJersey(
				"https://r4.ontoserver.csiro.au/fhir", mapper, fhirContext);
		ValueSetExpansionClient valueSetExpansionClientWithCache = new ValueSetExpansionClientWithFileSystemCache(
				cacheFolder, fhirContext, valueSetExpansionClient);
		ValidationPackageManager manager = new ValidationPackageManagerImpl(validationPackageClientWithCache,
				valueSetExpansionClientWithCache, mapper, fhirContext, PluginSnapshotGeneratorImpl::new,
				ValueSetExpanderImpl::new);

		List<ValidationPackage> validationPackages = manager.downloadPackageWithDependencies("de.gecco", "1.0.5");
		validationPackages.forEach(p ->
		{
			logger.debug(p.getName() + "/" + p.getVersion());
			p.parseResources(fhirContext);
		});

		validationPackages.stream().flatMap(p -> p.getValidationSupportResources().getStructureDefinitions().stream())
				.sorted(Comparator.comparing(StructureDefinition::getUrl)
						.thenComparing(Comparator.comparing(StructureDefinition::getVersion)))
				.forEach(s -> logger.debug(s.getUrl() + " " + s.getVersion()));

		StructureDefinition miiRef = validationPackages.stream()
				.flatMap(p -> p.getValidationSupportResources().getStructureDefinitions().stream())
				.filter(s -> "https://www.medizininformatik-initiative.de/fhir/core/StructureDefinition/MII-Reference"
						.equals(s.getUrl()))
				.findFirst().get();

		SnapshotGenerator sGen = new PluginSnapshotGeneratorImpl(fhirContext, new ValidationSupportChain(
				new InMemoryTerminologyServerValidationSupport(fhirContext),
				new ValidationSupportChain(validationPackages.stream()
						.map(ValidationPackage::getValidationSupportResources)
						.map(r -> new ValidationSupportWithCustomResources(fhirContext, r.getStructureDefinitions(),
								r.getCodeSystems(), r.getValueSets()))
						.toArray(IValidationSupport[]::new)),
				new DefaultProfileValidationSupport(fhirContext),
				new CommonCodeSystemsTerminologyService(fhirContext)));

		sGen = new PluginSnapshotGeneratorWithModifiers(sGen);

		SnapshotWithValidationMessages result = sGen.generateSnapshot(miiRef);

		result.getMessages().forEach(m ->
		{
			if (IssueSeverity.ERROR.equals(m.getLevel()) || IssueSeverity.FATAL.equals(m.getLevel()))
				logger.error("Error while generating snapshot for {}|{}: {}", result.getSnapshot().getUrl(),
						result.getSnapshot().getVersion(), m.toString());
			else if (IssueSeverity.WARNING.equals(m.getLevel()))
				logger.warn("Warning while generating snapshot for {}|{}: {}", result.getSnapshot().getUrl(),
						result.getSnapshot().getVersion(), m.toString());
			else
				logger.info("Info while generating snapshot for {}|{}: {}", result.getSnapshot().getUrl(),
						result.getSnapshot().getVersion(), m.toString());
		});

		Map<String, StructureDefinition> sDefByUrl = validationPackages.stream()
				.flatMap(p -> p.getValidationSupportResources().getStructureDefinitions().stream())
				.collect(Collectors.toMap(StructureDefinition::getUrl, Function.identity()));

		validationPackages.stream().flatMap(p -> p.getValidationSupportResources().getStructureDefinitions().stream())
				.forEach(s ->
				{
					logger.info("StructureDefinition {}|{}:", s.getUrl(), s.getVersion());
					printTree(s, sDefByUrl);
					logger.debug("");
				});
	}

	private void printTree(StructureDefinition def, Map<String, StructureDefinition> structureDefinitionsByUrl)
	{
		logger.debug("");

		Set<String> profileDependencies = new HashSet<>();
		Set<String> targetProfileDependencies = new HashSet<>();
		printTree(def.getUrl(), def, structureDefinitionsByUrl, "", profileDependencies, targetProfileDependencies);

		if (!profileDependencies.isEmpty())
		{
			logger.debug("");
			logger.debug("  Profile-Dependencies:");
			profileDependencies.stream().sorted().forEach(url -> logger.debug("    " + url));
		}
		if (!targetProfileDependencies.isEmpty())
		{
			logger.debug("");
			logger.debug("  TargetProfile-Dependencies:");
			targetProfileDependencies.stream().sorted().forEach(url -> logger.debug("    " + url));
		}
	}

	private void printTree(String k, StructureDefinition def,
			Map<String, StructureDefinition> structureDefinitionsByUrl, String indentation,
			Set<String> profileDependencies, Set<String> targetProfileDependencies)
	{
		logger.debug(indentation + "Profile: " + k);
		for (ElementDefinition element : def.getDifferential().getElement())
		{
			if (element.getType().stream().filter(t -> !t.getProfile().isEmpty() || !t.getTargetProfile().isEmpty())
					.findAny().isPresent())
			{
				logger.debug(indentation + "  Element: " + element.getId() + " (Path: " + element.getPath() + ")");
				for (TypeRefComponent type : element.getType())
				{
					if (!type.getProfile().isEmpty())
					{
						for (CanonicalType profile : type.getProfile())
						{
							profileDependencies.add(profile.getValue());

							if (structureDefinitionsByUrl.containsKey(profile.getValue()))
								printTree(profile.getValue(), structureDefinitionsByUrl.get(profile.getValue()),
										structureDefinitionsByUrl, indentation + "    ", profileDependencies,
										targetProfileDependencies);
							else
								logger.debug(indentation + "    Profile: " + profile.getValue() + " ?");
						}
					}
					if (!type.getTargetProfile().isEmpty())
					{
						for (CanonicalType targetProfile : type.getTargetProfile())
						{
							targetProfileDependencies.add(targetProfile.getValue());
							logger.debug(indentation + "    TargetProfile: " + targetProfile.getValue());
						}
					}
				}
			}
		}
	}
}
