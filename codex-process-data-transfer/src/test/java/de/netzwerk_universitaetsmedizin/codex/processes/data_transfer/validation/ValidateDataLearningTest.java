package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bouncycastle.pkcs.PKCSException;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r4.model.Enumerations.BindingStrength;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Objects;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.ValidationResult;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;
import dev.dsf.fhir.validation.ValidationSupportWithCustomResources;
import dev.dsf.fhir.validation.ValueSetExpanderImpl;

public class ValidateDataLearningTest
{
	private static final Logger logger = LoggerFactory.getLogger(ValidateDataLearningTest.class);

	private static final Path cacheFolder = Paths.get("target");
	private static final FhirContext fhirContext = FhirContext.forR4();
	private static final ObjectMapper mapper = JsonMapper.builder().serializationInclusion(Include.NON_NULL)
			.serializationInclusion(Include.NON_EMPTY).disable(MapperFeature.AUTO_DETECT_CREATORS)
			.disable(MapperFeature.AUTO_DETECT_FIELDS).disable(MapperFeature.AUTO_DETECT_SETTERS).build();

	private static final class ResourceAndFilename
	{
		final Resource resource;
		final String filename;

		ResourceAndFilename(Resource resource, String filename)
		{
			this.resource = resource;
			this.filename = filename;
		}
	}

