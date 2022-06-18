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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.GeccoFhirClient;

public class GeccoClientImpl implements GeccoClient
{
	private static final Logger logger = LoggerFactory.getLogger(GeccoClientImpl.class);

	private final IRestfulClientFactory clientFactory;

	private final String geccoServerBase;

	private final String geccoServerBasicAuthUsername;
	private final String geccoServerBasicAuthPassword;
	private final String geccoServerBearerToken;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;
	private final Path searchBundleOverride;
	private final String localIdentifierValue;
	private final Class<GeccoFhirClient> geccoFhirClientClass;
	private final boolean useChainedParameterNotLogicalReference;

	public GeccoClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, int connectionRequestTimeout, String geccoServerBasicAuthUsername,
			String geccoServerBasicAuthPassword, String geccoServerBearerToken, String geccoServerBase, String proxyUrl,
			String proxyUsername, String proxyPassword, boolean hapiClientVerbose, FhirContext fhirContext,
			Path searchBundleOverride, String localIdentifierValue, Class<GeccoFhirClient> geccoFhirClientClass,
			boolean useChainedParameterNotLogicalReference)
	{
		clientFactory = createClientFactory(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout);

		this.geccoServerBase = geccoServerBase;

		this.geccoServerBasicAuthUsername = geccoServerBasicAuthUsername;
		this.geccoServerBasicAuthPassword = geccoServerBasicAuthPassword;
		this.geccoServerBearerToken = geccoServerBearerToken;

		configureProxy(clientFactory, proxyUrl, proxyUsername, proxyPassword);

		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;
		this.searchBundleOverride = searchBundleOverride;
		this.localIdentifierValue = localIdentifierValue;
		this.geccoFhirClientClass = geccoFhirClientClass;
		this.useChainedParameterNotLogicalReference = useChainedParameterNotLogicalReference;
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

				logger.info("Using proxy for GECCO FHIR server connection with {host: {}, port: {}, username: {}}",
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
		if (geccoServerBasicAuthUsername != null && geccoServerBasicAuthPassword != null)
			client.registerInterceptor(
					new BasicAuthInterceptor(geccoServerBasicAuthUsername, geccoServerBasicAuthPassword));
	}

	private void configureBearerTokenAuthInterceptor(IGenericClient client)
	{
		if (geccoServerBearerToken != null)
			client.registerInterceptor(new BearerTokenAuthInterceptor(geccoServerBearerToken));
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
		return geccoServerBase;
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
	public GeccoFhirClient getFhirClient()
	{
		try
		{
			Constructor<GeccoFhirClient> constructor = geccoFhirClientClass.getConstructor(GeccoClient.class);

			return constructor.newInstance(this);
		}
		catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e)
		{
			logger.warn("Error while creating GECCO FHIR client: {}", e.getMessage());
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
		IGenericClient client = clientFactory.newGenericClient(geccoServerBase);

		configuredWithBasicAuth(client);
		configureBearerTokenAuthInterceptor(client);
		configureLoggingInterceptor(client);

		return client;
	}

	@Override
	public String getLocalIdentifierValue()
	{
		return localIdentifierValue;
	}

	@Override
	public boolean shouldUseChainedParameterNotLogicalReference()
	{
		return useChainedParameterNotLogicalReference;
	}
}
