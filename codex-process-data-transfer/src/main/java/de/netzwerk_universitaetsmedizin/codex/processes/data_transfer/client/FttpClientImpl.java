package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PSEUDONYM_PATTERN_STRING;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

public class FttpClientImpl implements FttpClient
{
	private static final Logger logger = LoggerFactory.getLogger(FttpClientImpl.class);

	private static final Pattern DIC_PSEUDONYM_PATTERN = Pattern.compile(PSEUDONYM_PATTERN_STRING);

	private final IRestfulClientFactory clientFactory;

	private final String fttpServerBase;

	private final String fttpBasicAuthUsername;
	private final String fttpBasicAuthPassword;

	private final String fttpStudy;
	private final String fttpTarget;
	private final String fttpApiKey;

	private final boolean hapiClientVerbose;

	public FttpClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, int connectionRequestTimeout, String fttpBasicAuthUsername, String fttpBasicAuthPassword,
			String fttpServerBase, String fttpApiKey, String fttpStudy, String fttpTarget, String proxySchemeHostPort,
			String proxyUsername, String proxyPassword, boolean hapiClientVerbose)
	{
		clientFactory = createClientFactory(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout);

		this.fttpServerBase = fttpServerBase;

		this.fttpBasicAuthUsername = fttpBasicAuthUsername;
		this.fttpBasicAuthPassword = fttpBasicAuthPassword;

		this.fttpApiKey = fttpApiKey;
		this.fttpStudy = fttpStudy;
		this.fttpTarget = fttpTarget;

		configureProxy(clientFactory, proxySchemeHostPort, proxyUsername, proxyPassword);

		this.hapiClientVerbose = hapiClientVerbose;
	}

	protected ApacheRestfulClientFactoryWithTlsConfig createClientFactory(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, int connectTimeout, int socketTimeout, int connectionRequestTimeout)
	{
		Objects.requireNonNull(trustStore, "trustStore");
		Objects.requireNonNull(keyStore, "keyStore");
		Objects.requireNonNull(keyStorePassword, "keyStorePassword");

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

	private void configureProxy(IRestfulClientFactory clientFactory, String proxySchemeHostPort, String proxyUsername,
			String proxyPassword)
	{
		if (proxySchemeHostPort != null && !proxySchemeHostPort.isBlank())
		{
			try
			{
				URL url = new URL(proxySchemeHostPort);
				clientFactory.setProxy(url.getHost(), url.getPort());
				clientFactory.setProxyCredentials(proxyUsername, proxyPassword);

				logger.info("Using proxy for fTTP connection with {host: {}, port: {}, username: {}}", url.getHost(),
						url.getPort(), proxyUsername);
			}
			catch (MalformedURLException e)
			{
				logger.error("Could not configure proxy", e);
			}
		}
	}

	@Override
	public Optional<String> getCrrPseudonym(String dicSourceAndPseudonym)
	{
		Objects.requireNonNull(dicSourceAndPseudonym, "dicSourceAndPseudonym");

		logger.info("Requesting CRR pseudonym from {} ...", dicSourceAndPseudonym);

		try
		{
			IGenericClient client = createGenericClient();

			Parameters parameters = client.operation().onServer().named("requestPsnWorkflow")
					.withParameters(createParametersForPsnWorkflow(dicSourceAndPseudonym))
					.accept(Constants.CT_FHIR_XML_NEW).encoded(EncodingEnum.XML).execute();

			return getPseudonym(parameters);
		}
		catch (Exception e)
		{
			logger.error("Error while retrieving CRR pseudonym", e);
			return Optional.empty();
		}
	}

	protected Parameters createParametersForPsnWorkflow(String dicSourceAndPseudonym)
	{
		Matcher matcher = DIC_PSEUDONYM_PATTERN.matcher(dicSourceAndPseudonym);
		if (!matcher.matches())
			throw new IllegalArgumentException("DIC pseudonym not matching " + PSEUDONYM_PATTERN_STRING);

		String source = matcher.group(1);
		String original = matcher.group(2);

		Parameters p = new Parameters();
		p.addParameter("study", fttpStudy);
		p.addParameter("original", original);
		p.addParameter("source", source);
		p.addParameter("target", fttpTarget);
		p.addParameter("apikey", fttpApiKey);

		return p;
	}

	@Override
	public Optional<String> getDicPseudonym(String bloomFilter)
	{
		Objects.requireNonNull(bloomFilter, "bloomFilter");

		logger.info("Requesting DIC pseudonym for bloomfilter {} ", bloomFilter);

		try
		{
			IGenericClient client = createGenericClient();

			Parameters parameters = client.operation().onServer().named("$requestPsnFromBfWorkflow")
					.withParameters(createParametersForBfWorkflow(bloomFilter)).accept(Constants.CT_FHIR_XML_NEW)
					.encoded(EncodingEnum.XML).execute();

			return getPseudonym(parameters).map(p -> fttpTarget + "/" + p);
		}
		catch (Exception e)
		{
			logger.error("Error while retrieving DIC pseudonym", e);
			return Optional.empty();
		}
	}

	protected Parameters createParametersForBfWorkflow(String bloomFilter)
	{
		Parameters p = new Parameters();
		p.addParameter("study", fttpStudy);
		p.addParameter("bloomfilter", new Base64BinaryType(bloomFilter));
		p.addParameter("target", fttpTarget);
		p.addParameter("apikey", fttpApiKey);

		return p;
	}

	protected Optional<String> getPseudonym(Parameters params)
	{
		if (params == null)
			return Optional.empty();

		for (ParametersParameterComponent comp : params.getParameterFirstRep().getPart())
		{
			if ("pseudonym".equals(comp.getName()))
			{
				if (!comp.hasValue())
					logger.warn("fTTP return parameter object has no value for sub-parameter 'pseudonym'");

				return Optional.ofNullable(comp.getValue()).filter(v -> v instanceof Identifier)
						.map(v -> (Identifier) v)
						.filter(i -> "https://ths-greifswald.de/dispatcher".equals(i.getSystem()))
						.map(Identifier::getValue);
			}
		}

		return Optional.empty();
	}

	@Override
	public void testConnection()
	{
		IGenericClient client = createGenericClient();

		CapabilityStatement statement = client.capabilities().ofType(CapabilityStatement.class).execute();

		logger.info("Connection test OK {} - {}", statement.getSoftware().getName(),
				statement.getSoftware().getVersion());
	}

	private IGenericClient createGenericClient()
	{
		IGenericClient client = clientFactory.newGenericClient(fttpServerBase);

		configuredWithBasicAuth(client);
		configureLoggingInterceptor(client);

		return client;
	}

	private void configuredWithBasicAuth(IGenericClient client)
	{
		if (fttpBasicAuthUsername != null && fttpBasicAuthPassword != null)
			client.registerInterceptor(new BasicAuthInterceptor(fttpBasicAuthUsername, fttpBasicAuthPassword));
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
}
