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
import java.util.Map;
import java.util.UUID;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClientStub;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ReceiveDataStoreConfig;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

public class DataStoreClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(DataStoreClientFactory.class);
	private static final String DEFAULT_DATA_STORE = "default";

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

	private final Map<String, ReceiveDataStoreConfig.DataStoreConnectionValues> dataStoreConnectionMap;

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
			String dataStoreServerBasicAuthPassword, String dataStoreServerBearerToken,
			Map<String, ReceiveDataStoreConfig.DataStoreConnectionValues> dataStoreConnectionMap, String proxyUrl,
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

		this.dataStoreConnectionMap = dataStoreConnectionMap;
		this.dataStoreConnectionMap.put(DEFAULT_DATA_STORE,
				new ReceiveDataStoreConfig.DataStoreConnectionValues(dataStoreServerBase,
						dataStoreServerBasicAuthUsername, dataStoreServerBasicAuthPassword,
						dataStoreServerBearerToken));

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
		return dataStoreConnectionMap.get(DEFAULT_DATA_STORE).getBaseUrl();
	}

	public void testConnection()
	{
		try
		{
			for (String client : dataStoreConnectionMap.keySet())
			{
				final ReceiveDataStoreConfig.DataStoreConnectionValues value = dataStoreConnectionMap.get(client);
				logger.info(
						"Testing connection to Data Store FHIR server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
								+ " basicAuthUsername: {}, basicAuthPassword: {}, bearerToken: {}, serverBase: {}, proxy: values from 'DEV_DSF_PROXY'... config}",
						trustStorePath, certificatePath, privateKeyPath, privateKeyPassword != null ? "***" : "null",
						value.getBaseUrl(), value.getPassword() != null ? "***" : "null",
						value.getBearerToken() != null ? "***" : "null", value.getUsername());

				getDataStoreClient(client).testConnection();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while testing connection to Data Store FHIR server", e);
		}
	}

	public DataStoreClient getDataStoreClient()
	{
		return getDataStoreClient(DEFAULT_DATA_STORE);
	}

	public DataStoreClient getDataStoreClient(String client)
	{
		if (configured(client))
			return createDataStoreClient(client);
		else
			return new DataStoreClientStub(fhirContext, dataLogger);
	}

	private boolean configured(String client)
	{
		return dataStoreConnectionMap.get(client).getBaseUrl() != null
				&& !dataStoreConnectionMap.get(client).getBaseUrl().isBlank();
	}

	protected DataStoreClient createDataStoreClient()
	{
		return createDataStoreClient(DEFAULT_DATA_STORE);
	}

	protected DataStoreClient createDataStoreClient(String dataStore)
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

		final ReceiveDataStoreConfig.DataStoreConnectionValues dataStoreConfig = dataStoreConnectionMap.get(dataStore);

		return new DataStoreClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout, dataStoreConfig.getUsername(), dataStoreConfig.getPassword(),
				dataStoreConfig.getBearerToken(), dataStoreConfig.getBaseUrl(), proxyUrl, proxyUsername, proxyPassword,
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
