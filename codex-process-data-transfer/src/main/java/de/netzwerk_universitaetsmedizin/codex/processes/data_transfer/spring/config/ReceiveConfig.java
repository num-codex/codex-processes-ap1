package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcessWithError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueTranslateProcessWithValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.*;
import dev.dsf.bpe.v1.ProcessPluginApi;

@Configuration
public class ReceiveConfig
{

	@Autowired
	private ProcessPluginApi api;

	@Autowired
	private TransferDataConfig transferDataConfig;

	@Autowired
	private RdpCrrConfig rdpCrrConfig;

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
				transferDataConfig.dataLogger(), api.getFhirContext(), rdpCrrConfig.getRdpClientMap());
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
}