	private ValidationPackageManager createValidationPackageManager()
			throws IOException, CertificateException, PKCSException, KeyStoreException, NoSuchAlgorithmException
	{
		Properties properties = new Properties();
		try (InputStream appProperties = Files.newInputStream(Paths.get("application.properties")))
		{
			properties.load(appProperties);
		}

		X509Certificate certificate = PemIo.readX509CertificateFromPem(Paths.get(properties.getProperty(
				"de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate")));
		char[] keyStorePassword = properties.getProperty(
				"de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate.private.key.password")
				.toCharArray();
		PrivateKey privateKey = PemIo.readPrivateKeyFromPem(Paths.get(properties.getProperty(
				"de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate.private.key")),
				keyStorePassword);
		KeyStore keyStore = CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate },
				UUID.randomUUID().toString(), keyStorePassword);

		ValidationPackageClient validationPackageClient = new ValidationPackageClientJersey(
				"https://packages.simplifier.net");
		ValidationPackageClient validationPackageClientWithCache = new ValidationPackageClientWithFileSystemCache(
				cacheFolder, mapper, validationPackageClient);
		ValueSetExpansionClient valueSetExpansionClient = new ValueSetExpansionClientJersey(
				"https://terminology-highmed.medic.medfak.uni-koeln.de/fhir", null, keyStore, keyStorePassword, null,
				null, null, null, null, 0, 0, false, mapper, fhirContext);
		ValueSetExpansionClient valueSetExpansionClientWithModifiers = new ValueSetExpansionClientWithModifiers(
				valueSetExpansionClient);
		ValueSetExpansionClient valueSetExpansionClientWithCache = new ValueSetExpansionClientWithFileSystemCache(
				cacheFolder, fhirContext, valueSetExpansionClientWithModifiers);
		ValidationPackageManager manager = new ValidationPackageManagerImpl(validationPackageClientWithCache,
				valueSetExpansionClientWithCache, mapper, fhirContext,
				(fc, vs) -> new PluginSnapshotGeneratorWithFileSystemCache(cacheFolder, fc,
						new PluginSnapshotGeneratorWithModifiers(new PluginSnapshotGeneratorImpl(fc, vs))),
				(fc, vs) -> new ValueSetExpanderWithFileSystemCache(cacheFolder, fc, new ValueSetExpanderImpl(fc, vs)));
		return manager;
	}

	@Test
	public void testCheckValueSetForComplexeSnomedCtCodes() throws Exception
	{
		ValidationPackageManager validationPackageManager = createValidationPackageManager();
		ValidationPackageWithDepedencies packageWithDepedencies = validationPackageManager
				.downloadPackageWithDependencies("de.gecco", "1.0.5");

		// packageWithDepedencies.parseResources(fhirContext);
		//
		// List<ValueSet> valueSets = packageWithDepedencies
		// .getValueSetsIncludingDependencies(EnumSet.allOf(BindingStrength.class));
		//
		// valueSets.stream()
		// .filter(v -> v.getCompose().getInclude().stream().flatMap(c -> c.getConcept().stream()).anyMatch(
		// c -> c.getCode().contains("+") || c.getCode().contains(":") || c.getCode().contains("=")))
		// .forEach(v -> System.out.println(v.getUrl() + "|" + v.getVersion()));

		IValidationSupport validationSupport = validationPackageManager
				.expandValueSetsAndGenerateStructureDefinitionSnapshots(packageWithDepedencies);

		List<ValueSet> valueSets = packageWithDepedencies
				.getValueSetsIncludingDependencies(EnumSet.allOf(BindingStrength.class), fhirContext);

		valueSets.forEach(v ->
		{
			System.out.println(v.getUrl() + "|" + v.getVersion() + " " + v.hasCompose() + " " + v.hasExpansion());
			ValueSet expandedV = (ValueSet) validationSupport.fetchValueSet(v.getUrl());

			if (expandedV != null)
				System.out.println(expandedV.getUrl() + "|" + expandedV.getVersion() + " " + expandedV.hasCompose()
						+ " " + expandedV.hasExpansion());
			else
				System.out.println("null");

			if (v.hasCompose() && !v.hasExpansion() && !expandedV.hasCompose() && expandedV.hasExpansion())
				testContainsAllFromCompose(v.getCompose(), expandedV.getExpansion());
		});
	}

	private void testContainsAllFromCompose(ValueSetComposeComponent compose, ValueSetExpansionComponent expansion)
	{
		compose.getInclude().stream().filter(i -> i.hasConcept()).flatMap(c -> c.getConcept().stream())
				.filter(c -> c.hasCode()).map(c -> c.getCode()).forEach(code ->
				{
					if (!expansion.getContains().stream().anyMatch(c -> Objects.equal(c.getCode(), code)))
						System.out.println("Missing code in expansion: " + code);
				});
	}

	@Test
	public void testDownloadTarGzAndParseDescriptor() throws Exception
	{
		ValidationPackageClient client = new ValidationPackageClientJersey("https://packages.simplifier.net");

		ValidationPackage validationPackage = client.download("de.gecco", "1.0.5");

		validationPackage.getEntries().forEach(e ->
		{
			if ("package/package.json".equals(e.getFileName()))
				logger.debug(new String(e.getContent(), StandardCharsets.UTF_8));
		});

		ValidationPackageDescriptor descriptor = validationPackage.getDescriptor(mapper);
		logger.debug("{}/{}:", descriptor.getName(), descriptor.getVersion());
		descriptor.getDependencies().forEach((k, v) -> logger.debug("\t{}/{}", k, v));
	}

	@Test
	public void testDownloadTarGzAndListExampleResources() throws Exception
	{
		ValidationPackageClient client = new ValidationPackageClientJersey("https://packages.simplifier.net");

		ValidationPackage validationPackage = client.download("de.gecco", "1.0.5");

		validationPackage.getEntries().forEach(e ->
		{
			if (e.getFileName() != null && e.getFileName().startsWith("package/examples/"))
				logger.debug(e.getFileName());
		});
	}

	@Test
	public void testDownloadTarGzAndValidateExampleResources() throws Exception
	{
		ValidationPackageClient client = new ValidationPackageClientJersey("https://packages.simplifier.net");
		ValidationPackage validationPackage = client.download("de.gecco", "1.0.5");

		List<ResourceAndFilename> examples = validationPackage.getEntries().stream().map(e ->
		{
			if (e.getFileName() != null && e.getFileName().startsWith("package/examples/"))
			{
				logger.debug("Reading {}", e.getFileName());
				try
				{
					return new ResourceAndFilename((Resource) fhirContext.newJsonParser()
							.parseResource(new ByteArrayInputStream(e.getContent())), e.getFileName());
				}
				catch (Exception ex)
				{
					logger.error("Error while reading {}: {}", e.getFileName(), ex.getMessage());
					return null;
				}
			}
			else
				return null;
		}).filter(e -> e != null).collect(Collectors.toList());

		ValidationPackageManager manager = createValidationPackageManager();

		BundleValidator validator = manager.createBundleValidator("de.gecco", "1.0.5");

		examples.forEach(r ->
		{
			ValidationResult result;
			try
			{
				logger.debug("Validating resource of type {} from {}", r.resource.getResourceType().name(), r.filename);
				result = validator.validate(r.resource);
			}
			catch (Exception ex)
			{
				logger.error("Unable to validate resource of type {} from {}: {}", r.resource.getResourceType().name(),
						r.filename, ex.getMessage());
				return;
			}

			OperationOutcome outcome = (OperationOutcome) result.toOperationOutcome();

			outcome.getIssue().forEach(issue ->
			{
				if (OperationOutcome.IssueSeverity.FATAL.equals(issue.getSeverity()))
					logger.error("Bundle fatal validation error ({}): {}",
							issue.getLocation().stream().map(StringType::getValue).collect(Collectors.joining(", ")),
							issue.getDiagnostics());
				else if (OperationOutcome.IssueSeverity.ERROR.equals(issue.getSeverity()))
					logger.error("Bundle validation error ({}): {}",
							issue.getLocation().stream().map(StringType::getValue).collect(Collectors.joining(", ")),
							issue.getDiagnostics());
				else if (OperationOutcome.IssueSeverity.WARNING.equals(issue.getSeverity()))
					logger.warn("Bundle validation warning ({}): {}",
							issue.getLocation().stream().map(StringType::getValue).collect(Collectors.joining(", ")),
							issue.getDiagnostics());
				else if (issue.hasLocation())
					logger.info("Bundle validation info ({}): {}",
							issue.getLocation().stream().map(StringType::getValue).collect(Collectors.joining(", ")),
							issue.getDiagnostics());
				else
					logger.info("Bundle validation info: {}", issue.getDiagnostics());
			});
		});
	}

	@Test
	public void testDownloadWithDependencies() throws Exception
	{
		ValidationPackageManager manager = createValidationPackageManager();
		ValidationPackageWithDepedencies packageWithDependencies = manager.downloadPackageWithDependencies("de.gecco",
				"1.0.5");
		packageWithDependencies.parseResources(fhirContext);
	}

	@Test
	public void testValidate() throws Exception
	{
		ValidationPackageManager manager = createValidationPackageManager();
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
		ValidationPackageManager manager = createValidationPackageManager();

		ValidationPackageWithDepedencies packageWithDependencies = manager.downloadPackageWithDependencies("de.gecco",
				"1.0.5");
		packageWithDependencies.parseResources(fhirContext);

		packageWithDependencies.getAllStructureDefinitions().stream()
				.sorted(Comparator.comparing(StructureDefinition::getUrl)
						.thenComparing(Comparator.comparing(StructureDefinition::getVersion)))
				.forEach(s -> logger.debug("{} {}", s.getUrl(), s.getVersion()));

		StructureDefinition miiRef = packageWithDependencies.getAllStructureDefinitions().stream()
				.filter(s -> "https://www.medizininformatik-initiative.de/fhir/core/StructureDefinition/MII-Reference"
						.equals(s.getUrl()))
				.findFirst().get();

		SnapshotGenerator sGen = new PluginSnapshotGeneratorImpl(fhirContext,
				new ValidationSupportChain(new InMemoryTerminologyServerValidationSupport(fhirContext),
						new ValidationSupportWithCustomResources(fhirContext,
								packageWithDependencies.getAllStructureDefinitions(),
								packageWithDependencies.getAllCodeSystems(), packageWithDependencies.getAllValueSets()),
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

		Map<String, StructureDefinition> sDefByUrl = packageWithDependencies.getAllStructureDefinitions().stream()
				.collect(Collectors.toMap(StructureDefinition::getUrl, Function.identity()));

		packageWithDependencies.getAllStructureDefinitions().forEach(s ->
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
			profileDependencies.stream().sorted().forEach(url -> logger.debug("    {}", url));
		}
		if (!targetProfileDependencies.isEmpty())
		{
			logger.debug("");
			logger.debug("  TargetProfile-Dependencies:");
			targetProfileDependencies.stream().sorted().forEach(url -> logger.debug("    {}", url));
		}
	}

	private void printTree(String k, StructureDefinition def,
			Map<String, StructureDefinition> structureDefinitionsByUrl, String indentation,
			Set<String> profileDependencies, Set<String> targetProfileDependencies)
	{
		logger.debug("{}Profile: {}", indentation, k);
		for (ElementDefinition element : def.getDifferential().getElement())
		{
			if (element.getType().stream().filter(t -> !t.getProfile().isEmpty() || !t.getTargetProfile().isEmpty())
					.findAny().isPresent())
			{
				logger.debug("{}  Element: {} (Path: {})", indentation, element.getId(), element.getPath());
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
								logger.debug("{}    Profile: {} ?", indentation, profile.getValue());
						}
					}
					if (!type.getTargetProfile().isEmpty())
					{
						for (CanonicalType targetProfile : type.getTargetProfile())
						{
							targetProfileDependencies.add(targetProfile.getValue());
							logger.debug("{}    TargetProfile: {}", indentation, targetProfile.getValue());
						}
					}
				}
			}
		}
	}
}
