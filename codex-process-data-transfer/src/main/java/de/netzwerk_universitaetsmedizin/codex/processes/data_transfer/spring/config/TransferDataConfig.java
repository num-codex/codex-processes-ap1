package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.ConsentClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.HapiFhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirClientBuilder;
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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ExtractPatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.FindNewData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.HandleNoConsentIdatMerge;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.HandleNoConsentUsageAndTransfer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.InsertDataIntoCodex;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ReadData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ReplacePseudonym;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ResolvePseudonym;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.SaveLastExportTo;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.StartTimer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.StopTimer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.StoreDataForCrr;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.StoreDataForTransferHub;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.ValidateData;

@Configuration
public class TransferDataConfig
{
	private static final Logger logger = LoggerFactory.getLogger(TransferDataConfig.class);

	@Autowired
	private FhirWebserviceClientProvider fhirClientProvider;

	@Autowired
	private TaskHelper taskHelper;

	@Autowired
	private OrganizationProvider organizationProvider;

	@Autowired
	private FhirContext fhirContext;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.serverBase:#{null}}")
	private String fhirStoreBaseUrl;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.username:#{null}}")
	private String fhirStoreUsername;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.password:#{null}}")
	private String fhirStorePassword;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.bearerToken:#{null}}")
	private String fhirStoreBearerToken;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.connectTimeout:10000}")
	private int fhirStoreConnectTimeout;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.socketTimeout:10000}")
	private int fhirStoreSocketTimeout;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.connectionRequestTimeout:10000}")
	private int fhirStoreConnectionRequestTimeout;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.hapiClientVerbose:false}")
	private boolean fhirStoreHapiClientVerbose;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.client:de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirBridgeClient}")
	private String fhirStoreClientClass;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.useChainedParameterNotLogicalReference:true}")
	private boolean fhirStoreUseChainedParameterNotLogicalReference;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fhir.searchBundleOverride:#{null}}")
	private String fhirStoreSearchBundleOverride;

	@Value("${de.netzwerk_universitaetsmedizin.codex.crr.publicKey:#{null}}")
	private String crrPublicKeyFile;

	@Value("${de.netzwerk_universitaetsmedizin.codex.crr.privateKey:#{null}}")
	private String crrPrivateKeyFile;

	@Value("${de.netzwerk_universitaetsmedizin.codex.geccoTransferHubIdentifierValue:gth.hs-heilbronn.de}")
	private String geccoTransferHubIdentifierValue;

	@Value("${de.netzwerk_universitaetsmedizin.codex.crrIdentifierValue:num-codex.de}")
	private String crrIdentifierValue;

	@Value("#{'${de.netzwerk_universitaetsmedizin.codex.consent.mdatTransferGrantedOids:2.16.840.1.113883.3.1937.777.24.5.3.8,2.16.840.1.113883.3.1937.777.24.5.3.9,2.16.840.1.113883.3.1937.777.24.5.3.33,2.16.840.1.113883.3.1937.777.24.5.3.34}'.split(',')}")
	private List<String> mdatTransferGrantedOids;

	@Value("#{'${de.netzwerk_universitaetsmedizin.codex.consent.idatMergeGrantedOids:2.16.840.1.113883.3.1937.777.24.5.3.4}'.split(',')}")
	private List<String> idatMergeGrantedOids;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.trustStore:#{null}}")
	private String fttpTrustStore;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.certificate:#{null}}")
	private String fttpCertificate;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.privateKey:#{null}}")
	private String fttpPrivateKey;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.connectTimeout:10000}")
	private int fttpConnectTimeout;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.socketTimeout:10000}")
	private int fttpSocketTimeout;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.connectionRequestTimeout:10000}")
	private int fttpConnectionRequestTimeout;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.basicAuthUsername:#{null}}")
	private String fttpBasicAuthUsername;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.basicAuthPassword:#{null}}")
	private String fttpBasicAuthPassword;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.serverBase:#{null}}")
	private String fttpServerBase;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.apiKey:#{null}}")
	private String fttpApiKey;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.study:num}")
	private String fttpStudy;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.target:codex}")
	private String fttpTarget;

	@Value("${de.netzwerk_universitaetsmedizin.codex.fttp.hapiClientVerbose:false}")
	private boolean fttpHapiClientVerbose;

	@Value("${org.highmed.dsf.bpe.fhir.remote.webservice.proxy.schemeHostPort:#{null}}")
	private String proxySchemeHostPort;

	@Value("${org.highmed.dsf.bpe.fhir.remote.webservice.proxy.username:#{null}}")
	private String proxyUsername;

	@Value("${org.highmed.dsf.bpe.fhir.remote.webservice.proxy.password:#{null}}")
	private String proxyPassword;

	@Value("${org.highmed.dsf.fhir.local-organization.identifier}")
	private String localIdentifierValue;

	@Bean
	public CrrKeyProvider crrKeyProvider()
	{
		return CrrKeyProviderImpl.fromFiles(crrPrivateKeyFile, crrPublicKeyFile);
	}

	@Bean
	public HapiFhirClientFactory hapiFhirClientFactory()
	{
		return new HapiFhirClientFactory(fhirContext, fhirStoreBaseUrl, fhirStoreUsername, fhirStorePassword,
				fhirStoreBearerToken, fhirStoreConnectTimeout, fhirStoreSocketTimeout,
				fhirStoreConnectionRequestTimeout, fhirStoreHapiClientVerbose,
				fhirStoreUseChainedParameterNotLogicalReference);
	}

	@Bean
	public FttpClientFactory fttpClientFactory()
	{
		Path trustStorePath = checkExists(fttpTrustStore);
		Path certificatePath = checkExists(fttpCertificate);
		Path privateKeyPath = checkExists(fttpPrivateKey);

		return new FttpClientFactory(trustStorePath, certificatePath, privateKeyPath, fttpConnectTimeout,
				fttpSocketTimeout, fttpConnectionRequestTimeout, fttpBasicAuthUsername, fttpBasicAuthPassword,
				fttpServerBase, fttpApiKey, fttpStudy, fttpTarget, proxySchemeHostPort, proxyUsername, proxyPassword,
				fttpHapiClientVerbose);
	}

	@Bean
	public FhirClientFactory fhirClientFactory()
	{
		Path searchBundleOverride = checkExists(fhirStoreSearchBundleOverride);

		return new FhirClientFactory(hapiFhirClientFactory(), fhirContext, searchBundleOverride, localIdentifierValue,
				clientBuilder());
	}

	private FhirClientBuilder clientBuilder()
	{
		return (fhirContext, clientFactory, searchBundleOverride) ->
		{
			logger.info("Using {} as fhir client", fhirStoreClientClass);

			try
			{
				@SuppressWarnings("unchecked")
				Class<FhirClient> clientClass = (Class<FhirClient>) Class.forName(fhirStoreClientClass);
				Constructor<FhirClient> constructor = clientClass.getConstructor(FhirContext.class,
						HapiFhirClientFactory.class, Path.class);

				return constructor.newInstance(fhirContext, clientFactory, searchBundleOverride);
			}
			catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				throw new RuntimeException(e);
			}
		};
	}

	private Path checkExists(String file)
	{
		if (file == null)
			return null;
		else
		{
			Path path = Paths.get(file);

			if (!Files.isReadable(path))
				throw new RuntimeException(path.toString() + " not readable");

			return path;
		}
	}

	@Bean
	public ConsentClientFactory consentClientFactory()
	{
		return new ConsentClientFactory();
	}

	// numCodexDataTrigger

	@Bean
	public StartTimer startTimer()
	{
		return new StartTimer(fhirClientProvider, taskHelper);
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
	public StopTimer stopTimer()
	{
		return new StopTimer(fhirClientProvider, taskHelper);
	}

	@Bean
	public SaveLastExportTo saveLastExportTo()
	{
		return new SaveLastExportTo(fhirClientProvider, taskHelper);
	}

	// numCodexDataSend

	@Bean
	public ExtractPatientReference extractPseudonym()
	{
		return new ExtractPatientReference(fhirClientProvider, taskHelper);
	}

	@Bean
	public ResolvePseudonym resolvePseudonym()
	{
		return new ResolvePseudonym(fhirClientProvider, taskHelper, fhirClientFactory(), fttpClientFactory());
	}

	@Bean
	public CheckConsent checkConsent()
	{
		return new CheckConsent(fhirClientProvider, taskHelper, consentClientFactory(), idatMergeGrantedOids,
				mdatTransferGrantedOids);
	}

	@Bean
	public HandleNoConsentUsageAndTransfer handleNoConsentUsageAndTransfer()
	{
		return new HandleNoConsentUsageAndTransfer(fhirClientProvider, taskHelper);
	}

	@Bean
	public HandleNoConsentIdatMerge handleNoConsentIdatMerge()
	{
		return new HandleNoConsentIdatMerge(fhirClientProvider, taskHelper);
	}

	@Bean
	public ReadData readData()
	{
		return new ReadData(fhirClientProvider, taskHelper, fhirContext, fhirClientFactory());
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
		return new InsertDataIntoCodex(fhirClientProvider, taskHelper, fhirContext, fhirClientFactory());
	}
}
