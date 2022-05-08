package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.GeccoFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.GeccoFhirClientStub;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

public class GeccoClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(GeccoClientFactory.class);

	private static final class GeccoClientStub implements GeccoClient
	{
		final FhirContext fhirContext;
		final String localIdentifierValue;

		GeccoClientStub(FhirContext fhirContext, String localIdentifierValue)
		{
			this.fhirContext = fhirContext;
			this.localIdentifierValue = localIdentifierValue;
		}

		@Override
		public void testConnection()
		{
			logger.warn("Stub implementation, no connection test performed");
		}

		@Override
		public GeccoFhirClient getFhirClient()
		{
			return new GeccoFhirClientStub(this);
		}

		@Override
		public FhirContext getFhirContext()
		{
			return fhirContext;
		}

		@Override
		public Path getSearchBundleOverride()
		{
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		public IGenericClient getGenericFhirClient()
		{
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		public String getLocalIdentifierValue()
		{
			return localIdentifierValue;
		}

		@Override
		public boolean shouldUseChainedParameterNotLogicalReference()
		{
			return false;
		}
	}

	private final Path trustStorePath;
	private final Path certificatePath;
	private final Path privateKeyPath;
	private final char[] privateKeyPassword;

	private final int connectTimeout;
	private final int socketTimeout;
	private final int connectionRequestTimeout;

	private final String geccoServerBase;
	private final String geccoServerBasicAuthUsername;
	private final String geccoServerBasicAuthPassword;
	private final String geccoServerBearerToken;

	private final String proxyUrl;
	private final String proxyUsername;
	private final String proxyPassword;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;
	private final Path searchBundleOverride;
	private final String localIdentifierValue;
	private final Class<GeccoFhirClient> geccoFhirClientClass;
	private final boolean useChainedParameterNotLogicalReference;

	public GeccoClientFactory(Path trustStorePath, Path certificatePath, Path privateKeyPath, char[] privateKeyPassword,
			int connectTimeout, int socketTimeout, int connectionRequestTimeout, String geccoServerBase,
			String geccoServerBasicAuthUsername, String geccoServerBasicAuthPassword, String geccoServerBearerToken,
			String proxyUrl, String proxyUsername, String proxyPassword, boolean hapiClientVerbose,
			FhirContext fhirContext, Path searchBundleOverride, String localIdentifierValue,
			Class<GeccoFhirClient> geccoFhirClientClass, boolean useChainedParameterNotLogicalReference)
	{
		super();
		this.trustStorePath = trustStorePath;
		this.certificatePath = certificatePath;
		this.privateKeyPath = privateKeyPath;
		this.privateKeyPassword = privateKeyPassword;

		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;
		this.connectionRequestTimeout = connectionRequestTimeout;

		this.geccoServerBase = geccoServerBase;
		this.geccoServerBasicAuthUsername = geccoServerBasicAuthUsername;
		this.geccoServerBasicAuthPassword = geccoServerBasicAuthPassword;
		this.geccoServerBearerToken = geccoServerBearerToken;

		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;
		this.searchBundleOverride = searchBundleOverride;
		this.localIdentifierValue = localIdentifierValue;
		this.geccoFhirClientClass = geccoFhirClientClass;
		this.useChainedParameterNotLogicalReference = useChainedParameterNotLogicalReference;
	}

	public void testConnection()
	{
		try
		{
			logger.info(
					"Testing connection to GECCO FHIR server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
							+ " basicAuthUsername {}, basicAuthPassword {}, bearerToken {}, serverBase: {}, proxyUrl {}, proxyUsername, proxyPassword {}}",
					trustStorePath, certificatePath, privateKeyPath, privateKeyPassword != null ? "***" : "null",
					geccoServerBasicAuthUsername, geccoServerBasicAuthPassword != null ? "***" : "null",
					geccoServerBearerToken != null ? "***" : "null", geccoServerBase, proxyUrl, proxyUsername,
					proxyPassword != null ? "***" : "null");

			getGeccoClient().testConnection();
		}
		catch (Exception e)
		{
			logger.error("Error while testing connection to fTTP", e);
		}
	}

	public GeccoClient getGeccoClient()
	{
		if (configured())
			return createGeccoClient();
		else
			return new GeccoClientStub(fhirContext, localIdentifierValue);
	}

	private boolean configured()
	{
		return geccoServerBase != null && !geccoServerBase.isBlank();
	}

	protected GeccoClient createGeccoClient()
	{
		KeyStore trustStore = null;
		char[] keyStorePassword = null;
		if (trustStorePath != null)
		{
			logger.debug("Reading trust-store from {}", trustStorePath.toString());
			trustStore = readTrustStore(trustStorePath);
			keyStorePassword = UUID.randomUUID().toString().toCharArray();
		}

		KeyStore keyStore = null;
		if (certificatePath != null && privateKeyPath != null)
		{
			logger.debug("Creating key-store from {} and {} with password {}", certificatePath.toString(),
					privateKeyPath.toString(), keyStorePassword != null ? "***" : "null");
			keyStore = readKeyStore(certificatePath, privateKeyPath, privateKeyPassword, keyStorePassword);
		}

		return new GeccoClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout, geccoServerBasicAuthUsername, geccoServerBasicAuthPassword,
				geccoServerBearerToken, geccoServerBase, proxyUrl, proxyUsername, proxyPassword, hapiClientVerbose,
				fhirContext, searchBundleOverride, localIdentifierValue, geccoFhirClientClass,
				useChainedParameterNotLogicalReference);
	}

	private KeyStore readTrustStore(Path trustPath)
	{
		try
		{
			return CertificateReader.allFromCer(trustPath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore readKeyStore(Path certificatePath, Path keyPath, char[] keyPassword, char[] keyStorePassword)
	{
		try
		{
			PrivateKey privateKey = PemIo.readPrivateKeyFromPem(keyPath, keyPassword);
			X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);

			return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate },
					UUID.randomUUID().toString(), keyStorePassword);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}
}
