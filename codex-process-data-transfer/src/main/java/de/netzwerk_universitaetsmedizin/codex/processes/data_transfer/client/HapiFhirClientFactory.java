package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RequestFormatParamStyleEnum;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.IDelete;
import ca.uhn.fhir.rest.gclient.IFetchConformanceUntyped;
import ca.uhn.fhir.rest.gclient.IGetPage;
import ca.uhn.fhir.rest.gclient.IHistory;
import ca.uhn.fhir.rest.gclient.IMeta;
import ca.uhn.fhir.rest.gclient.IOperation;
import ca.uhn.fhir.rest.gclient.IPatch;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.ITransaction;
import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IValidate;

public class HapiFhirClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(HapiFhirClientFactory.class);

	private final FhirContext fhirContext;
	private final String serverBase;
	private final String basicAuthUsername;
	private final String basicAuthPassword;
	private final String bearerToken;

	private final boolean hapiClientVerbose;
	private final boolean useChainedParameterNotLogicalReference;

	private final ApacheRestfulClientFactory clientFactory;

	/**
	 * @param fhirContext
	 *            may be <code>null</code>, will use new {@link FhirContext#forR4()} if <code>null</code>
	 * @param serverBase
	 *            may be <code>null</code>
	 * @param basicAuthUsername
	 *            may be <code>null</code>
	 * @param basicAuthPassword
	 *            may be <code>null</code>
	 * @param bearerToken
	 *            may be <code>null</code>
	 * @param connectTimeout
	 *            >= -1, -1: system default, 0: infinity, >0: timeout in ms
	 * @param socketTimeout
	 *            >= -1, -1: system default, 0: infinity, >0: timeout in ms
	 * @param connectionRequestTimeout
	 *            >= -1, -1: system default, 0: infinity, >0: timeout in ms
	 * @param hapiClientVerbose
	 *            <code>true</code> for verbose logging
	 * @param useChainedParameterNotLogicalReference
	 *            <code>true</code> to enable modifying the search parameters during data storage
	 */
	public HapiFhirClientFactory(FhirContext fhirContext, String serverBase, String basicAuthUsername,
			String basicAuthPassword, String bearerToken, int connectTimeout, int socketTimeout,
			int connectionRequestTimeout, boolean hapiClientVerbose, boolean useChainedParameterNotLogicalReference)
	{
		if (fhirContext != null)
			this.fhirContext = fhirContext;
		else
			this.fhirContext = FhirContext.forR4();

		this.serverBase = serverBase;
		this.basicAuthUsername = basicAuthUsername;
		this.basicAuthPassword = basicAuthPassword;
		this.bearerToken = bearerToken;

		this.hapiClientVerbose = hapiClientVerbose;
		this.useChainedParameterNotLogicalReference = useChainedParameterNotLogicalReference;

		if (isConfigured())
		{
			clientFactory = new ApacheRestfulClientFactory(this.fhirContext);
			clientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
			clientFactory.setConnectTimeout(connectTimeout);
			clientFactory.setSocketTimeout(socketTimeout);
			clientFactory.setConnectionRequestTimeout(connectionRequestTimeout);
		}
		else
			clientFactory = null;
	}

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event)
	{
		if (isConfigured())
		{
			try
			{
				logger.info(
						"Testing connection to GECCO FHIR server with {basicAuthUsername: {}, basicAuthPassword: {}, bearerToken: {}, serverBase: {}}",
						basicAuthUsername, basicAuthPassword != null ? "***" : "null",
						bearerToken != null ? "***" : "null", serverBase);

				CapabilityStatement statement = getFhirStoreClient().capabilities().ofType(CapabilityStatement.class)
						.execute();

				logger.info("Connection test OK {} - {}", statement.getSoftware().getName(),
						statement.getSoftware().getVersion());
			}
			catch (Exception e)
			{
				logger.error("Error while testing connection to GECCO FHIR server", e);
			}
		}
		else
			logger.warn("GECCO FHIR server Client stub implementation, no connection test performed");
	}

	protected boolean isConfigured()
	{
		return serverBase != null && !serverBase.isEmpty();
	}

	public IGenericClient getFhirStoreClient()
	{
		if (isConfigured())
		{
			IGenericClient client = clientFactory.newGenericClient(serverBase);
			configureBasicAuthInterceptor(client);
			configureBearerTokenAuthInterceptor(client);
			configureLoggingInterceptor(client);
			return client;
		}
		else
		{
			return createFhirStoreClientStub();
		}
	}

	private IGenericClient createFhirStoreClientStub()
	{
		return new IGenericClient()
		{
			@Override
			public void setSummary(SummaryEnum theSummary)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public void setPrettyPrint(Boolean thePrettyPrint)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public void setInterceptorService(IInterceptorService theInterceptorService)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public void setFormatParamStyle(RequestFormatParamStyleEnum theRequestFormatParamStyle)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public void setEncoding(EncodingEnum theEncoding)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public String getServerBase()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IInterceptorService getInterceptorService()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IHttpClient getHttpClient()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public FhirContext getFhirContext()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public EncodingEnum getEncoding()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public <T extends IBaseResource> T fetchResourceFromUrl(Class<T> theResourceType, String theUrl)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public <T extends IBaseResource> T vread(Class<T> theType, String theId, String theVersionId)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public <T extends IBaseResource> T vread(Class<T> theType, IdDt theId)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public MethodOutcome validate(IBaseResource theResource)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IValidate validate()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public MethodOutcome update(String theId, IBaseResource theResource)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public MethodOutcome update(IdDt theId, IBaseResource theResource)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IUpdate update()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public void unregisterInterceptor(Object theInterceptor)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public ITransaction transaction()
			{
				return new ITransaction()
				{
					@Override
					public ITransactionTyped<List<IBaseResource>> withResources(
							List<? extends IBaseResource> theResources)
					{
						throw new UnsupportedOperationException("Not implemented");
					}

					@Override
					public ITransactionTyped<String> withBundle(String theBundle)
					{
						throw new UnsupportedOperationException("Not implemented");
					}

					@Override
					public <T extends IBaseBundle> ITransactionTyped<T> withBundle(T theBundleResource)
					{
						return new ITransactionTyped<T>()
						{
							@Override
							public ITransactionTyped<T> andLogRequestAndResponse(boolean theLogRequestAndResponse)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> cacheControl(CacheControlDirective theCacheControlDirective)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> elementsSubset(String... theElements)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> encoded(EncodingEnum theEncoding)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> encodedJson()
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> encodedXml()
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> withAdditionalHeader(String theHeaderName,
									String theHeaderValue)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public T execute()
							{
								if (logger.isInfoEnabled())
									logger.info("Bundle from GTH: {}",
											fhirContext.newJsonParser().encodeResourceToString(theBundleResource));

								return null;
							}

							@Override
							public ITransactionTyped<T> preferResponseType(Class<? extends IBaseResource> theType)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> preferResponseTypes(
									List<Class<? extends IBaseResource>> theTypes)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> prettyPrint()
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> summaryMode(SummaryEnum theSummary)
							{
								throw new UnsupportedOperationException("Not implemented");
							}

							@Override
							public ITransactionTyped<T> accept(String theHeaderValue)
							{
								throw new UnsupportedOperationException("Not implemented");
							}
						};
					}
				};
			}

			@Override
			public void setLogRequestAndResponse(boolean theLogRequestAndResponse)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public <T extends IBaseBundle> IUntypedQuery<T> search()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public void registerInterceptor(Object theInterceptor)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public <T extends IBaseResource> T read(Class<T> theType, UriDt theUrl)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public <T extends IBaseResource> T read(Class<T> theType, String theId)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IBaseResource read(UriDt theUrl)
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IRead read()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IPatch patch()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IOperation operation()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IMeta meta()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IGetPage loadPage()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IHistory history()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public void forceConformanceCheck() throws FhirClientConnectionException
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IFetchConformanceUntyped fetchConformance()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IDelete delete()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public ICreate create()
			{
				throw new UnsupportedOperationException("Not implemented");
			}

			@Override
			public IFetchConformanceUntyped capabilities()
			{
				throw new UnsupportedOperationException("Not implemented");
			}
		};
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

	private void configureLoggingInterceptor(IGenericClient client)
	{
		if (hapiClientVerbose)
		{
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor(true);
			loggingInterceptor.setLogger(new HapiClientLogger(logger));
			client.registerInterceptor(loggingInterceptor);
		}
	}

	public boolean shouldUseChainedParameterNotLogicalReference()
	{
		return useChainedParameterNotLogicalReference;
	}
}
