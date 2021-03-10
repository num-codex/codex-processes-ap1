package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import de.rwh.utils.crypto.io.PemIo;

public class CrrKeyProviderImpl implements CrrKeyProvider
{
	// openssl genrsa -out keypair.pem 4096
	// openssl rsa -in keypair.pem -pubout -out publickey.crt
	// openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key

	/**
	 * on of the parameters should be <code>null</code> the other not <code>null</code>
	 * 
	 * @param crrPrivateKeyFile
	 * @param crrPublicKeyFile
	 * @return
	 */
	public static CrrKeyProviderImpl fromFiles(String crrPrivateKeyFile, String crrPublicKeyFile)
	{
		if ((crrPrivateKeyFile == null && crrPublicKeyFile == null)
				|| (crrPrivateKeyFile != null && crrPublicKeyFile != null))
		{
			throw new RuntimeException("Either CRR public key or CRR private key must be set, not both");
		}

		RSAPrivateCrtKey crrPrivateKey = null;
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
		catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e)
		{
			throw new RuntimeException("Error while reading CRR private-key from " + crrPrivateKeyFile);
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
			throw new RuntimeException("Error while reading CRR public-key from " + crrPublicKeyFile);
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
