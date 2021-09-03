package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.ConsentClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.GeccoFhirClient;
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
	@Autowired
	private FhirWebserviceClientProvider fhirClientProvider;

	@Autowired
	private TaskHelper taskHelper;

	@Autowired
	private ReadAccessHelper readAccessHelper;

	@Autowired
	private OrganizationProvider organizationProvider;

	@Autowired
	private EndpointProvider endpointProvider;

	@Autowired
	private FhirContext fhirContext;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.trust.certificates:#{null}}")
	private String fhirStoreTrustStore;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.certificate:#{null}}")
	private String fhirStoreCertificate;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.private.key:#{null}}")
	private String fhirStorePrivateKey;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.private.key.password:#{null}}")
	private char[] fhirStorePrivateKeyPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.base.url:#{null}}")
	private String fhirStoreBaseUrl;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.basicauth.username:#{null}}")
	private String fhirStoreUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.basicauth.password:#{null}}")
	private String fhirStorePassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.bearer.token:#{null}}")
	private String fhirStoreBearerToken;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.timeout.connect:10000}")
	private int fhirStoreConnectTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.timeout.socket:10000}")
	private int fhirStoreSocketTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.timeout.connection.request:10000}")
	private int fhirStoreConnectionRequestTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.client:de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirBridgeClient}")
	private String fhirStoreClientClass;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.client.hapi.verbose:false}")
	private boolean fhirStoreHapiClientVerbose;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.proxy.url:#{null}}")
	private String fhirStoreProxyUrl;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.proxy.username:#{null}}")
	private String fhirStoreProxyUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.proxy.password:#{null}}")
	private String fhirStoreProxyPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.use.chained.parameter.not.logical.reference:true}")
	private boolean fhirStoreUseChainedParameterNotLogicalReference;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.search.bundle.override:#{null}}")
	private String fhirStoreSearchBundleOverride;

	@Value("${de.netzwerk.universitaetsmedizin.codex.crr.public.key:#{null}}")
	private String crrPublicKeyFile;

	@Value("${de.netzwerk.universitaetsmedizin.codex.crr.private.key:#{null}}")
	private String crrPrivateKeyFile;

	@Value("${de.netzwerk.universitaetsmedizin.codex.gth.identifier.value:hs-heilbronn.de}")
	private String geccoTransferHubIdentifierValue;

	@Value("${de.netzwerk.universitaetsmedizin.codex.crr.identifier.value:num-codex.de}")
	private String crrIdentifierValue;

	@Value("#{'${de.netzwerk.universitaetsmedizin.codex.consent.granted.oids.mdat.transfer:2.16.840.1.113883.3.1937.777.24.5.3.8,2.16.840.1.113883.3.1937.777.24.5.3.9,2.16.840.1.113883.3.1937.777.24.5.3.33,2.16.840.1.113883.3.1937.777.24.5.3.34}'.split(',')}")
	private List<String> mdatTransferGrantedOids;

	@Value("#{'${de.netzwerk.universitaetsmedizin.codex.consent.granted.oids.idat.merge:2.16.840.1.113883.3.1937.777.24.5.3.4}'.split(',')}")
	private List<String> idatMergeGrantedOids;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.trust.certificates:#{null}}")
	private String fttpTrustStore;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.certificate:#{null}}")
	private String fttpCertificate;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.private.key:#{null}}")
	private String fttpPrivateKey;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.private.key.password:#{null}}")
	private char[] fttpPrivateKeyPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.timeout.connect:10000}")
	private int fttpConnectTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.timeout.socket:10000}")
	private int fttpSocketTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.timeout.connection.request:10000}")
	private int fttpConnectionRequestTimeout;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.basicauth.username:#{null}}")
	private String fttpBasicAuthUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.basicauth.password:#{null}}")
	private String fttpBasicAuthPassword;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.server.base.url:#{null}}")
	private String fttpServerBase;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.api.key:#{null}}")
	private String fttpApiKey;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.study:num}")
	private String fttpStudy;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.target:codex}")
	private String fttpTarget;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.client.hapi.verbose:false}")
	private boolean fttpHapiClientVerbose;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.proxy.url:#{null}}")
	private String fttpProxyUrl;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.proxy.username:#{null}}")
	private String fttpProxyUsername;

	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.proxy.password:#{null}}")
	private String fttpProxyPassword;

	@Value("${org.highmed.dsf.bpe.fhir.server.organization.identifier.value}")
	private String localIdentifierValue;

	@Bean
	public CrrKeyProvider crrKeyProvider()
	{
		return CrrKeyProviderImpl.fromFiles(crrPrivateKeyFile, crrPublicKeyFile);
	}

	@Bean
	public FttpClientFactory fttpClientFactory()
	{
		Path trustStorePath = checkExists(fttpTrustStore);
		Path certificatePath = checkExists(fttpCertificate);
		Path privateKeyPath = checkExists(fttpPrivateKey);

		return new FttpClientFactory(trustStorePath, certificatePath, privateKeyPath, fttpPrivateKeyPassword,
				fttpConnectTimeout, fttpSocketTimeout, fttpConnectionRequestTimeout, fttpBasicAuthUsername,
				fttpBasicAuthPassword, fttpServerBase, fttpApiKey, fttpStudy, fttpTarget, fttpProxyUrl,
				fttpProxyUsername, fttpProxyPassword, fttpHapiClientVerbose);
	}

	@Bean
	@SuppressWarnings("unchecked")
	public GeccoClientFactory geccoClientFactory()
	{
		Path trustStorePath = checkExists(fhirStoreTrustStore);
		Path certificatePath = checkExists(fhirStoreCertificate);
		Path privateKeyPath = checkExists(fhirStorePrivateKey);
		Path searchBundleOverride = checkExists(fhirStoreSearchBundleOverride);

		try
		{
			return new GeccoClientFactory(trustStorePath, certificatePath, privateKeyPath, fhirStorePrivateKeyPassword,
					fhirStoreConnectTimeout, fhirStoreSocketTimeout, fhirStoreConnectionRequestTimeout,
					fhirStoreBaseUrl, fhirStoreUsername, fhirStorePassword, fhirStoreBearerToken, fhirStoreProxyUrl,
					fhirStoreProxyUsername, fhirStoreProxyPassword, fhirStoreHapiClientVerbose, fhirContext,
					searchBundleOverride, localIdentifierValue,
					(Class<GeccoFhirClient>) Class.forName(fhirStoreClientClass),
					fhirStoreUseChainedParameterNotLogicalReference);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
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
		return new StartTimer(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public FindNewData findNewData()
	{
		return new FindNewData(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider, endpointProvider,
				geccoClientFactory());
	}

	@Bean
	public StartSendProcess startSendProcess()
	{
		return new StartSendProcess(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	@Bean
	public StopTimer stopTimer()
	{
		return new StopTimer(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SaveLastExportTo saveLastExportTo()
	{
		return new SaveLastExportTo(fhirClientProvider, taskHelper, readAccessHelper);
	}

	// numCodexDataSend

	@Bean
	public ExtractPatientReference extractPseudonym()
	{
		return new ExtractPatientReference(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public ResolvePseudonym resolvePseudonym()
	{
		return new ResolvePseudonym(fhirClientProvider, taskHelper, readAccessHelper, geccoClientFactory(),
				fttpClientFactory());
	}

	@Bean
	public CheckConsent checkConsent()
	{
		return new CheckConsent(fhirClientProvider, taskHelper, readAccessHelper, consentClientFactory(),
				idatMergeGrantedOids, mdatTransferGrantedOids);
	}

	@Bean
	public HandleNoConsentUsageAndTransfer handleNoConsentUsageAndTransfer()
	{
		return new HandleNoConsentUsageAndTransfer(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public HandleNoConsentIdatMerge handleNoConsentIdatMerge()
	{
		return new HandleNoConsentIdatMerge(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public ReadData readData()
	{
		return new ReadData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext, geccoClientFactory());
	}

	@Bean
	public ValidateData validateData()
	{
		return new ValidateData(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public EncryptData encryptData()
	{
		return new EncryptData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext, crrKeyProvider());
	}

	@Bean
	public StoreDataForTransferHub storeDataForTransferHub()
	{
		return new StoreDataForTransferHub(fhirClientProvider, taskHelper, readAccessHelper, endpointProvider,
				geccoTransferHubIdentifierValue);
	}

	@Bean
	public StartTranslateProcess startTranslateProcess()
	{
		return new StartTranslateProcess(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	// numCodexDataTranslate

	@Bean
	public DownloadDataFromDic downloadDataFromDiz()
	{
		return new DownloadDataFromDic(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public ReplacePseudonym replacePseudonym()
	{
		return new ReplacePseudonym(fhirClientProvider, taskHelper, readAccessHelper, fttpClientFactory());
	}

	@Bean
	public StoreDataForCrr storeDataForCodex()
	{
		return new StoreDataForCrr(fhirClientProvider, taskHelper, readAccessHelper, endpointProvider,
				crrIdentifierValue);
	}

	@Bean
	public StartReceiveProcess startReceiveProcess()
	{
		return new StartReceiveProcess(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	// InsertDataIntoCodex

	@Bean
	public DownloadDataFromTransferHub downloadDataFromTransferHub()
	{
		return new DownloadDataFromTransferHub(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public DecryptData decryptData()
	{
		return new DecryptData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext, crrKeyProvider());
	}

	@Bean
	public InsertDataIntoCodex insertDataIntoCodex()
	{
		return new InsertDataIntoCodex(fhirClientProvider, taskHelper, readAccessHelper, fhirContext,
				geccoClientFactory());
	}
}
