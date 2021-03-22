package de.netzwerk_universitaetsmedizin.codex.processes.tools.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.PemIo;

public class RsaKeyPairGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(RsaKeyPairGenerator.class);

	private KeyPair pair;

	public void createRsaKeyPair()
	{
		Path crrPrivateKeyFile = Paths.get("rsa/crr_private-key.pem");
		Path crrPublicKeyFile = Paths.get("rsa/crr_public-key.pem");

		if (Files.isReadable(crrPrivateKeyFile) && Files.isReadable(crrPublicKeyFile))
		{
			try
			{
				logger.info("Reading CRR private-key from {}", crrPrivateKeyFile.toString());
				RSAPrivateCrtKey privateKey = PemIo.readPrivateKeyFromPem(crrPrivateKeyFile);

				logger.info("Reading CRR public-key from {}", crrPublicKeyFile.toString());
				RSAPublicKey publicKey = PemIo.readPublicKeyFromPem(crrPublicKeyFile);

				pair = new KeyPair(publicKey, privateKey);
			}
			catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e)
			{
				logger.error("Error while reading rsa key-pair from " + crrPrivateKeyFile.toString() + " and "
						+ crrPublicKeyFile.toString(), e);
				throw new RuntimeException(e);
			}
		}
		else
		{
			try
			{
				logger.info("Generating 4096 Bit RSA key-pair");
				pair = CertificateHelper.createRsaKeyPair4096Bit();

				logger.info("Writing CRR private-key to {}", crrPrivateKeyFile.toString());
				PemIo.writePrivateKeyToPem((RSAPrivateCrtKey) pair.getPrivate(), crrPrivateKeyFile);

				logger.info("Writing CRR public-key to {}", crrPublicKeyFile.toString());
				PemIo.writePublicKeyToPem((RSAPublicKey) pair.getPublic(), crrPublicKeyFile);
			}
			catch (NoSuchAlgorithmException | IOException e)
			{
				logger.error("Error while creating or writing rsa key-pair to " + crrPrivateKeyFile.toString() + " and "
						+ crrPublicKeyFile.toString(), e);
				throw new RuntimeException(e);
			}
		}
	}

	public void copyDockerTestRsaKeyPair()
	{
		Path crrPrivateKeyFile = Paths
				.get("../codex-processes-ap1-docker-test-setup/crr/bpe/app/conf/crr_private-key.pem");

		Path crrPublicKeyFile = Paths
				.get("../codex-processes-ap1-docker-test-setup/crr/bpe/app/conf/crr_public-key.pem");

		Path dicCrrPublicKeyFile = Paths
				.get("../codex-processes-ap1-docker-test-setup/dic/bpe/app/conf/crr_public-key.pem");

		try
		{
			logger.info("Copying crr private-key to {}", crrPrivateKeyFile.toString());
			PemIo.writePrivateKeyToPem((RSAPrivateCrtKey) pair.getPrivate(), crrPrivateKeyFile);

			logger.info("Copying crr public-key to {}", crrPublicKeyFile.toString());
			PemIo.writePublicKeyToPem((RSAPublicKey) pair.getPublic(), crrPublicKeyFile);

			logger.info("Copying crr public-key to {}", dicCrrPublicKeyFile.toString());
			PemIo.writePublicKeyToPem((RSAPublicKey) pair.getPublic(), dicCrrPublicKeyFile);
		}
		catch (IOException e)
		{
			logger.error("Error copying key-pair", e);
			throw new RuntimeException(e);
		}
	}
}
