package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.security.KeyStore;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.highmed.dsf.fhir.adapter.CapabilityStatementJsonFhirAdapter;
import org.highmed.dsf.fhir.adapter.CapabilityStatementXmlFhirAdapter;
import org.highmed.dsf.fhir.adapter.OperationOutcomeJsonFhirAdapter;
import org.highmed.dsf.fhir.adapter.OperationOutcomeXmlFhirAdapter;
import org.highmed.dsf.fhir.adapter.ParametersJsonFhirAdapter;
import org.highmed.dsf.fhir.adapter.ParametersXmlFhirAdapter;
import org.highmed.dsf.fhir.adapter.ValueSetJsonFhirAdapter;
import org.highmed.dsf.fhir.adapter.ValueSetXmlFhirAdapter;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;

public class ValueSetExpansionClientJersey implements ValueSetExpansionClient
{
	private static final Logger logger = LoggerFactory.getLogger(ValueSetExpansionClientJersey.class);
	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(ValueSetExpansionClientJersey.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private final Client client;
	private final String baseUrl;

	public ValueSetExpansionClientJersey(String baseUrl, ObjectMapper objectMapper, FhirContext fhirContext)
	{
		this(baseUrl, null, null, null, null, null, null, null, null, 0, 0, false, objectMapper, fhirContext);
	}

	public ValueSetExpansionClientJersey(String baseUrl, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, String basicAuthUsername, char[] basicAuthPassword, String proxySchemeHostPort,
			String proxyUsername, char[] proxyPassword, int connectTimeout, int readTimeout, boolean logRequests,
			ObjectMapper objectMapper, FhirContext fhirContext)
	{
		SSLContext sslContext = null;
		if (trustStore != null && keyStore == null && keyStorePassword == null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).createSSLContext();
		else if (trustStore == null && keyStore != null && keyStorePassword != null)
			sslContext = SslConfigurator.newInstance().keyStore(keyStore).keyStorePassword(keyStorePassword)
					.createSSLContext();
		else if (trustStore != null && keyStore != null && keyStorePassword != null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).keyStore(keyStore)
					.keyStorePassword(keyStorePassword).createSSLContext();

		ClientBuilder builder = ClientBuilder.newBuilder();

		if (sslContext != null)
			builder = builder.sslContext(sslContext);

		if (basicAuthUsername != null && basicAuthPassword != null)
		{
			HttpAuthenticationFeature basicAuthFeature = HttpAuthenticationFeature.basic(basicAuthUsername,
					String.valueOf(basicAuthPassword));
			builder = builder.register(basicAuthFeature);
		}

		ClientConfig config = new ClientConfig();
		config.connectorProvider(new ApacheConnectorProvider());
		config.property(ClientProperties.PROXY_URI, proxySchemeHostPort);
		config.property(ClientProperties.PROXY_USERNAME, proxyUsername);
		config.property(ClientProperties.PROXY_PASSWORD, proxyPassword == null ? null : String.valueOf(proxyPassword));
		builder = builder.withConfig(config);

		builder = builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS).connectTimeout(connectTimeout,
				TimeUnit.MILLISECONDS);

		if (objectMapper != null)
		{
			JacksonJaxbJsonProvider p = new JacksonJaxbJsonProvider(JacksonJsonProvider.BASIC_ANNOTATIONS);
			p.setMapper(objectMapper);
			builder = builder.register(p);
		}

		builder = builder.register(new CapabilityStatementJsonFhirAdapter(fhirContext))
				.register(new CapabilityStatementXmlFhirAdapter(fhirContext))
				.register(new OperationOutcomeJsonFhirAdapter(fhirContext))
				.register(new OperationOutcomeXmlFhirAdapter(fhirContext))
				.register(new ParametersJsonFhirAdapter(fhirContext))
				.register(new ParametersXmlFhirAdapter(fhirContext)).register(new ValueSetJsonFhirAdapter(fhirContext))
				.register(new ValueSetXmlFhirAdapter(fhirContext));

		if (logRequests)
		{
			builder = builder.register(new LoggingFeature(requestDebugLogger, Level.INFO, Verbosity.PAYLOAD_ANY,
					LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
		}

		client = builder.build();

		this.baseUrl = baseUrl;
	}

	private WebTarget getResource()
	{
		return client.target(baseUrl);
	}

	@Override
	public ValueSet expand(ValueSet valueSet) throws WebApplicationException
	{
		Objects.requireNonNull(valueSet, "valueSet");

		if (valueSet.hasExpansion())
		{
			logger.debug("ValueSet {}|{} already expanded", valueSet.getUrl(), valueSet.getVersion());
			return valueSet;
		}

		Parameters parameters = new Parameters();
		parameters.addParameter().setName("valueSet").setResource(valueSet);

		return getResource().path("ValueSet").path("$expand").request(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW), ValueSet.class);
	}

	@Override
	public CapabilityStatement getMetadata() throws WebApplicationException
	{
		return getResource().path("metadata").request(Constants.CT_FHIR_JSON_NEW).get(CapabilityStatement.class);
	}
}
