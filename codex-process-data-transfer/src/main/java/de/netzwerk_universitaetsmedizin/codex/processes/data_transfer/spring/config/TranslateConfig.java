package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.ReplacePseudonymBack;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.SetTimeoutError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.StoreDataForCrr;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.StoreValidationErrorForDic;

@Configuration
public class TranslateConfig
{
	@Autowired
	private TransferDataConfig transferDataConfig;

	@Bean
	public DownloadDataFromDic downloadDataFromDiz()
	{
		return new DownloadDataFromDic(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public ReplacePseudonym replacePseudonym()
	{
		return new ReplacePseudonym(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.fttpClientFactory());
	}

	@Bean
	public StoreDataForCrr storeDataForCodex()
	{
		return new StoreDataForCrr(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.endpointProvider(),
				transferDataConfig.crrIdentifierValue());
	}

	@Bean
	public StartReceiveProcess startReceiveProcess()
	{
		return new StartReceiveProcess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.organizationProvider(),
				transferDataConfig.fhirContext());
	}

	@Bean
	public SetTimeoutError setTimeoutError()
	{
		return new SetTimeoutError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public DeleteDataForCrr deleteDataForCrr()
	{
		return new DeleteDataForCrr(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public CheckForError checkForError()
	{
		return new CheckForError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public ContinueSendProcess continueSendProcess()
	{
		return new ContinueSendProcess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.organizationProvider(),
				transferDataConfig.fhirContext());
	}

	@Bean
	public LogSuccess logSuccess()
	{
		return new LogSuccess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public DownloadValidationErrorFromCrr downloadValidationErrorFromCrr()
	{
		return new DownloadValidationErrorFromCrr(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper());
	}

	@Bean
	public ReplacePseudonymBack replacePseudonymBack()
	{
		return new ReplacePseudonymBack(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public StoreValidationErrorForDic storeValidationErrorForDic()
	{
		return new StoreValidationErrorForDic(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public ContinueSendProcessWithValidationError continueSendProcessWithValidationError()
	{
		return new ContinueSendProcessWithValidationError(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper(),
				transferDataConfig.organizationProvider(), transferDataConfig.fhirContext());
	}

	@Bean
	public LogValidationError logValidationError()
	{
		return new LogValidationError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public DeleteValidationErrorForDic deleteValidationErrorForDic()
	{
		return new DeleteValidationErrorForDic(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public ContinueSendProcessWithError continueSendProcessWithError()
	{
		return new ContinueSendProcessWithError(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper(),
				transferDataConfig.organizationProvider(), transferDataConfig.fhirContext());
	}

	@Bean
	public LogError logError()
	{
		return new LogError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}
}
