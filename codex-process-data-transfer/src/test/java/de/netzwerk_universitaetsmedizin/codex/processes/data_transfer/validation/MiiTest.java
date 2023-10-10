package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import org.bouncycastle.pkcs.PKCSException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.fhir.validation.ValueSetExpanderImpl;

public class MiiTest
{
	private static final Logger logger = LoggerFactory.getLogger(MiiTest.class);

	private static final Path cacheFolder = Paths.get("target");
	private static final FhirContext fhirContext = FhirContext.forR4();
	private static final ObjectMapper mapper = JsonMapper.builder().serializationInclusion(Include.NON_NULL)
			.serializationInclusion(Include.NON_EMPTY).disable(MapperFeature.AUTO_DETECT_CREATORS)
			.disable(MapperFeature.AUTO_DETECT_FIELDS).disable(MapperFeature.AUTO_DETECT_SETTERS).build();

	@Test
	public void testInit() throws Exception
	{
		ValidationPackageManager validationPackageManager = createValidationPackageManager();

		String[] packages = { "de.basisprofil.r4|1.4.0", "de.medizininformatikinitiative.kerndatensatz.diagnose|1.0.4",
				"de.medizininformatikinitiative.kerndatensatz.fall|1.0.1",
				"de.medizininformatikinitiative.kerndatensatz.laborbefund|1.0.6",
				"de.medizininformatikinitiative.kerndatensatz.medikation|1.0.11",
				"de.medizininformatikinitiative.kerndatensatz.person|1.0.16",
				"de.medizininformatikinitiative.kerndatensatz.prozedur|1.0.7" };

		List<ValidationPackageWithDepedencies> packagesWithDependencies = validationPackageManager
				.downloadPackagesWithDependencies(
						Stream.of(packages).map(ValidationPackageIdentifier::fromString).toList());

		IValidationSupport support = validationPackageManager
				.expandValueSetsAndGenerateStructureDefinitionSnapshots(packagesWithDependencies);

		BundleValidator validator = validationPackageManager.createBundleValidator(support, packagesWithDependencies);
		assertNotNull(validator);

		for (ValidationPackageWithDepedencies p : packagesWithDependencies)
		{
			logger.info(p.getName() + "|" + p.getVersion());
			for (ValidationPackage d : p.getDependencies())
				logger.info("\t" + d.getName() + "|" + d.getVersion());
		}
	}

	private ValidationPackageManager createValidationPackageManager()
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
