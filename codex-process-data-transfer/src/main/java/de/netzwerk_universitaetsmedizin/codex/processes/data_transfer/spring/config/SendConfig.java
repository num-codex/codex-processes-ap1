package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartTranslateProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.CheckConsent;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.CheckForError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.DecryptValidationErrorFromGth;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.DeleteDataForGth;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.DownloadValidationErrorFromGth;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.EncryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ExtractPatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.LogError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.LogSuccess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.LogValidationError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ReadData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ResolvePsn;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetNoConsentIdatMergeError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetNoConsentUsageAndTransferError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetTimeoutError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.StoreDataForGth;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ValidateData;

@Configuration
public class SendConfig
{
	@Autowired
	private TransferDataConfig transferDataConfig;

	@Bean
	public ExtractPatientReference extractPatientReference()
	{
		return new ExtractPatientReference(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public CheckConsent checkConsent()
	{
		return new CheckConsent(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.consentClientFactory(),
				transferDataConfig.idatMergeGrantedOids(), transferDataConfig.mdatTransferGrantedOids());
	}

	@Bean
	public SetNoConsentIdatMergeError setNoConsentIdatMergeError()
	{
		return new SetNoConsentIdatMergeError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public ResolvePsn resolvePsn()
	{
		return new ResolvePsn(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.geccoClientFactory(),
				transferDataConfig.fttpClientFactory());
	}

	@Bean
	public SetNoConsentUsageAndTransferError setNoConsentUsageAndTransferError()
	{
		return new SetNoConsentUsageAndTransferError(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper());
	}

	@Bean
	public ReadData readData()
	{
		return new ReadData(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.fhirContext(),
				transferDataConfig.geccoClientFactory());
	}

	@Bean
	public ValidateData validateData()
	{
		return new ValidateData(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.bundleValidatorFactory(),
				transferDataConfig.errorOutputParameterGenerator(), transferDataConfig.errorLogger());
	}

	@Bean
	public EncryptData encryptData()
	{
		return new EncryptData(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.fhirContext(),
				transferDataConfig.crrKeyProvider());
	}

	@Bean
	public StoreDataForGth storeDataForGth()
	{
		return new StoreDataForGth(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.endpointProvider(),
				transferDataConfig.gthIdentifierValue());
	}

	@Bean
	public StartTranslateProcess startTranslateProcess()
	{
		return new StartTranslateProcess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.organizationProvider(),
				transferDataConfig.fhirContext());
	}

	@Bean(name = "Send-setTimeoutError") // prefix to force distinct bean names
	public SetTimeoutError setTimeoutError()
	{
		return new SetTimeoutError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public DeleteDataForGth deleteDataForGth()
	{
		return new DeleteDataForGth(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean(name = "Send-checkForError") // prefix to force distinct bean names
	public CheckForError checkForError()
	{
		return new CheckForError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean
	public DownloadValidationErrorFromGth downloadValidationErrorFromGth()
	{
		return new DownloadValidationErrorFromGth(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper());
	}

	@Bean
	public DecryptValidationErrorFromGth decryptValidationErrorFromGth()
	{
		return new DecryptValidationErrorFromGth(transferDataConfig.fhirClientProvider(),
				transferDataConfig.taskHelper(), transferDataConfig.readAccessHelper());
	}

	@Bean(name = "Send-logSuccess") // prefix to force distinct bean names
	public LogSuccess logSuccess()
	{
		return new LogSuccess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean(name = "Send-logValidationError") // prefix to force distinct bean names
	public LogValidationError logValidationError()
	{
		return new LogValidationError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}

	@Bean(name = "Send-logError") // prefix to force distinct bean names
	public LogError logError()
	{
		return new LogError(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.errorOutputParameterGenerator());
	}
}
