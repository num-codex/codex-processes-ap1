package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.ConsentClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto.CrrKeyProviderImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartReceiveProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartSendProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartTranslateProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.CheckConsent;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.DecryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.DownloadDataFromDic;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.DownloadDataFromTransferHub;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.EncryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.FindNewData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.HandleNoConsent;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.InsertDataIntoCodex;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ReadData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ReadLastExecutionTime;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ReplacePseudonym;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.SaveBusinessKey;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.SaveLastExecutionTime;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.StoreDataForCrr;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.StoreDataForTransferHub;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ValidateData;

@Configuration
public class TransferDataConfig
{
	@Autowired
	private FhirWebserviceClientProvider fhirClientProvider;

	@Autowired
	private TaskHelper taskHelper;

	@Autowired
	private OrganizationProvider organizationProvider;

	@Autowired
	private FhirContext fhirContext;

	@Value("${de.netzwerk_universitaetsmedizin.codex.localFhirStoreBaseUrl:@null}")
	private String localFhirStoreBaseUrl;

	@Value("${de.netzwerk_universitaetsmedizin.codex.localFhirUsername:@null}")
	private String localFhirStoreUsername;

	@Value("${de.netzwerk_universitaetsmedizin.codex.localFhirPassword:@null}")
	private String localFhirStorePassword;

	@Value("${de.netzwerk_universitaetsmedizin.codex.crrPublicKeyFile:@null}")
	private String crrPublicKeyFile;

	@Value("${de.netzwerk_universitaetsmedizin.codex.crrPrivateKeyFile:@null}")
	private String crrPrivateKeyFile;

	@Value("${de.netzwerk_universitaetsmedizin.codex.geccoTransferHubIdentifierValue:hs-heilbronn.de}")
	private String geccoTransferHubIdentifierValue;

	@Value("${de.netzwerk_universitaetsmedizin.codex.crrIdentifierValue:num-codex.de}")
	private String crrIdentifierValue;

	@Value("${de.netzwerk_universitaetsmedizin.codex.usageGrantedOid:2.16.840.1.113883.3.1937.777.24.5.1.1}")
	private String usageGrantedOid;

	@Value("${de.netzwerk_universitaetsmedizin.codex.transferGrantedOid:2.16.840.1.113883.3.1937.777.24.5.1.34}")
	private String transferGrantedOid;


	@Bean
	public CrrKeyProvider crrKeyProvider()
	{
		return CrrKeyProviderImpl.fromFiles(crrPrivateKeyFile, crrPublicKeyFile);
	}

	@Bean
	public FhirClientFactory fhirClientFactory()
	{
		return new FhirClientFactory(fhirContext, localFhirStoreBaseUrl, localFhirStoreUsername, localFhirStorePassword,
				null);
	}

	@Bean
	public FttpClientFactory fttpClientFactory()
	{
		return new FttpClientFactory();
	}

	@Bean
	public ConsentClientFactory consentClientFactory()
	{
		return new ConsentClientFactory();
	}

	// numCodexDataTrigger

	// TODO remove if expression works
	// @Bean
	// public StartTimer startTimer()
	// {
	// return new StartTimer(fhirClientProvider, taskHelper);
	// }

	@Bean
	public SaveBusinessKey saveBusinessKey()
	{
		return new SaveBusinessKey(fhirClientProvider, taskHelper);
	}

	@Bean
	public ReadLastExecutionTime readLastExecutionTime()
	{
		return new ReadLastExecutionTime(fhirClientProvider, taskHelper);
	}

	@Bean
	public FindNewData findNewData()
	{
		return new FindNewData(fhirClientProvider, taskHelper, organizationProvider, fhirClientFactory());
	}

	@Bean
	public StartSendProcess startSendProcess()
	{
		return new StartSendProcess(fhirClientProvider, taskHelper, organizationProvider, fhirContext);
	}

	@Bean
	public SaveLastExecutionTime saveLastExecutionTime()
	{
		return new SaveLastExecutionTime(fhirClientProvider, taskHelper);
	}

	// TODO remove if expression works
	// @Bean
	// public StopTimer stopTimer()
	// {
	// return new StopTimer(fhirClientProvider, taskHelper);
	// }

	// numCodexDataSend

	@Bean
	public CheckConsent checkConsent()
	{
		return new CheckConsent(fhirClientProvider, taskHelper, consentClientFactory(), usageGrantedOid,
				transferGrantedOid);
	}

	@Bean
	public HandleNoConsent handleNoConsent()
	{
		return new HandleNoConsent(fhirClientProvider, taskHelper);
	}

	@Bean
	public ReadData readData()
	{
		return new ReadData(fhirClientProvider, taskHelper);
	}

	@Bean
	public ValidateData validateData()
	{
		return new ValidateData(fhirClientProvider, taskHelper);
	}

	@Bean
	public EncryptData encryptData()
	{
		return new EncryptData(fhirClientProvider, taskHelper, fhirContext, crrKeyProvider());
	}

	@Bean
	public StoreDataForTransferHub storeDataForTransferHub()
	{
		return new StoreDataForTransferHub(fhirClientProvider, taskHelper, geccoTransferHubIdentifierValue);
	}

	@Bean
	public StartTranslateProcess startTranslateProcess()
	{
		return new StartTranslateProcess(fhirClientProvider, taskHelper, organizationProvider, fhirContext);
	}

	// numCodexDataTranslate

	@Bean
	public DownloadDataFromDic downloadDataFromDiz()
	{
		return new DownloadDataFromDic(fhirClientProvider, taskHelper);
	}

	@Bean
	public ReplacePseudonym replacePseudonym()
	{
		return new ReplacePseudonym(fhirClientProvider, taskHelper, fttpClientFactory());
	}

	@Bean
	public StoreDataForCrr storeDataForCodex()
	{
		return new StoreDataForCrr(fhirClientProvider, taskHelper, crrIdentifierValue);
	}

	@Bean
	public StartReceiveProcess startReceiveProcess()
	{
		return new StartReceiveProcess(fhirClientProvider, taskHelper, organizationProvider, fhirContext);
	}

	// InsertDataIntoCodex

	@Bean
	public DownloadDataFromTransferHub downloadDataFromTransferHub()
	{
		return new DownloadDataFromTransferHub(fhirClientProvider, taskHelper);
	}

	@Bean
	public DecryptData decryptData()
	{
		return new DecryptData(fhirClientProvider, taskHelper, fhirContext, crrKeyProvider());
	}

	@Bean
	public InsertDataIntoCodex insertDataIntoCodex()
	{
		return new InsertDataIntoCodex(fhirClientProvider, taskHelper, fhirClientFactory());
	}
}
