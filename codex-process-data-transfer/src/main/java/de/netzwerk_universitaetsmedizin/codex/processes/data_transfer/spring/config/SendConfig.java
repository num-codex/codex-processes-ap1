package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.listener.AfterDryRunEndListener;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartTranslateProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.CheckConsent;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.CheckDryRun;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.CheckForError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.DecryptValidationErrorFromDts;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.DeleteDataForDts;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.DownloadValidationErrorFromDts;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.EncryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ExtractPatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.LogDryRunSuccess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.LogError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.LogSuccess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.LogValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ReadData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ResolvePsn;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetNoConsentIdatMergeError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetNoConsentUsageAndTransferError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetTimeoutError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.StoreDataForDts;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ValidateData;
import dev.dsf.bpe.v1.ProcessPluginApi;

@Configuration
public class SendConfig
{
	@Autowired
	private TransferDataConfig transferDataConfig;

	@Autowired
	private ValidationConfig validationConfig;

	@Autowired
	private ProcessPluginApi api;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ExtractPatientReference extractPatientReference()
	{
		return new ExtractPatientReference(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public CheckConsent checkConsent()
	{
		return new CheckConsent(api, transferDataConfig.consentClientFactory(),
				transferDataConfig.idatMergeGrantedOids(), transferDataConfig.mdatTransferGrantedOids());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SetNoConsentIdatMergeError setNoConsentIdatMergeError()
	{
		return new SetNoConsentIdatMergeError(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ResolvePsn resolvePsn()
	{
		return new ResolvePsn(api, transferDataConfig.dataStoreClientFactory(), transferDataConfig.fttpClientFactory());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SetNoConsentUsageAndTransferError setNoConsentUsageAndTransferError()
	{
		return new SetNoConsentUsageAndTransferError(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ReadData readData()
	{
		return new ReadData(api, transferDataConfig.dataStoreClientFactory(), transferDataConfig.dataLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ValidateData validateData()
	{
		return new ValidateData(api, validationConfig.bundleValidatorFactory(),
				transferDataConfig.errorOutputParameterGenerator(), transferDataConfig.errorLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public EncryptData encryptData()
	{
		return new EncryptData(api, transferDataConfig.crrKeyProvider());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public CheckDryRun checkDryRun()
	{
		return new CheckDryRun(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogDryRunSuccess logDryRunSuccess()
	{
		return new LogDryRunSuccess(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public AfterDryRunEndListener afterDryRunEndListener()
	{
		return new AfterDryRunEndListener(api, transferDataConfig.getSendDryRunSuccessMail());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StoreDataForDts storeDataForDts()
	{
		return new StoreDataForDts(api, transferDataConfig.dtsIdentifierValue());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StartTranslateProcess startTranslateProcess()
	{
		return new StartTranslateProcess(api, transferDataConfig.dtsIdentifierValue());
	}

	@Bean(name = "Send-setTimeoutError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SetTimeoutError setTimeoutError()
	{
		return new SetTimeoutError(api);
	}

	@Bean(name = "Send-checkForError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public CheckForError checkForError()
	{
		return new CheckForError(api, new DataTransferProcessPluginDefinition().getResourceVersion());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DeleteDataForDts deleteDataForDts()
	{
		return new DeleteDataForDts(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DownloadValidationErrorFromDts downloadValidationErrorFromDts()
	{
		return new DownloadValidationErrorFromDts(api, transferDataConfig.dtsIdentifierValue());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DecryptValidationErrorFromDts decryptValidationErrorFromDts()
	{
		return new DecryptValidationErrorFromDts(api);
	}

	@Bean(name = "Send-logSuccess") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogSuccess logSuccess()
	{
		return new LogSuccess(api);
	}

	@Bean(name = "Send-logValidationError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogValidationError logValidationError()
	{
		return new LogValidationError(api, transferDataConfig.errorOutputParameterGenerator(),
				transferDataConfig.errorLogger());
	}

	@Bean(name = "Send-logError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogError logError()
	{
		return new LogError(api, transferDataConfig.errorOutputParameterGenerator(), transferDataConfig.errorLogger());
	}
}
