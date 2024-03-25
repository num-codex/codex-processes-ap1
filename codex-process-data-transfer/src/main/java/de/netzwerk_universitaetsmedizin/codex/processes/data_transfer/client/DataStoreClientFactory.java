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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClientStub;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

public class DataStoreClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(DataStoreClientFactory.class);

	private static final class DataStoreClientStub implements DataStoreClient
	{
		final FhirContext fhirContext;
		final DataLogger dataLogger;

		DataStoreClientStub(FhirContext fhirContext, DataLogger dataLogger)
		{
			this.fhirContext = fhirContext;
			this.dataLogger = dataLogger;
		}

		@Override
		public String getServerBase()
		{
			return null;
		}

		@Override
		public FhirContext getFhirContext()
		{
			return fhirContext;
		}

		@Override
		public void testConnection()
		{
			logger.warn("Stub implementation, no connection test performed");
		}

		@Override
		public DataStoreFhirClient getFhirClient()
		{
			return new DataStoreFhirClientStub(this, dataLogger);
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

	private final String dataStoreServerBase;
	private final String dataStoreServerBasicAuthUsername;
	private final String dataStoreServerBasicAuthPassword;
	private final String dataStoreServerBearerToken;

	private final String proxyUrl;
	private final String proxyUsername;
	private final String proxyPassword;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;
	private final Path searchBundleOverride;
	private final Class<DataStoreFhirClient> dataStoreFhirClientClass;
	private final boolean useChainedParameterNotLogicalReference;

	private final DataLogger dataLogger;

	public DataStoreClientFactory(Path trustStorePath, Path certificatePath, Path privateKeyPath,
			char[] privateKeyPassword, int connectTimeout, int socketTimeout, int connectionRequestTimeout,
			String dataStoreServerBase, String dataStoreServerBasicAuthUsername,
			String dataStoreServerBasicAuthPassword, String dataStoreServerBearerToken, String proxyUrl,
			String proxyUsername, String proxyPassword, boolean hapiClientVerbose, FhirContext fhirContext,
			Path searchBundleOverride, Class<DataStoreFhirClient> dataStoreFhirClientClass,
			boolean useChainedParameterNotLogicalReference, DataLogger dataLogger)
	{
		this.trustStorePath = trustStorePath;
		this.certificatePath = certificatePath;
		this.privateKeyPath = privateKeyPath;
		this.privateKeyPassword = privateKeyPassword;

		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;
		this.connectionRequestTimeout = connectionRequestTimeout;

		this.dataStoreServerBase = dataStoreServerBase;
		this.dataStoreServerBasicAuthUsername = dataStoreServerBasicAuthUsername;
		this.dataStoreServerBasicAuthPassword = dataStoreServerBasicAuthPassword;
		this.dataStoreServerBearerToken = dataStoreServerBearerToken;

		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;
		this.searchBundleOverride = searchBundleOverride;
		this.dataStoreFhirClientClass = dataStoreFhirClientClass;
		this.useChainedParameterNotLogicalReference = useChainedParameterNotLogicalReference;

		this.dataLogger = dataLogger;
	}

	public String getServerBase()
	{
		return dataStoreServerBase;
	}

	public void testConnection()
	{
		try
		{
			logger.info(
					"Testing connection to Data Store FHIR server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
							+ " basicAuthUsername: {}, basicAuthPassword: {}, bearerToken: {}, serverBase: {}, proxy: values from 'DEV_DSF_PROXY'... config}",
					trustStorePath, certificatePath, privateKeyPath, privateKeyPassword != null ? "***" : "null",
					dataStoreServerBasicAuthUsername, dataStoreServerBasicAuthPassword != null ? "***" : "null",
					dataStoreServerBearerToken != null ? "***" : "null", dataStoreServerBase);

			getDataStoreClient().testConnection();
		}
		catch (Exception e)
		{
			logger.error("Error while testing connection to Data Store FHIR server", e);
		}
	}

	public DataStoreClient getDataStoreClient()
	{
		if (configured())
			return createDataStoreClient();
		else
			return new DataStoreClientStub(fhirContext, dataLogger);
	}

	private boolean configured()
	{
		return dataStoreServerBase != null && !dataStoreServerBase.isBlank();
	}

	protected DataStoreClient createDataStoreClient()
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

		return new DataStoreClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout, dataStoreServerBasicAuthUsername, dataStoreServerBasicAuthPassword,
				dataStoreServerBearerToken, dataStoreServerBase, proxyUrl, proxyUsername, proxyPassword,
				hapiClientVerbose, fhirContext, searchBundleOverride, dataStoreFhirClientClass,
				useChainedParameterNotLogicalReference, dataLogger);
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
