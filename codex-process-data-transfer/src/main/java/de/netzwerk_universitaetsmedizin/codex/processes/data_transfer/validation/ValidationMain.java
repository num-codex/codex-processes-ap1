package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.ValidationResult;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ValidationConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ValidationConfig.TerminologyServerConnectionTestStatus;
import dev.dsf.bpe.v1.config.ProxyConfig;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;

public class ValidationMain implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationMain.class);

	private static final class FileNameAndResource
	{
		final String filename;
		final Resource resource;

		FileNameAndResource(String filename, Resource resource)
		{
			this.filename = filename;
			this.resource = resource;
		}

		String getFilename()
		{
			return filename;
		}

		Resource getResource()
		{
			return resource;
		}
	}

	@Configuration
	@PropertySource(ignoreResourceNotFound = true, value = "file:application.properties")
	public static class TestConfig
	{
		@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.output:JSON}")
		private Output output;

		@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.output.pretty:true}")
		private boolean outputPretty;

		@ProcessDocumentation(description = "Forward (http/https) proxy url, use *DEV_DSF_BPE_PROXY_NOPROXY* to list domains that do not require a forward proxy", example = "http://proxy.foo:8080")
		@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.proxy.url:#{null}}")
		private String proxyUrl;

		@ProcessDocumentation(description = "Forward proxy username", recommendation = "Configure username if proxy requires authentication")
		@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.proxy.username:#{null}}")
		private String proxyUsername;

		@ProcessDocumentation(description = "Forward Proxy password", recommendation = "Configure password if proxy requires authentication, use docker secret file to configure using *${env_variable}_FILE*")
		@Value("${de.netzwerk.universitaetsmedizin.rdp.validation.proxy.password:#{null}}")
		private char[] proxyPassword;

		@ProcessDocumentation(description = "Forward proxy no-proxy list, entries will match exactly or agianst (one level) sub-domains, if no port is specified - all ports are matched; comma or space separated list, YAML block scalars supported", example = "foo.bar, test.com:8080")
		@Value("#{'${de.netzwerk.universitaetsmedizin.rdp.validation.proxy.noProxy:}'.trim().split('(,[ ]?)|(\\n)')}")
		private List<String> proxyNoProxy;

		@Autowired
		private ValidationPackageManager packageManager;

		@Autowired
		private List<ValidationPackageIdentifier> validationPackageIdentifiers;

		@Autowired
		private ValueSetExpansionClient valueSetExpansionClient;

		@Autowired
		private ConfigurableEnvironment environment;

		@Bean
		public ObjectMapper objectMapper()
		{
			return JsonMapper.builder().serializationInclusion(Include.NON_NULL)
					.serializationInclusion(Include.NON_EMPTY).disable(MapperFeature.AUTO_DETECT_CREATORS)
					.disable(MapperFeature.AUTO_DETECT_FIELDS).disable(MapperFeature.AUTO_DETECT_SETTERS).build();
		}

		@Bean
		public FhirContext fhirContext()
		{
			FhirContext context = FhirContext.forR4();
			HapiLocalizer localizer = new HapiLocalizer()
			{
				@Override
				public Locale getLocale()
				{
					return Locale.ROOT;
				}
			};
			context.setLocalizer(localizer);
			return context;
		}

		@Bean
		public ProxyConfig proxyConfig()
		{
			return new ProxyConfig()
			{
				@Override
				public boolean isNoProxyUrl(String targetUrl)
				{
					if (proxyNoProxy.contains("*"))
						return true;

					if (targetUrl == null || targetUrl.isBlank())
						return false;

					try
					{
						URI u = new URI(targetUrl);

						String host = u.getHost();
						if (host == null)
						{
							logger.debug("Given targetUrl '{}' is malformed, no host value", targetUrl);
							return false;
						}

						String subHost = Stream.of(u.getHost().split("\\.")).skip(1).collect(Collectors.joining("."));
						int port = u.getPort() == -1 ? getDefaultPort(u.getScheme()) : u.getPort();

						return proxyNoProxy.stream().anyMatch(s -> s.equals(host) || s.equals(host + ":" + port)
								|| s.equals(subHost) || s.equals(subHost + ":" + port));
					}
					catch (URISyntaxException e)
					{
						logger.debug("Given targetUrl '{}' is malformed: {}", targetUrl, e.getMessage());
						return false;
					}
				}

				private int getDefaultPort(String scheme)
				{
					return switch (scheme)
					{
						case "http", "ws" -> 80;
						case "https", "wss" -> 443;
						default -> throw new IllegalArgumentException("Schema " + scheme + " not supported");
					};
				}

				@Override
				public boolean isEnabled()
				{
					return getUrl() != null;
				}

				@Override
				public String getUsername()
				{
					return proxyUsername;
				}

				@Override
				public String getUrl()
				{
					return proxyUrl;
				}

				@Override
				public char[] getPassword()
				{
					return proxyPassword;
				}

				@Override
				public List<String> getNoProxyUrls()
				{
					return proxyNoProxy;
				}
			};
		}

		@Bean
		public ValidationMain validatorMain()
		{
			return new ValidationMain(environment, fhirContext(), packageManager, validationPackageIdentifiers, output,
					outputPretty, valueSetExpansionClient);
		}
	}

	public static enum Output
	{
		JSON, XML

	}

	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			logger.warn("No files to validated specified");
			System.exit(1);
		}

		try (AnnotationConfigApplicationContext springContext = new AnnotationConfigApplicationContext(TestConfig.class,
				ValidationConfig.class))
		{
			ValidationConfig config = springContext.getBean(ValidationConfig.class);
			TerminologyServerConnectionTestStatus status = config.testConnectionToTerminologyServer();

			if (TerminologyServerConnectionTestStatus.OK.equals(status))
			{
				ValidationMain main = springContext.getBean(ValidationMain.class);
				main.validate(args);
			}
		}
		catch (Exception e)
		{
			logger.error("", e);
			System.exit(1);
		}
	}

	private final ConfigurableEnvironment environment;
	private final FhirContext fhirContext;
	private final ValidationPackageManager packageManager;
	private final List<ValidationPackageIdentifier> validationPackageIdentifiers = new ArrayList<>();
	private final Output output;
	private final boolean outputPretty;
	private final ValueSetExpansionClient valueSetExpansionClient;

	public ValidationMain(ConfigurableEnvironment environment, FhirContext fhirContext,
			ValidationPackageManager packageManager, List<ValidationPackageIdentifier> validationPackageIdentifiers,
			Output output, boolean outputPretty, ValueSetExpansionClient valueSetExpansionClient)
	{
		this.environment = environment;
		this.fhirContext = fhirContext;
		this.packageManager = packageManager;
		if (validationPackageIdentifiers != null)
			this.validationPackageIdentifiers.addAll(validationPackageIdentifiers);
		this.output = output;
		this.outputPretty = outputPretty;
		this.valueSetExpansionClient = valueSetExpansionClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(packageManager, "packageManager");
		Objects.requireNonNull(output, "output");
		Objects.requireNonNull(valueSetExpansionClient, "valueSetExpansionClient");
	}

	public void validate(String[] files)
	{
		logger.info("Using validation packages {}", validationPackageIdentifiers);
		getAllNumProperties().forEach(c -> logger.debug("Config: {}", c));

		BundleValidator validator = packageManager.createBundleValidator(validationPackageIdentifiers);

		Arrays.stream(files).map(this::read).filter(r -> r != null).forEach(r ->
		{
			logger.info("Validating {} from {}", r.getResource().getResourceType().name(), r.getFilename());

			if (r.getResource() instanceof Bundle)
			{
				Bundle validationResult = validator.validate((Bundle) r.getResource());
				System.out.println(getOutputParser().encodeResourceToString(validationResult));
			}
			else
			{
				ValidationResult validationResult = validator.validate(r.getResource());
				System.out.println(getOutputParser().encodeResourceToString(validationResult.toOperationOutcome()));
			}
		});
	}

	private Stream<String> getAllNumProperties()
	{
		return environment.getPropertySources().stream().filter(p -> p instanceof EnumerablePropertySource<?>)
				.map(p -> (EnumerablePropertySource<?>) p)
				.flatMap(p -> Arrays.stream(p.getPropertyNames())
						.filter(n -> n.startsWith("de.netzwerk.universitaetsmedizin"))
						.map(k -> new String[] { k, Objects.toString(p.getProperty(k)) }))
				.map(e -> e[0].contains("password") ? new String[] { e[0], "***" } : e).map(e -> e[0] + ": " + e[1]);
	}

	private IParser getOutputParser()
	{
		switch (output)
		{
			case JSON:
				return fhirContext.newJsonParser().setPrettyPrint(outputPretty);
			case XML:
				return fhirContext.newXmlParser().setPrettyPrint(outputPretty);
			default:
				throw new IllegalStateException("Output of type " + output + " not supported");
		}
	}

	private FileNameAndResource read(String file)
	{
		if (file.endsWith(".json"))
			return tryJson(file);
		else if (file.endsWith(".xml"))
			return tryXml(file);
		else
		{
			logger.warn("File {} not supported, filename needs to end with .json or .xml", file);
			return null;
		}
	}

	private FileNameAndResource tryJson(String file)
	{
		try (InputStream in = Files.newInputStream(Paths.get(file)))
		{
			IBaseResource resource = fhirContext.newJsonParser().parseResource(in);
			logger.info("{} read from {}", resource.getClass().getSimpleName(), file);
			return new FileNameAndResource(file, (Resource) resource);
		}
		catch (Exception e)
		{
			logger.warn("Unable to read {} as JSON, {}: {}", file, e.getClass().getName(), e.getMessage());
			return null;
		}
	}

	private FileNameAndResource tryXml(String file)
	{
		try (InputStream in = Files.newInputStream(Paths.get(file)))
		{
			IBaseResource resource = fhirContext.newXmlParser().parseResource(in);
			logger.info("{} read from {}", resource.getClass().getSimpleName(), file);
			return new FileNameAndResource(file, (Resource) resource);
		}
		catch (Exception e)
		{
			logger.warn("Unable to read {} as XML, {}: {}", file, e.getClass().getName(), e.getMessage());
			return null;
		}
	}
}
