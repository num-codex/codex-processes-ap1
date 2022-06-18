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
import org.highmed.dsf.tools.generator.ProcessDocumentation;
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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error.ErrorOutputParameterGenerator;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.ErrorLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartReceiveProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartSendProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartTranslateProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DecryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.DownloadDataFromTransferHub;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive.InsertDataIntoCodex;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.CheckConsent;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.EncryptData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ExtractPatientReference;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetNoConsentIdatMergeError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.SetNoConsentUsageAndTransferError;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ReadData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ResolvePseudonym;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.StoreDataForTransferHub;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send.ValidateData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.DownloadDataFromDic;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.ReplacePseudonym;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate.StoreDataForCrr;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.FindNewData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.SaveLastExportTo;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.StartTimer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.StopTimer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidatorFactory;

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

	@Autowired
	private BundleValidatorFactory bundleValidatorFactory;

	@ProcessDocumentation(description = "PEM encoded file with trusted certificates to validate the server-certificate of the GECCO FHIR server", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, recommendation = "Use docker secret file to configure", example = "/run/secrets/gecco_fhir_server_ca.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.trust.certificates:#{null}}")
	private String fhirStoreTrustStore;

	@ProcessDocumentation(description = "PEM encoded file with client-certificate, if GECCO FHIR server requires mutual TLS authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, recommendation = "Use docker secret file to configure", example = "/run/secrets/gecco_fhir_server_client_certificate.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.certificate:#{null}}")
	private String fhirStoreCertificate;

	@ProcessDocumentation(description = "PEM encoded file with private-key for the client-certificate defined via `de.netzwerk.universitaetsmedizin.codex.gecco.server.certificate`", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, recommendation = "Use docker secret file to configure", example = "/run/secrets/gecco_fhir_server_client_certificate_private_key.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.private.key:#{null}}")
	private String fhirStorePrivateKey;

	@ProcessDocumentation(description = "Password to decrypt the private-key defined via `de.netzwerk.universitaetsmedizin.codex.gecco.server.private.key`", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/gecco_fhir_server_client_certificate_private_key.pem.password")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.private.key.password:#{null}}")
	private char[] fhirStorePrivateKeyPassword;

	@ProcessDocumentation(description = "Base URL of the GECCO FHIR server", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, example = "http://foo.bar/fhir")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.base.url:#{null}}")
	private String fhirStoreBaseUrl;

	@ProcessDocumentation(description = "Basic authentication username to authenticate against the GECCO FHIR server, set if the server requests authentication using basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.basicauth.username:#{null}}")
	private String fhirStoreUsername;

	@ProcessDocumentation(description = "Basic authentication password to authenticate against the GECCO FHIR server, set if the server requests authentication using basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/gecco_fhir_server_basicauth.password")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.basicauth.password:#{null}}")
	private String fhirStorePassword;

	@ProcessDocumentation(description = "Bearer token to authenticate against the GECCO FHIR server, set if the server requests authentication using bearer token, cannot be set using docker secrets", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.bearer.token:#{null}}")
	private String fhirStoreBearerToken;

	@ProcessDocumentation(description = "Connection timeout in milliseconds used when accessing the GECCO FHIR server, time until a connection needs to be established before aborting", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.timeout.connect:10000}")
	private int fhirStoreConnectTimeout;

	@ProcessDocumentation(description = "Connection request timeout in milliseconds used when requesting a connection from the connection manager while accessing the GECCO FHIR server, time until a connection needs to be established before aborting", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.timeout.connection.request:10000}")
	private int fhirStoreConnectionRequestTimeout;

	@ProcessDocumentation(description = "Maximum period of inactivity in milliseconds between two consecutive data packets from the GECCO FHIR server, time until the server needs to send a data packet before aborting", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.timeout.socket:10000}")
	private int fhirStoreSocketTimeout;

	@ProcessDocumentation(description = "GECCO FHIR Server client implementation class", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.client:de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.FhirBridgeClient}")
	private String fhirStoreClientClass;

	@ProcessDocumentation(description = "To enable verbose logging of requests and replies to the GECCO FHIR server set to `true`", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.client.hapi.verbose:false}")
	private boolean fhirStoreHapiClientVerbose;

	@ProcessDocumentation(description = "Forwarding proxy server url, set if the GECCO FHIR server can only be reached via a proxy server", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, example = "http://proxy.foo:8080")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.proxy.url:#{null}}")
	private String fhirStoreProxyUrl;

	@ProcessDocumentation(description = "Forwarding proxy server basic authentication username, set if the GECCO FHIR server can only be reached via a proxy server that requires basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataReceive" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.proxy.username:#{null}}")
	private String fhirStoreProxyUsername;

	@ProcessDocumentation(description = "Forwarding proxy server basic authentication password, set if the GECCO FHIR server can only be reached via a proxy server that requires basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataReceive" }, recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/gecco_fhir_server_proxy_basicauth.password")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.proxy.password:#{null}}")
	private String fhirStoreProxyPassword;

	@ProcessDocumentation(description = "To enable the use of logical references instead of chained parameters (`patient:identifier` instead of `patient.identifier`) while searching for Patients in the GECCO FHIR server set to `true`", processNames = "wwwnetzwerk-universitaetsmedizinde_dataReceive")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.use.chained.parameter.not.logical.reference:true}")
	private boolean fhirStoreUseChainedParameterNotLogicalReference;

	@ProcessDocumentation(description = "Location of a FHIR batch bundle used to override the internally provided bundle used to search for GECCO FHIR ressources", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gecco.server.search.bundle.override:#{null}}")
	private String fhirStoreSearchBundleOverride;

	@ProcessDocumentation(description = "Location of the CRR public-key e.g. [crr_public-key-pre-prod.pem](https://keys.num-codex.de/crr_public-key-pre-prod.pem) used to RSA encrypt FHIR bundles for the central repository", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend", recommendation = "Ask central repository management for the correct public key regarding the test and production environments", example = "/run/secrets/crr_public-key-pre-prod.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.crr.public.key:#{null}}")
	private String crrPublicKeyFile;

	@ProcessDocumentation(description = "Location of the CRR private-key used to RSA decrypt FHIR bundles for the central repository", processNames = "wwwnetzwerk-universitaetsmedizinde_dataReceive", example = "/run/secrets/crr_private-key-pre-prod.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.crr.private.key:#{null}}")
	private String crrPrivateKeyFile;

	@ProcessDocumentation(description = "DSF organization identifier of the GECCO Transfer Hub", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("${de.netzwerk.universitaetsmedizin.codex.gth.identifier.value:hs-heilbronn.de}")
	private String geccoTransferHubIdentifierValue;

	@ProcessDocumentation(description = "DSF organization identifier of the central research repository", processNames = "wwwnetzwerk-universitaetsmedizinde_dataTranslate")
	@Value("${de.netzwerk.universitaetsmedizin.codex.crr.identifier.value:num-codex.de}")
	private String crrIdentifierValue;

	@ProcessDocumentation(description = "List of expected consent provision permit codes for exporting GECCO resources", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("#{'${de.netzwerk.universitaetsmedizin.codex.consent.granted.oids.mdat.transfer:"
			+ "2.16.840.1.113883.3.1937.777.24.5.3.8," + "2.16.840.1.113883.3.1937.777.24.5.3.9,"
			+ "2.16.840.1.113883.3.1937.777.24.5.3.33," + "2.16.840.1.113883.3.1937.777.24.5.3.34"
			+ "}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> mdatTransferGrantedOids;

	@ProcessDocumentation(description = "List of expected consent provision permit codes for requesting a pseudonym based on a bloom filter from the fTTP", processNames = "wwwnetzwerk-universitaetsmedizinde_dataSend")
	@Value("#{'${de.netzwerk.universitaetsmedizin.codex.consent.granted.oids.idat.merge:"
			+ "2.16.840.1.113883.3.1937.777.24.5.3.4}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> idatMergeGrantedOids;

	@ProcessDocumentation(description = "PEM encoded file with trusted certificates to validate the server-certificate of the fTTP server", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Use docker secret file to configure", example = "/run/secrets/fttp_server_ca.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.trust.certificates:#{null}}")
	private String fttpTrustStore;

	@ProcessDocumentation(description = "PEM encoded file with client-certificate used to authenticated against fTTP server", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Use docker secret file to configure", example = "/run/secrets/fttp_server_client_certificate.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.certificate:#{null}}")
	private String fttpCertificate;

	@ProcessDocumentation(description = "PEM encoded file with private-key for the client-certificate defined via `de.netzwerk.universitaetsmedizin.codex.fttp.certificate`", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Use docker secret file to configure", example = "/run/secrets/fttp_server_client_certificate_private_key.pem")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.private.key:#{null}}")
	private String fttpPrivateKey;

	@ProcessDocumentation(description = "Password to decrypt the private-key defined via `de.netzwerk.universitaetsmedizin.codex.fttp.private.key`", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/fttp_server_client_certificate_private_key.pem.password")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.private.key.password:#{null}}")
	private char[] fttpPrivateKeyPassword;

	@ProcessDocumentation(description = "Connection timeout in milliseconds used when accessing the fTTP server, time until a connection needs to be established before aborting", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataTranslate" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.timeout.connect:10000}")
	private int fttpConnectTimeout;

	@ProcessDocumentation(description = "Connection request timeout in milliseconds used when requesting a connection from the connection manager while accessing the fTTP server, time until a connection needs to be established before aborting", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataTranslate" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.timeout.connection.request:10000}")
	private int fttpConnectionRequestTimeout;

	@ProcessDocumentation(description = "Maximum period of inactivity in milliseconds between two consecutive data packets from the fTTP server, time until the server needs to send a data packet before aborting", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataTranslate" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.timeout.socket:10000}")
	private int fttpSocketTimeout;

	@ProcessDocumentation(description = "Basic authentication username to authenticate against the fTTP server, set if the server requests authentication using basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataTranslate" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.basicauth.username:#{null}}")
	private String fttpBasicAuthUsername;

	@ProcessDocumentation(description = "Basic authentication password to authenticate against the fTTP server, set if the server requests authentication using basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/fttp_server_basicauth.password")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.basicauth.password:#{null}}")
	private String fttpBasicAuthPassword;

	@ProcessDocumentation(description = "TODO", processNames = { "wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Specify if you are using the send process to request pseudonyms from the fTTP")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.server.base.url:#{null}}")
	private String fttpServerBase;

	@ProcessDocumentation(description = "Your organizations API key provided by the fTTP, the fTTP API key can not be defined via docker secret file and needs to be defined directly via the environment variable", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Specify if you are using the send process to request pseudonyms from the fTTP")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.api.key:#{null}}")
	private String fttpApiKey;

	@ProcessDocumentation(description = "Study identifier specified by the fTTP", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataTranslate" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.study:num}")
	private String fttpStudy;

	@ProcessDocumentation(description = "Pseudonymization domain target identifier specified by the fTTP", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, example = "dic_heidelberg", recommendation = "Specify if you are using the send process to request pseudonyms from the fTTP")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.target:codex}")
	private String fttpTarget;

	@ProcessDocumentation(description = "To enable verbose logging of requests and replies to the fTTP server set to `true`", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataTranslate" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.client.hapi.verbose:false}")
	private boolean fttpHapiClientVerbose;

	@ProcessDocumentation(description = "Forwarding proxy server url, set if the fTTP server can only be reached via a proxy server", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, example = "http://proxy.foo:8080")
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.proxy.url:#{null}}")
	private String fttpProxyUrl;

	@ProcessDocumentation(description = "Forwarding proxy server basic authentication username, set if the fTTPserver can only be reached via a proxy server that requires basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend", "wwwnetzwerk-universitaetsmedizinde_dataTranslate" })
	@Value("${de.netzwerk.universitaetsmedizin.codex.fttp.proxy.username:#{null}}")
	private String fttpProxyUsername;

	@ProcessDocumentation(description = "Forwarding proxy server basic authentication password, set if the fTTP server can only be reached via a proxy server that requires basic authentication", processNames = {
			"wwwnetzwerk-universitaetsmedizinde_dataSend",
			"wwwnetzwerk-universitaetsmedizinde_dataTranslate" }, recommendation = "Use docker secret file to configure by using `${env_variable}_FILE`", example = "/run/secrets/fttp_server_proxy_basicauth.password")
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
	public SetNoConsentUsageAndTransferError setNoConsentUsageAndTransferError()
	{
		return new SetNoConsentUsageAndTransferError(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SetNoConsentIdatMergeError setNoConsentIdatMergeError()
	{
		return new SetNoConsentIdatMergeError(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public ReadData readData()
	{
		return new ReadData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext, geccoClientFactory());
	}

	@Bean
	public ValidateData validateData()
	{
		return new ValidateData(fhirClientProvider, taskHelper, readAccessHelper, bundleValidatorFactory,
				errorOutputParameterGenerator(), errorLogger());
	}

	@Bean
	public ErrorOutputParameterGenerator errorOutputParameterGenerator()
	{
		return new ErrorOutputParameterGenerator();
	}

	@Bean
	public ErrorLogger errorLogger()
	{
		return new ErrorLogger();
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
