package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Configuration
@PropertySource(ignoreResourceNotFound = true, value = "file:process/rdp-application.properties")
public class ReceiveDataStoreConfig
{
	private static final Logger logger = LoggerFactory.getLogger(ReceiveDataStoreConfig.class);
	public static final String INVALID_CONFIG_MESSAGE = "Invalid Client Config map";
	public static final String VALID_CONFIG_MESSAGE = "Client Config found: {}";

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${dataStores:#{null}}")
	private String dataStoresProperty;
	private Map<String, DataStoreConnectionValues> dataStoreConnectionConfigs = new HashMap<>();;

	@PostConstruct
	private void convertClientMap()
	{
		if (dataStoresProperty == null || dataStoresProperty.isEmpty())
		{
			return;
		}

		try
		{
			dataStoreConnectionConfigs = objectMapper.readValue(dataStoresProperty, new TypeReference<>()
			{
			});

			logger.info(VALID_CONFIG_MESSAGE, dataStoreConnectionConfigs.keySet());
		}
		catch (JsonProcessingException e)
		{
			logger.error(INVALID_CONFIG_MESSAGE);
			throw new RuntimeException(INVALID_CONFIG_MESSAGE, e);
		}
	}

	@Bean
	public Map<String, DataStoreConnectionValues> getDataStoreConnectionConfigs()
	{
		return dataStoreConnectionConfigs;
	}

	public static class DataStoreConnectionValues
	{
		private String baseUrl;
		private String username;
		private String password;
		private String bearerToken;

		public DataStoreConnectionValues()
		{
		}

		public DataStoreConnectionValues(String baseUrl, String username, String password, String bearerToken)
		{
			this.baseUrl = baseUrl;
			this.username = username;
			this.password = password;
			this.bearerToken = bearerToken;
		}

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
