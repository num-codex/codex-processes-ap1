package de.netzwerk_universitaetsmedizin.codex.processes;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;

public class FttpTest
{
	public static void main(String[] args)
	{
		Path trustStorePath = Paths
				.get("C:/Users/hhund/repositories/num-codex_gth-test/bpe/app/conf/fttp-num-ca_certificate.pem");
		Path certificatePath = Paths
				.get("C:/Users/hhund/repositories/num-codex_gth-test/bpe/app/conf/fttp-num-client_certificate.pem");
		Path privateKeyPath = Paths
				.get("C:/Users/hhund/repositories/num-codex_gth-test/bpe/app/conf/fttp-num-client_private-key.pem");

		String fttpServerBase = "https://ip-test.ths.num.med.uni-greifswald.de/ttp-fhir/fhir";
		String fttpApiKey = "zpx9tlz07z0ebraw";
		String fttpStudy = "num";
		String fttpTarget = "codex";

		FttpClientFactory factory = new FttpClientFactory(trustStorePath, certificatePath, privateKeyPath, 10_000,
				10_000, 20_000, null, null, fttpServerBase, fttpApiKey, fttpStudy, fttpTarget, null, null, null, false);
		// factory.getFttpClient().testConnection();
		factory.getFttpClient().getCrrPseudonym("foo/bar");
	}
}