package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwh.utils.crypto.io.PemIo;

public class CrrKeyProviderImpl implements CrrKeyProvider
{
	private static final Logger logger = LoggerFactory.getLogger(CrrKeyProviderImpl.class);

	// openssl genrsa -out keypair.pem 4096
	// openssl rsa -in keypair.pem -pubout -out publickey.crt
	// openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key

	/**
	 * One or both parameters should be <code>null</code>
	 * 
	 * @param crrPrivateKeyFile
	 * @param crrPublicKeyFile
	 * @return
	 */
	public static CrrKeyProviderImpl fromFiles(String crrPrivateKeyFile, String crrPublicKeyFile)
	{
		logger.info("Configuring CrrKeyProvider with private-key from {} and public-key from {}", crrPrivateKeyFile,
				crrPublicKeyFile);

		if (crrPrivateKeyFile != null && crrPublicKeyFile != null)
		{
			throw new RuntimeException("Both CRR private-key (" + crrPrivateKeyFile + ") and CRR public-key ("
					+ crrPublicKeyFile + ") set");
		}

		PrivateKey crrPrivateKey = null;
		RSAPublicKey crrPublicKey = null;
		try
		{
			if (crrPrivateKeyFile != null)
			{
				Path crrPrivateKeyPath = Paths.get(crrPrivateKeyFile);
				if (!Files.isReadable(crrPrivateKeyPath))
					throw new RuntimeException("CRR public-key at " + crrPrivateKeyFile + " not readable");

				crrPrivateKey = PemIo.readPrivateKeyFromPem(crrPrivateKeyPath);
			}

		}
		catch (IOException | PKCSException e)
		{
			throw new RuntimeException("Error while reading CRR private-key from " + crrPrivateKeyFile, e);
		}

		try
		{
			if (crrPublicKeyFile != null)
			{
				Path crrPublicKeyPath = Paths.get(crrPublicKeyFile);
				if (!Files.isReadable(crrPublicKeyPath))
					throw new RuntimeException("CRR public-key at " + crrPublicKeyFile + " not readable");

				crrPublicKey = PemIo.readPublicKeyFromPem(crrPublicKeyPath);
			}
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e)
		{
			throw new RuntimeException("Error while reading CRR public-key from " + crrPublicKeyFile, e);
		}

		return new CrrKeyProviderImpl(crrPrivateKey, crrPublicKey);
	}

	private final PrivateKey crrPrivateKey;
	private final PublicKey crrPublicKey;

	public CrrKeyProviderImpl(PrivateKey crrPrivateKey, PublicKey crrPublicKey)
	{
		this.crrPrivateKey = crrPrivateKey;
		this.crrPublicKey = crrPublicKey;
	}

	@Override
	public PrivateKey getPrivateKey()
	{
		return crrPrivateKey;
	}

	@Override
	public PublicKey getPublicKey()
	{
		return crrPublicKey;
	}
}
