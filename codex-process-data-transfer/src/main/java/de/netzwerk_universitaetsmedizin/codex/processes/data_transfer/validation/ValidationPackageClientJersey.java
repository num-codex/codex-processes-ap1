package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;

public class ValidationPackageClientJersey implements ValidationPackageClient
{
	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(ValidationPackageClientJersey.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private final Client client;
	private final String baseUrl;

	public ValidationPackageClientJersey(String baseUrl)
	{
		this(baseUrl, null, null, null, null, null, null, null, null, 0, 0, false);
	}

	public ValidationPackageClientJersey(String baseUrl, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, String basicAuthUsername, char[] basicAuthPassword, String proxySchemeHostPort,
			String proxyUsername, char[] proxyPassword, int connectTimeout, int readTimeout, boolean logRequests)
	{
		SSLContext sslContext = null;
		if (trustStore != null && keyStore == null && keyStorePassword == null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).createSSLContext();
		if (trustStore == null && keyStore != null && keyStorePassword != null)
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
	public ValidationPackage download(ValidationPackageIdentifier identifier)
			throws IOException, WebApplicationException
	{
		Objects.requireNonNull(identifier, "identifier");

		try (InputStream in = getResource().path(identifier.getName()).path(identifier.getVersion())
				.request("application/tar+gzip").get(InputStream.class))
		{
			return ValidationPackage.from(identifier.getName(), identifier.getVersion(), in);
		}
	}
}
