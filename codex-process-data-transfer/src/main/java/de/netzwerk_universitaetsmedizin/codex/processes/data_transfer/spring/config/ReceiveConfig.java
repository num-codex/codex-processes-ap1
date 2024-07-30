package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcessWithError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcessWithValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DecryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DeleteValidationErrorForDts;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DownloadDataFromDts;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.EncryptValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.InsertDataIntoCodex;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.LogError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.LogSuccess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.LogValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.StoreValidationErrorForDts;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;
import jakarta.annotation.PostConstruct;

@Configuration
public class ReceiveConfig
{
	private static final Logger logger = LoggerFactory.getLogger(ReceiveConfig.class);
	public static final String INVALID_CONFIG_MESSAGE = "Invalid Client Config, incorrect key value pair, key = '{}'";
	public static final String VALID_CONFIG_MESSAGE = "Client Config found: {}";
	public static final String CLIENT_SEPARATOR = ",";
	public static final String KEY_VALUE_SEPARATOR = "=";

	@Autowired
	private ProcessPluginApi api;

	@Autowired
	private TransferDataConfig transferDataConfig;

	@ProcessDocumentation(description = "A Test Value", processNames = "wwwnetzwerk-universitaetsmedizinde_receive")
	@Value("${de.netzwerk.universitaetsmedizin.rdp.client.map:#{null}}")
	private String clientConfig;

	private Map<String, String> clientConfigMap;

	@PostConstruct
	private void convertConfigValues()
	{
		if (clientConfig == null)
		{
			clientConfigMap = Collections.emptyMap();
			return;
		}

		clientConfigMap = new HashMap<>();
		String[] configList = clientConfig.split(CLIENT_SEPARATOR);
		for (String config : configList)
		{
			String[] configEntrySet = config.split(KEY_VALUE_SEPARATOR);
			if (configEntrySet.length % 2 != 0)
			{
				logger.warn(INVALID_CONFIG_MESSAGE, configEntrySet[0]);
				continue;
			}
			clientConfigMap.put(configEntrySet[0].trim(), configEntrySet[1].trim());
		}
		logger.info(VALID_CONFIG_MESSAGE, clientConfigMap.keySet());
		logger.debug(VALID_CONFIG_MESSAGE, clientConfigMap);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DownloadDataFromDts downloadDataFromDts()
	{
		return new DownloadDataFromDts(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DecryptData decryptData()
	{
		return new DecryptData(api, transferDataConfig.crrKeyProvider());
	}

	@Bean
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public InsertDataIntoCodex insertDataIntoCodex()
	{
		return new InsertDataIntoCodex(api, transferDataConfig.dataStoreClientFactory(),
				transferDataConfig.dataLogger(), api.getFhirContext(), getClientConfigMap());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ContinueTranslateProcess continueTranslateProcess()
	{
		return new ContinueTranslateProcess(api, transferDataConfig.dtsIdentifierValue());
	}

	@Bean(name = "Receive-logSuccess") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogSuccess logSuccess()
	{
		return new LogSuccess(api);
	}

	@Bean(name = "Receive-logValidation") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogValidationError logValidationError()
	{
		return new LogValidationError(api, transferDataConfig.errorOutputParameterGenerator(),
				transferDataConfig.errorLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public EncryptValidationError encryptValidationError()
	{
		return new EncryptValidationError(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StoreValidationErrorForDts storeValidationErrorForDts()
	{
		return new StoreValidationErrorForDts(api, transferDataConfig.dtsIdentifierValue(),
				transferDataConfig.dataLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ContinueTranslateProcessWithValidationError continueTranslateProcessWithValidationError()
	{
		return new ContinueTranslateProcessWithValidationError(api, transferDataConfig.dtsIdentifierValue());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DeleteValidationErrorForDts deleteValidationErrorForDts()
	{
		return new DeleteValidationErrorForDts(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ContinueTranslateProcessWithError continueTranslateProcessWithError()
	{
		return new ContinueTranslateProcessWithError(api, transferDataConfig.dtsIdentifierValue(),
				transferDataConfig.errorInputParameterGenerator());
	}

	@Bean(name = "Receive-logError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogError logError()
	{
		return new LogError(api, transferDataConfig.errorOutputParameterGenerator(), transferDataConfig.errorLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	public Map<String, String> getClientConfigMap()
	{
		return clientConfigMap;
	}
}
