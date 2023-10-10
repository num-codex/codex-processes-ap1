package de.netzwerk_universitaetsmedizin.codex.processes;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bouncycastle.pkcs.PKCSException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.DiagnosisComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorWithModifiers;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageClientJersey;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageClientWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageIdentifier;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageManager;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageManagerImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageWithDepedencies;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpanderWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClientJersey;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClientWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClientWithModifiers;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.fhir.validation.ValueSetExpanderImpl;

public class PolarDataTest
{
	private static final Logger logger = LoggerFactory.getLogger(PolarDataTest.class);

	private static final String serverBase = "http://localhost:8080/fhir";

	private static final Path cacheFolder = Paths.get("target");
	private static final FhirContext fhirContext = FhirContext.forR4();
	private static final ObjectMapper mapper = JsonMapper.builder().serializationInclusion(Include.NON_NULL)
			.serializationInclusion(Include.NON_EMPTY).disable(MapperFeature.AUTO_DETECT_CREATORS)
			.disable(MapperFeature.AUTO_DETECT_FIELDS).disable(MapperFeature.AUTO_DETECT_SETTERS).build();

	private static final Random random = new Random();

	private static final record BundleWithTrace(Bundle bundle, String trace, String file)
	{
	}

	@Test
	public void readPolar() throws Exception
	{
		List<BundleWithTrace> bundles = readBundles();

		for (BundleWithTrace b : bundles)
		{
			Map<String, Integer> resourcesCount = new HashMap<>();

			b.bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
					.map(BundleEntryComponent::getResource).forEach(r ->
					{
						String resourceType = r.getResourceType().name();
						String profiles = r.getMeta().getProfile().stream().map(CanonicalType::getValue)
								.collect(Collectors.joining(", ", "[", "]"));

						resourcesCount.merge(resourceType + profiles, 1, (i, j) -> i + j);
					});

			List<String> results = resourcesCount.entrySet().stream().sorted(Comparator.comparing(Entry::getKey))
					.map(e -> String.format("%4d %s", e.getValue(), e.getKey())).toList();

			logger.info("{}: {} entr{}", b.trace, String.format("%4d", b.bundle.getEntry().size()),
					(b.bundle.getEntry().size() == 1 ? "y" : "ies"));
			// using list / for-each for output formatting
			for (String r : results)
				logger.info("{}: {}", b.trace, r);
		}
	}

	@Test
	public void insertPolar() throws Exception
	{
		List<BundleWithTrace> bundles = readBundles();

		addBloomFilters(bundles);

		for (BundleWithTrace bundleWithTrace : bundles)
		{
			logger.debug("Posting bundle with {} entries from {} to ", bundleWithTrace.bundle.getEntry().size(),
					bundleWithTrace.trace);
			fhirContext.newRestfulGenericClient(serverBase).transaction().withBundle(bundleWithTrace.bundle).execute();
		}
	}

