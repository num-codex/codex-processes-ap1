package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

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
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

public class FttpClientFactory
{
	private static final class FttpClientStub implements FttpClient
	{
		private static final Logger logger = LoggerFactory.getLogger(FttpClientStub.class);

		@Override
		public Optional<String> getCrrPseudonym(String dicPseudonym)
		{
			logger.warn("Using SHA-256 hash of DIC pseudonym to simulate CRR pseudonym");

			try
			{
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] sha256Hash = digest.digest(dicPseudonym.getBytes(StandardCharsets.UTF_8));
				return Optional.of(Base64.getEncoder().encodeToString(sha256Hash));
			}
			catch (NoSuchAlgorithmException e)
			{
				logger.error("Error while creating CRR pseudonym");
				return Optional.empty();
			}
		}
	}

	private final FhirContext fhirContext;
	private final Path trustStorePath;
	private final Path certificatePath;
	private final Path privateKeyPath;
	private final String fttpServerBase;
	private final String fttpApiKey;
	private final String fttpStudy;
	private final String fttpTarget;

	public FttpClientFactory(FhirContext fhirContext, Path trustStorePath, Path certificatePath, Path privateKeyPath,
			String fttpServerBase, String fttpApiKey, String fttpStudy, String fttpTarget)
	{
		if (fhirContext != null)
			this.fhirContext = fhirContext;
		else
			this.fhirContext = FhirContext.forR4();

		this.trustStorePath = trustStorePath;
		this.certificatePath = certificatePath;
		this.privateKeyPath = privateKeyPath;

		this.fttpServerBase = fttpServerBase;
		this.fttpApiKey = fttpApiKey;
		this.fttpStudy = fttpStudy;
		this.fttpTarget = fttpTarget;
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
		KeyStore trustStore = readTrustStore(trustStorePath);
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = readKeyStore(certificatePath, privateKeyPath, keyStorePassword);

		return new FttpClientImpl(fhirContext, trustStore, keyStore, keyStorePassword, fttpServerBase, fttpApiKey,
				fttpStudy, fttpTarget);
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