package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Collections;
import java.util.Map;

@Configuration
@PropertySource(ignoreResourceNotFound = true, value = "file:process/rdp-application.properties")
public class RdpCrrConfig
{
	private static final Logger logger = LoggerFactory.getLogger(RdpCrrConfig.class);
	public static final String INVALID_CONFIG_MESSAGE = "Invalid Client Config map";
	public static final String VALID_CONFIG_MESSAGE = "Client Config found: {}";

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${rpdClient:#{null}}")
	private String rdpClientMapProperty;
	private Map<String, RdpClientConfigValues> rdpClientConfigValues;

	@PostConstruct
	private void convertClientMap()
	{
		if (rdpClientMapProperty == null || rdpClientMapProperty.isEmpty())
		{
			return;
		}

		try
		{
			rdpClientConfigValues = objectMapper.readValue(rdpClientMapProperty, new TypeReference<>()
			{
			});

			logger.info(VALID_CONFIG_MESSAGE, rdpClientConfigValues.keySet());
		}
		catch (JsonProcessingException e)
		{
			logger.error(INVALID_CONFIG_MESSAGE);
			rdpClientConfigValues = Collections.emptyMap();
			throw new RuntimeException(INVALID_CONFIG_MESSAGE, e);
		}
	}

	@Bean
	public Map<String, RdpClientConfigValues> getRdpClientMap()
	{
		return rdpClientConfigValues;
	}

	public static class RdpClientConfigValues
	{
		private String baseUrl;
		private String username;
		private String password;
		private String bearerToken;

		public String getBaseUrl()
		{
			return baseUrl;
		}

		public String getUsername()
		{
			return username;
		}

		public String getPassword()
		{
			return password;
		}

		public String getBearerToken()
		{
			return bearerToken;
		}

		@Override
		public String toString()
		{
			return "RdpClientConfigValues{" + "baseUrl='" + baseUrl + '\'' + ", Username='" + username + '\'' + '}';
		}
	}
}
