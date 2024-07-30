package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueSendProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueSendProcessWithError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.ContinueSendProcessWithValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartReceiveProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.CheckForError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.DeleteDataForCrr;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.DeleteValidationErrorForDic;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.DownloadDataFromDic;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.DownloadValidationErrorFromCrr;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.LogError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.LogSuccess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.LogValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.ReplacePseudonym;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.SetTimeoutError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.StoreDataForCrr;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.StoreValidationErrorForDic;
import dev.dsf.bpe.v1.ProcessPluginApi;

@Configuration
public class TranslateConfig
{
	@Autowired
	private ProcessPluginApi api;

	@Autowired
	private TransferDataConfig transferDataConfig;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DownloadDataFromDic downloadDataFromDiz()
	{
		return new DownloadDataFromDic(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ReplacePseudonym replacePseudonym()
	{
		return new ReplacePseudonym(api, transferDataConfig.fttpClientFactory());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StoreDataForCrr storeDataForCodex()
	{
		return new StoreDataForCrr(api, transferDataConfig.crrIdentifierValue(), transferDataConfig.dataLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StartReceiveProcess startReceiveProcess()
	{
		return new StartReceiveProcess(api, transferDataConfig.crrIdentifierValue(), api.getFhirContext());
	}

	@Bean(name = "Translate-setTimeoutError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SetTimeoutError setTimeoutError()
	{
		return new SetTimeoutError(api);
	}

	@Bean(name = "Translate-checkForError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public CheckForError checkForError()
	{
		return new CheckForError(api, new DataTransferProcessPluginDefinition().getResourceVersion());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DeleteDataForCrr deleteDataForCrr()
	{
		return new DeleteDataForCrr(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ContinueSendProcess continueSendProcess()
	{
		return new ContinueSendProcess(api);
	}

	@Bean(name = "Translate-logSuccess") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogSuccess logSuccess()
	{
		return new LogSuccess(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DownloadValidationErrorFromCrr downloadValidationErrorFromCrr()
	{
		return new DownloadValidationErrorFromCrr(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StoreValidationErrorForDic storeValidationErrorForDic()
	{
		return new StoreValidationErrorForDic(api, transferDataConfig.dataLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ContinueSendProcessWithValidationError continueSendProcessWithValidationError()
	{
		return new ContinueSendProcessWithValidationError(api);
	}

	@Bean(name = "Translate-logValidationError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogValidationError logValidationError()
	{
		return new LogValidationError(api, transferDataConfig.errorOutputParameterGenerator(),
				transferDataConfig.errorLogger());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DeleteValidationErrorForDic deleteValidationErrorForDic()
	{
		return new DeleteValidationErrorForDic(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ContinueSendProcessWithError continueSendProcessWithError()
	{
		return new ContinueSendProcessWithError(api, transferDataConfig.errorInputParameterGenerator());
	}

	@Bean(name = "Translate-logError") // prefix to force distinct bean names
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public LogError logError()
	{
		return new LogError(api, transferDataConfig.errorOutputParameterGenerator(), transferDataConfig.errorLogger());
	}
}
