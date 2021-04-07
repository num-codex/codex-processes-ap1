package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.PSEUDONYM_PATTERN_STRING;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

public class FttpClientFactory implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FttpClientFactory.FttpClientStub.class);

	private static final class FttpClientStub implements FttpClient
	{
		private static final Logger logger = LoggerFactory.getLogger(FttpClientStub.class);

		private static final Pattern DIC_PSEUDONYM_PATTERN = Pattern.compile(PSEUDONYM_PATTERN_STRING);

		@Override
		public Optional<String> getCrrPseudonym(String dicSourceAndPseudonym)
		{
			logger.warn("Using SHA-256 hash of DIC pseudonym {} to simulate CRR pseudonym", dicSourceAndPseudonym);

			Matcher matcher = DIC_PSEUDONYM_PATTERN.matcher(dicSourceAndPseudonym);
			if (!matcher.matches())
				throw new IllegalArgumentException("DIC pseudonym not matching " + PSEUDONYM_PATTERN_STRING);

			String original = matcher.group(2);

			try
			{
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] sha256Hash = digest.digest(original.getBytes(StandardCharsets.UTF_8));
				return Optional.of(Hex.encodeHexString(sha256Hash));
			}
			catch (NoSuchAlgorithmException e)
			{
				logger.error("Error while creating CRR pseudonym");
				return Optional.empty();
			}
		}

		@Override
		public void testConnection()
		{
			logger.warn("Stub implementation, no connection test performed");
		}
	}

	private final Path trustStorePath;
	private final Path certificatePath;
	private final Path privateKeyPath;
	private final String fttpServerBase;
	private final String fttpApiKey;
	private final String fttpStudy;
	private final String fttpTarget;

	public FttpClientFactory(Path trustStorePath, Path certificatePath, Path privateKeyPath, String fttpServerBase,
			String fttpApiKey, String fttpStudy, String fttpTarget)
	{
		this.trustStorePath = trustStorePath;
		this.certificatePath = certificatePath;
		this.privateKeyPath = privateKeyPath;

		this.fttpServerBase = fttpServerBase;
		this.fttpApiKey = fttpApiKey;
		this.fttpStudy = fttpStudy;
		this.fttpTarget = fttpTarget;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		try
		{
			logger.info(
					"Testing connection to fTTP with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, fttpServerBase: {}, fttpApiKey: {}, fttpStudy: {}, fttpTarget: {}}",
					trustStorePath, certificatePath, privateKeyPath, fttpServerBase, fttpApiKey, fttpStudy, fttpTarget);

			getFttpClient().testConnection();
		}
		catch (Exception e)
		{
			logger.error("Error while testing connection to fTTP", e);
		}
	}

	public FttpClient getFttpClient()
	{
		if (configured())
			return createFttpClient();
		else
			return new FttpClientStub();
	}

	private boolean configured()
	{
		return trustStorePath != null && certificatePath != null && privateKeyPath != null && fttpServerBase != null
				&& !fttpServerBase.isBlank() && fttpApiKey != null && !fttpApiKey.isBlank() && fttpStudy != null
				&& !fttpStudy.isBlank() && fttpTarget != null && !fttpTarget.isBlank();
	}

	protected FttpClient createFttpClient()
	{
		logger.debug("Reading trust-store from {}", trustStorePath.toString());
		KeyStore trustStore = readTrustStore(trustStorePath);
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		logger.debug("Creating key-store from {} and {}", certificatePath.toString(), privateKeyPath.toString());
		KeyStore keyStore = readKeyStore(certificatePath, privateKeyPath, keyStorePassword);

		return new FttpClientImpl(trustStore, keyStore, keyStorePassword, fttpServerBase, fttpApiKey, fttpStudy,
				fttpTarget);
	}

	private KeyStore readTrustStore(Path trustPath)
	{
		try
		{
			return CertificateReader.allFromCer(trustPath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore readKeyStore(Path certificatePath, Path keyPath, char[] keyStorePassword)
	{
		try
		{
			RSAPrivateCrtKey privateKey = PemIo.readPrivateKeyFromPem(keyPath);
			X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);

			return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate },
					UUID.randomUUID().toString(), keyStorePassword);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | CertificateException | KeyStoreException
				| IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
