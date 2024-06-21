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
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bouncycastle.pkcs.PKCSException;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations.BindingStrength;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidatorFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidatorFactoryImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorWithFileSystemCache;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.PluginSnapshotGeneratorWithModifiers;
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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.ValueSetExpansionClientWithModifiers;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.StructureDefinitionModifier;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set.ValueSetModifier;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.bpe.v1.config.ProxyConfig;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.ValueSetExpander;
import dev.dsf.fhir.validation.ValueSetExpanderImpl;
import jakarta.ws.rs.WebApplicationException;

@Configuration
public class ValidationConfig
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationConfig.class);

	public static enum TerminologyServerConnectionTestStatus
	{
		OK, NOT_OK, DISABLED
	}

	@ProcessDocumentation(description = "Enables/disables local FHIR validation, set to `false` to skip validation", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation:true}")
	private boolean validationEnabled;

	@ProcessDocumentation(description = "FHIR implementation guide package used to validated resources, specify as `name|version`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("#{'${de.netzwerk.universitaetsmedizin.rdp.validation.package:de.basisprofil.r4|1.4.0,de.medizininformatikinitiative.kerndatensatz.meta|1.0.3,de.medizininformatikinitiative.kerndatensatz.person|2024.0.0-ballot,de.medizininformatikinitiative.kerndatensatz.fall|2024.0.0-ballot,de.medizininformatikinitiative.kerndatensatz.mikrobiologie|2024.0.0}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> validationPackages;

	@ProcessDocumentation(description = "FHIR implementation guide packages that do not need to be downloaded, list with `name|version` values", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("#{'${de.netzwerk.universitaetsmedizin.rdp.validation.package.noDownload:hl7.fhir.r4.core|4.0.1}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> noDownloadPackages;

	@ProcessDocumentation(description = "Folder for storing downloaded FHIR implementation guide packages", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.cacheFolder:${java.io.tmpdir}/rdp_validation_cache/Package}")
	private String packageCacheFolder;

	@ProcessDocumentation(description = "URL of the FHIR implementation guide package server to download validation packages from", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.server.baseUrl:https://packages.simplifier.net}")
	private String packageServerBaseUrl;

	@ProcessDocumentation(description = "PEM encoded file with trusted certificates to validate the server-certificate of the FHIR implementation guide package server, uses the JVMs trust store if not specified", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure", example = "/run/secrets/validation_package_server_ca.pem")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.trust.certificates:#{null}}")
	private String packageClientTrustCertificates;

	@ProcessDocumentation(description = "PEM encoded file with client-certificate, if the FHIR implementation guide package server requires mutual TLS authentication", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure", example = "/run/secrets/validation_package_server_client_certificate.pem")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.authentication.certificate:#{null}}")
	private String packageClientCertificate;

	@ProcessDocumentation(description = "PEM encoded file with private-key for the client-certificate defined via `de.netzwerk.universitaetsmedizin.rdp.validation.package.client.authentication.certificate`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure", example = "/run/secrets/validation_package_server_client_certificate_private_key.pem")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.authentication.certificate.private.key:#{null}}")
	private String packageClientCertificatePrivateKey;

	@ProcessDocumentation(description = "Password to decrypt the private-key defined via `de.netzwerk.universitaetsmedizin.rdp.validation.package.client.authentication.certificate.private.key`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/validation_package_server_client_certificate_private_key.pem.password")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.authentication.certificate.private.key.password:#{null}}")
	private char[] packageClientCertificatePrivateKeyPassword;

	@ProcessDocumentation(description = "Basic authentication username to authenticate against the FHIR implementation guide package server, set if the server requests authentication using basic authentication", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.authentication.basic.username:#{null}}")
	private String packageClientBasicAuthUsername;

	@ProcessDocumentation(description = "Basic authentication password to authenticate against the FHIR implementation guide package server, set if the server requests authentication using basic authentication ", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/validation_package_server_basicauth.password")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.authentication.basic.password:#{null}}")
	private char[] packageClientBasicAuthPassword;

	@ProcessDocumentation(description = "Connection timeout in milliseconds used when accessing the FHIR implementation guide package server, time until a connection needs to be established before aborting", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.timeout.connect:10000}")
	private int packageClientConnectTimeout;

	@ProcessDocumentation(description = "Read timeout in milliseconds used when accessing the FHIR implementation guide package server, time until the server needs to send a reply before aborting", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.timeout.read:300000}")
	private int packageClientReadTimeout;

	@ProcessDocumentation(description = "To enable verbose logging of requests and replies to the FHIR implementation guide package server, set to `true`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.package.client.verbose:false}")
	private boolean packageClientVerbose;

	@ProcessDocumentation(description = "ValueSets found in the StructureDefinitions with the specified binding strength will be expanded", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("#{'${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.bindingStrength:required,extensible,preferred,example}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> valueSetExpansionBindingStrengths;

	@ProcessDocumentation(description = "Folder for storing expanded ValueSets", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.cacheFolder:${java.io.tmpdir}/rdp_validation_cache/ValueSet}")
	private String valueSetCacheFolder;

	@ProcessDocumentation(description = "Base URL of the terminology server used to expand FHIR ValueSets", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Specify a local terminology server to improve ValueSet expansion speed")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.server.baseUrl:https://terminology-highmed.medic.medfak.uni-koeln.de/fhir}")
	private String valueSetExpansionServerBaseUrl;

	@ProcessDocumentation(description = "PEM encoded file with trusted certificates to validate the server-certificate of the terminology server, uses the JVMs trust store if not specified", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure", example = "/run/secrets/terminology_server_ca.pem")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.trust.certificates:#{null}}")
	private String valueSetExpansionClientTrustCertificates;

	@ProcessDocumentation(description = "PEM encoded file with client-certificate, if the terminology server requires mutual TLS authentication", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure", example = "/run/secrets/terminology_server_client_certificate.pem")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate:#{null}}")
	private String valueSetExpansionClientCertificate;

	@ProcessDocumentation(description = "PEM encoded file with private-key for the client-certificate defined via `de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure", example = "/run/secrets/terminology_server_client_certificate_private_key.pem")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate.private.key:#{null}}")
	private String valueSetExpansionClientCertificatePrivateKey;

	@ProcessDocumentation(description = "Password to decrypt the private-key defined via `de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate.private.key`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/terminology_server_client_certificate_private_key.pem.password")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.certificate.private.key.password:#{null}}")
	private char[] valueSetExpansionClientCertificatePrivateKeyPassword;

	@ProcessDocumentation(description = "Basic authentication username to authenticate against the terminology server, set if the server requests authentication using basic authentication", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.basic.username:#{null}}")
	private String valueSetExpansionClientBasicAuthUsername;

	@ProcessDocumentation(description = "Basic authentication password to authenticate against the terminology server, set if the server requests authentication using basic authentication ", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/terminology_server_basicauth.password")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.authentication.basic.password:#{null}}")
	private char[] valueSetExpansionClientBasicAuthPassword;

	@ProcessDocumentation(description = "Connection timeout in milliseconds used when accessing the terminology server, time until a connection needs to be established before aborting", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.timeout.connect:10000}")
	private int valueSetExpansionClientConnectTimeout;

	@ProcessDocumentation(description = "Read timeout in milliseconds used when accessing the terminology server, time until the server needs to send a reply before aborting", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.timeout.read:300000}")
	private int valueSetExpansionClientReadTimeout;

	@ProcessDocumentation(description = "To enable verbose logging of requests and replies to the terminology server, set to `true`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.client.verbose:false}")
	private boolean valueSetExpansionClientVerbose;

	@ProcessDocumentation(description = "List of ValueSet modifier classes, modifiers are executed before atempting to expand a ValueSet and after", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("#{'${de.netzwerk.universitaetsmedizin.rdp.validation.valueset.expansion.modifierClasses:"
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set.KdsMikrobiologieBugFixer"
			+ ","
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set.MissingEntriesIncluder"
			+ "}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> valueSetModifierClasses;

	@ProcessDocumentation(description = "List of StructureDefinition modifier classes, modifiers are executed before atempting to generate a StructureDefinition snapshot", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("#{'${de.netzwerk.universitaetsmedizin.rdp.validation.structuredefinition.modifierClasses:"
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.ClosedTypeSlicingRemover,"
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.IdentifierRemover,"
			+ "de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.SliceMinFixer"
			+ "}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> structureDefinitionModifierClasses;

	@ProcessDocumentation(description = "Folder for storing generated StructureDefinition snapshots", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.structuredefinition.cacheFolder:${java.io.tmpdir}/rdp_validation_cache/StructureDefinition}")
	private String structureDefinitionCacheFolder;

	@Value("${java.io.tmpdir}")
	private String systemTempFolder;

	// not using process plugin api to enable reuse of this config class in stand-alone validator
	@Autowired
	private FhirContext fhirContext;

	// not using process plugin api to enable reuse of this config class in stand-alone validator
	@Autowired
	private ObjectMapper objectMapper;

	// not using process plugin api to enable reuse of this config class in stand-alone validator
	@Autowired
	private ProxyConfig proxyConfig;

	@Bean
	public ValidationPackageManager validationPackageManager()
	{
		List<ValidationPackageIdentifier> noDownload = noDownloadPackages.stream()
				.map(ValidationPackageIdentifier::fromString).collect(Collectors.toList());
		EnumSet<BindingStrength> bindingStrengths = EnumSet.copyOf(
				valueSetExpansionBindingStrengths.stream().map(BindingStrength::fromCode).collect(Collectors.toList()));

		return new ValidationPackageManagerImpl(validationPackageClient(), valueSetExpansionClient(), objectMapper,
				fhirContext, internalSnapshotGeneratorFactory(), internalValueSetExpanderFactory(), noDownload,
				bindingStrengths);
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

	@Bean
	public BiFunction<FhirContext, IValidationSupport, SnapshotGenerator> internalSnapshotGeneratorFactory()
	{
		List<StructureDefinitionModifier> structureDefinitionModifiers = structureDefinitionModifierClasses.stream()
				.map(this::createStructureDefinitionModifier).collect(Collectors.toList());

		return (fc, vs) -> new PluginSnapshotGeneratorWithFileSystemCache(structureDefinitionCacheFolder(), fc,
				new PluginSnapshotGeneratorWithModifiers(new PluginSnapshotGeneratorImpl(fc, vs),
						structureDefinitionModifiers));
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

	private Path cacheFolder(String cacheFolderType, String cacheFolder)
	{
		Objects.requireNonNull(cacheFolder, "cacheFolder");
		Path cacheFolderPath = Paths.get(cacheFolder);

		try
		{
			if (cacheFolderPath.startsWith(systemTempFolder))
			{
				Files.createDirectories(cacheFolderPath);
				logger.debug("Cache folder for type {} created at {}", cacheFolderType,
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

			logger.debug("Creating key-store for {} from {} and {} with password {}", keyStoreType,
					clientCertificatePath.toString(), clientCertificatePrivateKeyPath.toString(),
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
		return new ValidationPackageClientWithFileSystemCache(packageCacheFolder(), objectMapper,
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

		String proxyUrl = null, proxyUsername = null;
		char[] proxyPassword = null;
		if (proxyConfig.isEnabled() && !proxyConfig.isNoProxyUrl(packageServerBaseUrl))
		{
			proxyUrl = proxyConfig.getUrl();
			proxyUsername = proxyConfig.getUsername();
			proxyPassword = proxyConfig.getPassword() == null ? null : proxyConfig.getPassword();
		}

		KeyStore packageClientTrustStore = trustStore("FHIR package client", packageClientTrustCertificates);
		char[] packageClientKeyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore packageClientKeyStore = keyStore("FHIR package client", packageClientCertificate,
				packageClientCertificatePrivateKey, packageClientCertificatePrivateKeyPassword,
				packageClientKeyStorePassword);

		return new ValidationPackageClientJersey(packageServerBaseUrl, packageClientTrustStore, packageClientKeyStore,
				packageClientKeyStore == null ? null : packageClientKeyStorePassword, packageClientBasicAuthUsername,
				packageClientBasicAuthPassword, proxyUrl, proxyUsername, proxyPassword, packageClientConnectTimeout,
				packageClientReadTimeout, packageClientVerbose);
	}

	@Bean
	public ValueSetExpansionClient valueSetExpansionClient()
	{
		List<ValueSetModifier> modifiers = valueSetModifierClasses.stream().map(this::createValueSetModifier)
				.collect(Collectors.toList());

		return new ValueSetExpansionClientWithFileSystemCache(valueSetCacheFolder(), fhirContext,
				new ValueSetExpansionClientWithModifiers(valueSetExpansionClientJersey(), modifiers));
	}

	private ValueSetModifier createValueSetModifier(String className)
	{
		try
		{
			Class<?> modifierClass = Class.forName(className);
			if (ValueSetModifier.class.isAssignableFrom(modifierClass))
				return (ValueSetModifier) modifierClass.getConstructor().newInstance();
			else
				throw new IllegalArgumentException(
						"Class " + className + " not compatible with " + ValueSetModifier.class.getName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException | SecurityException e)
		{
			throw new RuntimeException(e);
		}
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

		String proxyUrl = null, proxyUsername = null;
		char[] proxyPassword = null;
		if (proxyConfig.isEnabled() && !proxyConfig.isNoProxyUrl(valueSetExpansionServerBaseUrl))
		{
			proxyUrl = proxyConfig.getUrl();
			proxyUsername = proxyConfig.getUsername();
			proxyPassword = proxyConfig.getPassword() == null ? null : proxyConfig.getPassword();
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
				valueSetExpansionClientBasicAuthUsername, valueSetExpansionClientBasicAuthPassword, proxyUrl,
				proxyUsername, proxyPassword, valueSetExpansionClientConnectTimeout, valueSetExpansionClientReadTimeout,
				valueSetExpansionClientVerbose, objectMapper, fhirContext);
	}

	public TerminologyServerConnectionTestStatus testConnectionToTerminologyServer()
	{
		if (validationEnabled)
			logger.info(
					"Testing connection to terminology server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
							+ " basicAuthUsername: {}, basicAuthPassword: {}, serverBase: {}, proxy: values from 'DEV_DSF_PROXY'... config}",
					valueSetExpansionClientTrustCertificates, valueSetExpansionClientCertificate,
					valueSetExpansionClientCertificatePrivateKey,
					valueSetExpansionClientCertificatePrivateKeyPassword != null ? "***" : "null",
					valueSetExpansionClientBasicAuthUsername,
					valueSetExpansionClientBasicAuthPassword != null ? "***" : "null", valueSetExpansionServerBaseUrl);
		else
			logger.warn(
					"Not testing connection to terminology server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
							+ " basicAuthUsername: {}, basicAuthPassword: {}, serverBase: {}, proxy: values from 'DEV_DSF_PROXY'... config}, validation disabled",
					valueSetExpansionClientTrustCertificates, valueSetExpansionClientCertificate,
					valueSetExpansionClientCertificatePrivateKey,
					valueSetExpansionClientCertificatePrivateKeyPassword != null ? "***" : "null",
					valueSetExpansionClientBasicAuthUsername,
					valueSetExpansionClientBasicAuthPassword != null ? "***" : "null", valueSetExpansionServerBaseUrl);

		if (!validationEnabled)
			return TerminologyServerConnectionTestStatus.DISABLED;

		try
		{
			CapabilityStatement metadata = valueSetExpansionClient().getMetadata();
			logger.info("Connection test OK: {} - {}", metadata.getSoftware().getName(),
					metadata.getSoftware().getVersion());
			return TerminologyServerConnectionTestStatus.OK;
		}
		catch (Exception e)
		{
			if (e instanceof WebApplicationException)
			{
				String response = ((WebApplicationException) e).getResponse().readEntity(String.class);
				logger.error("Connection test failed: {} - {}", e.getMessage(), response);
			}
			else
				logger.error("Connection test failed: {}", e.getMessage());

			return TerminologyServerConnectionTestStatus.NOT_OK;
		}
	}

	@Bean
	public BundleValidatorFactory bundleValidatorFactory()
	{
		return new BundleValidatorFactoryImpl(validationEnabled, validationPackageManager(),
				validationPackageIdentifiers());
	}

	@Bean
	public List<ValidationPackageIdentifier> validationPackageIdentifiers()
	{
		if (validationPackages == null || validationPackages.isEmpty())
			throw new IllegalArgumentException("Validation packages not specified");

		return validationPackages.stream().map(ValidationPackageIdentifier::fromString).toList();
	}
}
