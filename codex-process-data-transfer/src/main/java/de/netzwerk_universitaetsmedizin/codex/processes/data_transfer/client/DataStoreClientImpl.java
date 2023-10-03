package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.KeyStore;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;

public class DataStoreClientImpl implements DataStoreClient
{
	private static final Logger logger = LoggerFactory.getLogger(DataStoreClientImpl.class);

	private final IRestfulClientFactory clientFactory;

	private final String dataServerBase;

	private final String dataServerBasicAuthUsername;
	private final String dataServerBasicAuthPassword;
	private final String dataServerBearerToken;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;
	private final Path searchBundleOverride;
	private final Class<DataStoreFhirClient> dataFhirClientClass;
	private final boolean useChainedParameterNotLogicalReference;

	private final DataLogger dataLogger;

	public DataStoreClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, int connectionRequestTimeout, String dataServerBasicAuthUsername,
			String dataServerBasicAuthPassword, String dataServerBearerToken, String dataServerBase, String proxyUrl,
			String proxyUsername, String proxyPassword, boolean hapiClientVerbose, FhirContext fhirContext,
			Path searchBundleOverride, Class<DataStoreFhirClient> dataFhirClientClass,
			boolean useChainedParameterNotLogicalReference, DataLogger dataLogger)
	{
		clientFactory = createClientFactory(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout);

		this.dataServerBase = dataServerBase;

		this.dataServerBasicAuthUsername = dataServerBasicAuthUsername;
		this.dataServerBasicAuthPassword = dataServerBasicAuthPassword;
		this.dataServerBearerToken = dataServerBearerToken;

		configureProxy(clientFactory, proxyUrl, proxyUsername, proxyPassword);

		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;
		this.searchBundleOverride = searchBundleOverride;
		this.dataFhirClientClass = dataFhirClientClass;
		this.useChainedParameterNotLogicalReference = useChainedParameterNotLogicalReference;

		this.dataLogger = dataLogger;
	}

	private void configureProxy(IRestfulClientFactory clientFactory, String proxyUrl, String proxyUsername,
			String proxyPassword)
	{
		if (proxyUrl != null && !proxyUrl.isBlank())
		{
			try
			{
				URL url = new URL(proxyUrl);
				clientFactory.setProxy(url.getHost(), url.getPort());
				clientFactory.setProxyCredentials(proxyUsername, proxyPassword);

				logger.info("Using proxy for data FHIR server connection with {host: {}, port: {}, username: {}}",
						url.getHost(), url.getPort(), proxyUsername);
			}
			catch (MalformedURLException e)
			{
				logger.error("Could not configure proxy", e);
			}
		}
	}

	protected ApacheRestfulClientFactoryWithTlsConfig createClientFactory(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, int connectTimeout, int socketTimeout, int connectionRequestTimeout)
	{
		FhirContext fhirContext = FhirContext.forR4();
		ApacheRestfulClientFactoryWithTlsConfig hapiClientFactory = new ApacheRestfulClientFactoryWithTlsConfig(
				fhirContext, trustStore, keyStore, keyStorePassword);
		hapiClientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);

		hapiClientFactory.setConnectTimeout(connectTimeout);
		hapiClientFactory.setSocketTimeout(socketTimeout);
		hapiClientFactory.setConnectionRequestTimeout(connectionRequestTimeout);

		fhirContext.setRestfulClientFactory(hapiClientFactory);
		return hapiClientFactory;
	}

	private void configuredWithBasicAuth(IGenericClient client)
	{
		if (dataServerBasicAuthUsername != null && dataServerBasicAuthPassword != null)
			client.registerInterceptor(
					new BasicAuthInterceptor(dataServerBasicAuthUsername, dataServerBasicAuthPassword));
	}

	private void configureBearerTokenAuthInterceptor(IGenericClient client)
	{
		if (dataServerBearerToken != null)
			client.registerInterceptor(new BearerTokenAuthInterceptor(dataServerBearerToken));
	}

	private void configureLoggingInterceptor(IGenericClient client)
	{
		if (hapiClientVerbose)
		{
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor(true);
			loggingInterceptor.setLogger(new HapiClientLogger(logger));
			client.registerInterceptor(loggingInterceptor);
		}
	}

	@Override
	public String getServerBase()
	{
		return dataServerBase;
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
	}

	@Override
	public void testConnection()
	{
		CapabilityStatement statement = getGenericFhirClient().capabilities().ofType(CapabilityStatement.class)
				.execute();

		logger.info("Connection test OK {} - {}", statement.getSoftware().getName(),
				statement.getSoftware().getVersion());
	}

	@Override
	public DataStoreFhirClient getFhirClient()
	{
		try
		{
			Constructor<DataStoreFhirClient> constructor = dataFhirClientClass.getConstructor(DataStoreClient.class,
					DataLogger.class);

			return constructor.newInstance(this, dataLogger);
		}
		catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e)
		{
			logger.warn("Error while creating data FHIR client: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public Path getSearchBundleOverride()
	{
		return searchBundleOverride;
	}

	@Override
	public IGenericClient getGenericFhirClient()
	{
		IGenericClient client = clientFactory.newGenericClient(dataServerBase);

		configuredWithBasicAuth(client);
		configureBearerTokenAuthInterceptor(client);
		configureLoggingInterceptor(client);

		return client;
	}

	@Override
	public boolean shouldUseChainedParameterNotLogicalReference()
	{
		return useChainedParameterNotLogicalReference;
	}
}
