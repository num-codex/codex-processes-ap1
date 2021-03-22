package de.netzwerk_universitaetsmedizin.codex.processes.tools.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.tools.generator.CertificateGenerator.CertificateFiles;
import de.rwh.utils.crypto.CertificateAuthority;

public class TestDataGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);

	private static final CertificateGenerator certificateGenerator = new CertificateGenerator();
	private static final BundleGenerator bundleGenerator = new BundleGenerator();
	private static final ConfigGenerator configGenerator = new ConfigGenerator();
	private static final RsaKeyPairGenerator rsaKeyPairGenerator = new RsaKeyPairGenerator();

	static
	{
		CertificateAuthority.registerBouncyCastleProvider();
	}

	public static void main(String[] args)
	{
		certificateGenerator.generateCertificates();

		certificateGenerator.copyDockerTestSetupCertificates();

		CertificateFiles webbrowserTestUser = certificateGenerator.getClientCertificateFilesByCommonName()
				.get("Webbrowser Test User");
		logger.warn(
				"Install client-certificate and CA certificate from \"{}\" into your browsers certificate store to access fhir and bpe servers with your webbrowser",
				webbrowserTestUser.getP12KeyStoreFile().toAbsolutePath().toString());

		bundleGenerator.createDockerTestBundles(certificateGenerator.getClientCertificateFilesByCommonName());
		bundleGenerator.copyDockerTestBundles();

		configGenerator
				.modifyDockerTestFhirConfigProperties(certificateGenerator.getClientCertificateFilesByCommonName());
		configGenerator.copyDockerTestFhirConfigProperties();

		rsaKeyPairGenerator.createRsaKeyPair();
		rsaKeyPairGenerator.copyDockerTestRsaKeyPair();
	}
}
