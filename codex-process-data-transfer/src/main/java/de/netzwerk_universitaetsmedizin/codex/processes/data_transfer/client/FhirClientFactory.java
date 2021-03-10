package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.Objects;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;

public class FhirClientFactory
{
	private final FhirContext fhirContext;
	private final String serverBase;
	private final String basicAuthUsername;
	private final String basicAuthPassword;
	private final String bearerToken;

	/**
	 * @param fhirContext
	 *            may be <code>null</code>, will use new {@link FhirContext#forR4()} if <code>null</code>
	 * @param serverBase
	 *            not <code>null</code>
	 * @param basicAuthUsername
	 *            may be <code>null</code>
	 * @param basicAuthPassword
	 *            may be <code>null</code>
	 * @param bearerToken
	 *            may be <code>null</code>
	 */
	public FhirClientFactory(FhirContext fhirContext, String serverBase, String basicAuthUsername,
			String basicAuthPassword, String bearerToken)
	{
		if (fhirContext != null)
			this.fhirContext = fhirContext;
		else
			this.fhirContext = FhirContext.forR4();

		this.serverBase = Objects.requireNonNull(serverBase, "serverBase");
		this.basicAuthUsername = basicAuthUsername;
		this.basicAuthPassword = basicAuthPassword;
		this.bearerToken = bearerToken;
	}

	public IGenericClient getFhirStoreClient()
	{
		IGenericClient client = fhirContext.newRestfulGenericClient(serverBase);
		configureBasicAuthInterceptor(client);
		configureBearerTokenAuthInterceptor(client);

		return client;
	}

	private void configureBasicAuthInterceptor(IGenericClient client)
	{
		if (basicAuthUsername != null && basicAuthPassword != null)
			client.registerInterceptor(new BasicAuthInterceptor(basicAuthUsername, basicAuthPassword));
	}

	private void configureBearerTokenAuthInterceptor(IGenericClient client)
	{
		if (bearerToken != null)
			client.registerInterceptor(new BearerTokenAuthInterceptor(bearerToken));
	}
}
