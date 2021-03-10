package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface CrrKeyProvider
{
	PrivateKey getPrivateKey();

	PublicKey getPublicKey();
}
