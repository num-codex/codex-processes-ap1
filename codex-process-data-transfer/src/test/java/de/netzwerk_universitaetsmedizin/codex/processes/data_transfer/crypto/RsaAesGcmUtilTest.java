package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RsaAesGcmUtilTest
{
	private static final Logger logger = LoggerFactory.getLogger(RsaAesGcmUtilTest.class);
	
	@Test
	public void testEncryptDecypt() throws Exception
	{
		logger.debug("Generating 4096 Bit RSA Key ...");
		KeyPair keyPair = RsaAesGcmUtil.generateRsa4096KeyPair();

		String text = "Hello, World";

		byte[] encrypted = RsaAesGcmUtil.encrypt(keyPair.getPublic(), text.getBytes(StandardCharsets.UTF_8));
		byte[] decrypted = RsaAesGcmUtil.decrypt(keyPair.getPrivate(), encrypted);

		assertEquals(text, new String(decrypted, StandardCharsets.UTF_8));
	}
}
