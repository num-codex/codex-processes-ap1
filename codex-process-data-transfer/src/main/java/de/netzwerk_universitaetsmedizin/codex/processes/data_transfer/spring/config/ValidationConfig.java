package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bouncycastle.pkcs.PKCSException;
import org.highmed.dsf.fhir.json.ObjectMapperFactory;
import org.highmed.dsf.fhir.validation.ValueSetExpander;
import org.highmed.dsf.fhir.validation.ValueSetExpanderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageClientJersey;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageClientWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageIdentifier;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageManager;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValidationPackageManagerImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpanderWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClientJersey;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClientWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.StructureDefinitionModifier;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

@Configuration
public class ValidationConfig
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationConfig.class);

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package:de.gecco|1.0.5}")
	private String validationPackage;

	@Value("#{'${de.netzwerk.universitaetsmedizin.codex.gecco.validation.structureDefinitionModifierClasses:"
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.ClosedTypeSlicingRemover,"
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.MiiModuleLabObservationLab10IdentifierRemover,"
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.GeccoRadiologyProceduresCodingSliceMinFixer"
			+ "}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> structureDefinitionModifierClasses;

	@Value("#{'${de.netzwerk.universitaetsmedizin.codex.gecco.validation.packagesToIgnore:hl7.fhir.r4.core|4.0.1}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> packagesToIgnore;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.cacheFolder:#{null}}")
	private String packageCacheFolder;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.server.baseUrl:https://packages.simplifier.net}")
	private String packageServerBaseUrl;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.trust.certificates:#{null}}")
	private String packageClientTrustCertificates;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.authentication.certificate:#{null}}")
	private String packageClientCertificate;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.authentication.certificate.private.key:#{null}}")
	private String packageClientCertificatePrivateKey;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.authentication.certificate.private.key.password:#{null}}")
	private char[] packageClientCertificatePrivateKeyPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.authentication.basic.username:#{null}}")
	private String packageClientBasicAuthUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.authentication.basic.password:#{null}}")
	private char[] packageClientBasicAuthPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.proxy.schemeHostPort:#{null}}")
	private String packageClientProxySchemeHostPort;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.proxy.username:#{null}}")
	private String packageClientProxyUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.proxy.password:#{null}}")
	private char[] packageClientProxyPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.timeout.connect:10000}")
	private int packageClientConnectTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.package.client.timeout.read:300000}")
	private int packageClientReadTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.cacheFolder:#{null}}")
	private String valueSetCacheFolder;

	// TODO default should be MII ontology server
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.server.baseUrl:https://r4.ontoserver.csiro.au/fhir}")
	private String valueSetExpansionServerBaseUrl;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.trust.certificates:#{null}}")
	private String valueSetExpansionClientTrustCertificates;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.authentication.certificate:#{null}}")
	private String valueSetExpansionClientCertificate;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.authentication.certificate.private.key:#{null}}")
	private String valueSetExpansionClientCertificatePrivateKey;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.authentication.certificate.private.key.password:#{null}}")
	private char[] valueSetExpansionClientCertificatePrivateKeyPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.authentication.basic.username:#{null}}")
	private String valueSetExpansionClientBasicAuthUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.authentication.basic.password:#{null}}")
	private char[] valueSetExpansionClientBasicAuthPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.proxy.schemeHostPort:#{null}}")
	private String valueSetExpansionClientProxySchemeHostPort;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.proxy.username:#{null}}")
	private String valueSetExpansionClientProxyUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.proxy.password:#{null}}")
	private char[] valueSetExpansionClientProxyPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.timeout.connect:10000}")
	private int valueSetExpansionClientConnectTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.valueset.expansion.client.timeout.read:300000}")
	private int valueSetExpansionClientReadTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.validation.structuredefinition.cacheFolder:#{null}}")
	private String structureDefinitionCacheFolder;

	@Value("${java.io.tmpdir}")
	private String systemTempFolder;

	@Autowired
	private FhirContext fhirContext;

	@Bean
	public ValidationPackageIdentifier validationPackageIdentifier()
	{
		if (validationPackage == null || validationPackage.isBlank())
			throw new IllegalArgumentException("Validation package not specified");

		return ValidationPackageIdentifier.fromString(validationPackage);
	}

	@Bean
	public ValidationPackageManager validationPackageManager()
	{
		List<StructureDefinitionModifier> structureDefinitionModifiers = structureDefinitionModifierClasses.stream()
				.map(this::createStructureDefinitionModifier).collect(Collectors.toList());

		return new ValidationPackageManagerImpl(validationPackageClient(), valueSetExpansionClient(), objectMapper(),
				fhirContext, internalSnapshotGeneratorFactory(), internalValueSetExpanderFactory(),
				packagesToIgnore.stream().map(ValidationPackageIdentifier::fromString).collect(Collectors.toList()),
				structureDefinitionModifiers);
	}

	private StructureDefinitionModifier createStructureDefinitionModifier(String className)
	{
		try
		{
			Class<?> modifierClass = Class.forName(className);
			if (StructureDefinitionModifier.class.isAssignableFrom(modifierClass))
				return (StructureDefinitionModifier) modifierClass.getConstructor().newInstance();
			else
				throw new IllegalArgumentException(
						"Class " + className + " not compatible with " + StructureDefinitionModifier.class.getName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException | SecurityException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Path cacheFolder(String cacheFolderType, String cacheFolder)
	{
		try
		{
			Path cacheFolderPath;
			if (cacheFolder != null)
				cacheFolderPath = Paths.get(cacheFolder);
			else
			{
				cacheFolderPath = Paths.get(systemTempFolder).resolve("codex_gecco_validation_cache")
						.resolve(cacheFolderType);
				Files.createDirectories(cacheFolderPath);
				logger.debug("Cache folder for typ {} created at {}", cacheFolderType,
						cacheFolderPath.toAbsolutePath().toString());
			}

			if (!Files.isWritable(cacheFolderPath))
				throw new IOException("Cache folder for type " + cacheFolderType + " + at "
						+ cacheFolderPath.toAbsolutePath().toString() + " not writable");
			else
				return cacheFolderPath;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Path checkReadable(String file)
	{
		if (file == null)
			return null;
		else
		{
			Path path = Paths.get(file);

			if (!Files.isReadable(path))
				throw new RuntimeException(path.toString() + " not readable");

			return path;
		}
	}

	private KeyStore trustStore(String trustStoreType, String trustCertificatesFile)
	{
		if (trustCertificatesFile == null)
			return null;

		Path trustCertificatesPath = checkReadable(trustCertificatesFile);

		try
		{
			logger.debug("Creating trust-store for {} from {}", trustStoreType, trustCertificatesPath.toString());
			return CertificateReader.allFromCer(trustCertificatesPath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore keyStore(String keyStoreType, String clientCertificateFile, String clientCertificatePrivateKeyFile,
			char[] clientCertificatePrivateKeyPassword, char[] keyStorePassword)
	{
		if ((clientCertificateFile != null) != (clientCertificatePrivateKeyFile != null))
			throw new IllegalArgumentException(keyStoreType + " certificate or private-key not specified");
		else if (clientCertificateFile == null && clientCertificatePrivateKeyFile == null)
			return null;

		Path clientCertificatePath = checkReadable(clientCertificateFile);
		Path clientCertificatePrivateKeyPath = checkReadable(clientCertificatePrivateKeyFile);

		try
		{
			PrivateKey privateKey = PemIo.readPrivateKeyFromPem(clientCertificatePrivateKeyPath,
					clientCertificatePrivateKeyPassword);
			X509Certificate certificate = PemIo.readX509CertificateFromPem(clientCertificatePath);

			logger.debug("Creating key-store for {} from {} and {} with password {}", clientCertificatePath.toString(),
					clientCertificatePrivateKeyPath.toString(),
					clientCertificatePrivateKeyPassword != null ? "***" : "null");
			return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate },
					UUID.randomUUID().toString(), keyStorePassword);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Bean
	public ValidationPackageClient validationPackageClient()
	{
		return new ValidationPackageClientWithFileSystemCache(packageCacheFolder(), objectMapper(),
				validationPackageClientJersey());
	}

	@Bean
	public Path packageCacheFolder()
	{
		return cacheFolder("Package", packageCacheFolder);
	}

	private ValidationPackageClientJersey validationPackageClientJersey()
	{
		if ((packageClientBasicAuthUsername != null) != (packageClientBasicAuthPassword != null))
		{
			throw new IllegalArgumentException(
					"Package client basic authentication username or password not specified");
		}

		if ((packageClientProxyUsername != null) != (packageClientProxyPassword != null))
		{
			throw new IllegalArgumentException("Package client proxy username or password not specified");
		}

		KeyStore packageClientTrustStore = trustStore("FHIR package client", packageClientTrustCertificates);
		char[] packageClientKeyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore packageClientKeyStore = keyStore("FHIR package client", packageClientCertificate,
				packageClientCertificatePrivateKey, packageClientCertificatePrivateKeyPassword,
				packageClientKeyStorePassword);

		return new ValidationPackageClientJersey(packageServerBaseUrl, packageClientTrustStore, packageClientKeyStore,
				packageClientKeyStore == null ? null : packageClientKeyStorePassword, packageClientBasicAuthUsername,
				packageClientBasicAuthPassword, packageClientProxySchemeHostPort, packageClientProxyUsername,
				packageClientProxyPassword, packageClientConnectTimeout, packageClientReadTimeout);
	}

	@Bean
	public ValueSetExpansionClient valueSetExpansionClient()
	{
		return new ValueSetExpansionClientWithFileSystemCache(valueSetCacheFolder(), fhirContext,
				valueSetExpansionClientJersey());
	}

	@Bean
	public Path valueSetCacheFolder()
	{
		return cacheFolder("ValueSet", valueSetCacheFolder);
	}

	private ValueSetExpansionClient valueSetExpansionClientJersey()
	{
		if ((valueSetExpansionClientBasicAuthUsername != null) != (valueSetExpansionClientBasicAuthPassword != null))
		{
			throw new IllegalArgumentException(
					"ValueSet expansion client basic authentication username or password not specified");
		}

		if ((valueSetExpansionClientProxyUsername != null) != (valueSetExpansionClientProxyPassword != null))
		{
			throw new IllegalArgumentException("ValueSet expansion client proxy username or password not specified");
		}

		KeyStore valueSetExpansionClientTrustStore = trustStore("ValueSet expansion client",
				valueSetExpansionClientTrustCertificates);
		char[] valueSetExpansionClientKeyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore valueSetExpansionClientKeyStore = keyStore("ValueSet expansion client",
				valueSetExpansionClientCertificate, valueSetExpansionClientCertificatePrivateKey,
				valueSetExpansionClientCertificatePrivateKeyPassword, valueSetExpansionClientKeyStorePassword);

		return new ValueSetExpansionClientJersey(valueSetExpansionServerBaseUrl, valueSetExpansionClientTrustStore,
				valueSetExpansionClientKeyStore,
				valueSetExpansionClientKeyStore == null ? null : valueSetExpansionClientKeyStorePassword,
				valueSetExpansionClientBasicAuthUsername, valueSetExpansionClientBasicAuthPassword,
				valueSetExpansionClientProxySchemeHostPort, valueSetExpansionClientProxyUsername,
				valueSetExpansionClientProxyPassword, valueSetExpansionClientConnectTimeout,
				valueSetExpansionClientReadTimeout, objectMapper(), fhirContext);
	}

	@Bean
	public ObjectMapper objectMapper()
	{
		return ObjectMapperFactory.createObjectMapper(fhirContext);
	}

	@Bean
	public BiFunction<FhirContext, IValidationSupport, PluginSnapshotGenerator> internalSnapshotGeneratorFactory()
	{
		return (fc, vs) -> new PluginSnapshotGeneratorWithFileSystemCache(structureDefinitionCacheFolder(), fc,
				new PluginSnapshotGeneratorImpl(fc, vs));
	}

	@Bean
	public Path structureDefinitionCacheFolder()
	{
		return cacheFolder("StructureDefinition", structureDefinitionCacheFolder);
	}

	@Bean
	public BiFunction<FhirContext, IValidationSupport, ValueSetExpander> internalValueSetExpanderFactory()
	{
		return (fc, vs) -> new ValueSetExpanderWithFileSystemCache(valueSetCacheFolder(), fc,
				new ValueSetExpanderImpl(fc, vs));
	}
}
