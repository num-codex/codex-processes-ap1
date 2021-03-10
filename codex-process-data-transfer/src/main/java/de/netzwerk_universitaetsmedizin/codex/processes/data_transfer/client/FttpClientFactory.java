package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FttpClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(FttpClientFactory.class);

	public FttpClient getFttpClient()
	{
		return new FttpClient()
		{
			@Override
			public String getCrrPseudonym(String dicPseudonym)
			{
				logger.warn("Using SHA-256 hash of DIC pseudonym to simulate CRR pseudonym");

				try
				{
					MessageDigest digest = MessageDigest.getInstance("SHA-256");
					byte[] sha256Hash = digest.digest(dicPseudonym.getBytes(StandardCharsets.UTF_8));
					return Base64.getEncoder().encodeToString(sha256Hash);
				}
				catch (NoSuchAlgorithmException e)
				{
					throw new RuntimeException(e);
				}
			}
		};
	}
}