	private void addBloomFilters(List<BundleWithTrace> bundles)
	{
		bundles.stream().map(BundleWithTrace::bundle).map(Bundle::getEntry).flatMap(List::stream)
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof Patient).map(r -> (Patient) r).forEach(p ->
				{
					byte[] value = new byte[64];
					random.nextBytes(value);

					p.addIdentifier().setSystem("http://www.netzwerk-universitaetsmedizin.de/sid/bloom-filter")
							.setValue(Base64.getEncoder().encodeToString(value)).getType().addCoding()
							.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203").setCode("ANON");
				});
	}

	@Test
	public void testValidatePolar() throws Exception
	{
		List<BundleWithTrace> bundles = readBundles();

		BundleValidator bundleValidator = createValidator();

		for (BundleWithTrace bundle : bundles)
		{
			Bundle validated = bundleValidator.validate(bundle.bundle);
			String resourceString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(validated);

			Path outPath = cacheFolder.resolve(bundle.file.replace(".json", ".out.json"));

			logger.info("Writig validation result for {} to {}", bundle.trace, outPath.toAbsolutePath().toString());
			Files.writeString(outPath, resourceString, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		}
	}

	private List<BundleWithTrace> readBundles() throws IOException
	{
		Set<String> resourcesAndProfiles = new HashSet<>();

		List<Path> bundleZips = StreamSupport
				.stream(FileSystems.newFileSystem(Paths.get("src/test/resources/fhir/Polar.zip")).getRootDirectories()
						.spliterator(), false)
				.flatMap(walkQuite()).filter(p -> p.toString().endsWith(".json.zip")).toList();

		List<BundleWithTrace> bundles = new ArrayList<>();

		for (Path bundleZip : bundleZips)
		{
			List<Path> bundleFiles = StreamSupport
					.stream(FileSystems.newFileSystem(bundleZip).getRootDirectories().spliterator(), false)
					.flatMap(walkQuite()).filter(p -> p.toString().endsWith(".json")).toList();

			for (Path bundleFile : bundleFiles)
			{
				String trace = bundleZip.getFileName().toString() + "/" + bundleFile.getFileName().toString();
				Bundle bundle = readBundle(trace, bundleFile, fhirContext);
				bundle = fixBundle(trace, bundle, resourcesAndProfiles);

				bundles.add(new BundleWithTrace(bundle, trace, bundleFile.getFileName().toString()));
			}
		}

		logger.info("Resources and profiles in test data sets: {}",
				resourcesAndProfiles.stream().sorted().collect(Collectors.joining("\n")));

		return bundles;
	}

	private static Function<? super Path, ? extends Stream<? extends Path>> walkQuite()
	{
		return p ->
		{
			try
			{
				return Files.walk(p);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		};
	}

	private static Bundle readBundle(String trace, Path path, FhirContext context)
	{
		logger.debug("Reading Bundle from {}", trace);
		try (InputStream in = Files.newInputStream(path))
		{
			return context.newJsonParser().parseResource(Bundle.class, in);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}


	private static Bundle fixBundle(String trace, Bundle bundle, Set<String> resourcesAndProfiles)
	{
		Map<String, String> idTranslation = new HashMap<>();

		List<BundleEntryComponent> entries = bundle.getEntry();
		for (int i = 0; i < entries.size(); i++)
		{
			BundleEntryComponent entry = entries.get(i);
			String eTrace = trace + "[" + i + " - " + entry.getResource().getResourceType().name() + "]";

			Resource resource = entry.getResource();
			String newId = "urn:uuid:" + UUID.randomUUID().toString();
			String oldId = resource.getIdElement().getValue();

			logger.debug("{}: {} -> {}", eTrace, oldId, newId);
			idTranslation.put(oldId, newId);

			entry.setFullUrl(newId);
			entry.getRequest().setMethod(HTTPVerb.POST);
			entry.getRequest().setUrl(resource.getResourceType().name());
			resource.setIdElement(null);
		}

		for (int i = 0; i < entries.size(); i++)
		{
			Resource resource = entries.get(i).getResource();
			String eTrace = trace + "[" + i + " - " + resource.getResourceType().name() + "]";

			fixIdentifier(eTrace, resource);

			if (resource instanceof Patient patient)
			{
				// nothing to do
			}
			else if (resource instanceof Encounter encounter)
			{
				fixReference(eTrace + "/subject", encounter.getSubject(), idTranslation);

				for (int d = 0; d < encounter.getDiagnosis().size(); d++)
				{
					DiagnosisComponent diagnosis = encounter.getDiagnosis().get(d);
					fixReference(eTrace + "/diagnosis/condition[" + d + "]", diagnosis.getCondition(), idTranslation);
				}

				fixReference(eTrace + "/partOf", encounter.getPartOf(), idTranslation);
			}
			else if (resource instanceof Procedure procedure)
			{
				fixReference(eTrace + "/subject", procedure.getSubject(), idTranslation);
			}
			else if (resource instanceof Observation observation)
			{
				fixReference(eTrace + "/subject", observation.getSubject(), idTranslation);
				fixReference(eTrace + "/encounter", observation.getEncounter(), idTranslation);
			}
			else if (resource instanceof Condition condition)
			{
				fixReference(eTrace + "/subject", condition.getSubject(), idTranslation);
			}
			else if (resource instanceof Medication medication)
			{
				// nothing to do
			}
			else if (resource instanceof MedicationStatement statement)
			{
				fixReference(eTrace + "/medication", statement.getMedicationReference(), idTranslation);
				fixReference(eTrace + "/subject", statement.getSubject(), idTranslation);
				fixReference(eTrace + "/context", statement.getContext(), idTranslation);
			}
			else if (resource instanceof MedicationAdministration administration)
			{
				fixReference(eTrace + "/medication", administration.getMedicationReference(), idTranslation);
				fixReference(eTrace + "/subject", administration.getSubject(), idTranslation);
				fixReference(eTrace + "/context", administration.getContext(), idTranslation);
			}
			else
				logger.error("{} not supported", eTrace);

			resourcesAndProfiles.addAll(resource.getMeta().getProfile().stream().map(CanonicalType::getValue)
					.map(v -> resource.getResourceType().name() + " -> " + v).toList());
		}

		return bundle;
	}

	private static void fixIdentifier(String eTrace, Resource resource)
	{
		try
		{
			Method method = resource.getClass().getMethod("getIdentifier");
			@SuppressWarnings("unchecked")
			List<Identifier> identifiers = (List<Identifier>) method.invoke(resource);
			for (int i = 0; i < identifiers.size(); i++)
			{
				Identifier identifier = identifiers.get(i);
				// Removing (not replacing) extension, due to HAPI bug.
				if (identifier.getSystem() == null && identifier.getSystemElement().hasExtension())
				{
					logger.warn("{} removing system.extension{}", eTrace + "/identifier[" + i + "]",
							identifier.getExtension().size() > 1 ? "s" : "");
					identifier.getSystemElement().setExtension(null);
				}
				// if (identifier.getSystem() == null && identifier.getSystemElement()
				// .hasExtension("http://terminology.hl7.org/CodeSystem/data-absent-reason"))
				// {
				// logger.warn("{} fixing system.extension{}", eTrace + "/identifier[" + i + "]",
				// identifier.getExtension().size() > 1 ? "s" : "");
				// identifier.getSystemElement().getExtension().stream()
				// .forEach(e -> e.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason"));
				// }
			}
		}
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e)
		{
			logger.warn("unable to fix identifier", e);
		}
	}

	private static void fixReference(String trace, Reference reference, Map<String, String> idTranslation)
	{
		reference.setResource(null);

		String oldReference = reference.getReference();
		if (oldReference != null)
		{
			String newReference = idTranslation.get(oldReference);
			if (newReference != null)
				reference.setReference(newReference);
			else
				logger.warn("{}: No id for {}", trace, oldReference);
		}
		else if (oldReference == null && reference.hasReference() && reference.getReferenceElement_()
				.hasExtension("http://terminology.hl7.org/CodeSystem/data-absent-reason"))
		{
			logger.info(
					"{}: Reference extension.url ...CodeSystem/data-absent-reason -> StructureDefinition/data-absent-reason",
					trace);
			reference.getReferenceElement_()
					.getExtensionsByUrl("http://terminology.hl7.org/CodeSystem/data-absent-reason")
					.forEach(e -> e.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason"));
		}
		else
			logger.info("{}: Reference null", trace);
	}

	private static BundleValidator createValidator()
	{
		try
		{
			ValidationPackageManager validationPackageManager = createValidationPackageManager();

			String[] packages = { "de.basisprofil.r4|1.4.0",
					"de.medizininformatikinitiative.kerndatensatz.diagnose|1.0.4",
					"de.medizininformatikinitiative.kerndatensatz.fall|1.0.1",
					"de.medizininformatikinitiative.kerndatensatz.laborbefund|1.0.6",
					"de.medizininformatikinitiative.kerndatensatz.medikation|1.0.11",
					"de.medizininformatikinitiative.kerndatensatz.person|1.0.16",
					"de.medizininformatikinitiative.kerndatensatz.prozedur|1.0.7" };

			List<ValidationPackageWithDepedencies> packagesWithDependencies = validationPackageManager
					.downloadPackagesWithDependencies(Stream.of(packages).map(ValidationPackageIdentifier::fromString)
							.toArray(ValidationPackageIdentifier[]::new));

			IValidationSupport support = validationPackageManager
					.expandValueSetsAndGenerateStructureDefinitionSnapshots(packagesWithDependencies);

			return validationPackageManager.createBundleValidator(support, packagesWithDependencies);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private static ValidationPackageManager createValidationPackageManager()
			throws IOException, CertificateException, PKCSException, KeyStoreException, NoSuchAlgorithmException
	{
		logger.info("Reading UTF-8 encoded properties from {}", Paths.get("application.properties").toAbsolutePath());
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Paths.get("application.properties"));
				InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}

		String certificateProperty = properties.getProperty(
				"de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate");
		String passwordProperty = properties.getProperty(
				"de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate.private.key.password");
		String privateKeyProperty = properties.getProperty(
				"de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate.private.key");

		assertNotNull(certificateProperty);
		assertNotNull(passwordProperty);
		assertNotNull(privateKeyProperty);

		X509Certificate certificate = PemIo.readX509CertificateFromPem(Paths.get(certificateProperty));
		char[] keyStorePassword = passwordProperty.toCharArray();
		PrivateKey privateKey = PemIo.readPrivateKeyFromPem(Paths.get(privateKeyProperty), keyStorePassword);
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
}
