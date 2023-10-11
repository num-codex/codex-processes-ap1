package de.netzwerk_universitaetsmedizin.codex.processes.tools.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition;
import de.netzwerk_universitaetsmedizin.codex.processes.tools.generator.CertificateGenerator.CertificateFiles;

public class EnvGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(EnvGenerator.class);

	private static final String PROCESS_VERSION = "PROCESS_VERSION";
	private static final String WEBBROSER_TEST_USER_THUMBPRINT = "WEBBROSER_TEST_USER_THUMBPRINT";

	private static final class EnvEntry
	{
		final String userThumbprintVariableName;
		final String userThumbprint;

		EnvEntry(String userThumbprintVariableName, String userThumbprint)
		{
			this.userThumbprintVariableName = userThumbprintVariableName;
			this.userThumbprint = userThumbprint;
		}
	}

	public void generateAndWriteDockerTestFhirEnvFiles(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		String webbroserTestUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"Webbrowser Test User").findFirst().get();

		List<EnvEntry> entries = List.of(new EnvEntry(WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint));

		Map<String, String> additionalEntries = Map.of(PROCESS_VERSION,
				new DataTransferProcessPluginDefinition().getResourceVersion());

		writeEnvFile(Paths.get("../codex-processes-ap1-docker-test-setup/.env"), entries, additionalEntries);
	}

	private Stream<String> filterAndMapToThumbprint(Map<String, CertificateFiles> clientCertificateFilesByCommonName,
			String... commonNames)
	{
		return clientCertificateFilesByCommonName.entrySet().stream()
				.filter(entry -> Arrays.asList(commonNames).contains(entry.getKey()))
				.sorted(Comparator.comparing(e -> Arrays.asList(commonNames).indexOf(e.getKey()))).map(Entry::getValue)
				.map(CertificateFiles::getCertificateSha512ThumbprintHex);
	}

	private void writeEnvFile(Path target, List<? extends EnvEntry> entries, Map<String, String> additionalEntries)
	{
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < entries.size(); i++)
		{
			EnvEntry entry = entries.get(i);

			builder.append(entry.userThumbprintVariableName);
			builder.append('=');
			builder.append(entry.userThumbprint);

			if ((i + 1) < entries.size())
				builder.append("\n");
		}

		if (!additionalEntries.isEmpty())
			builder.append('\n');

		for (Entry<String, String> entry : additionalEntries.entrySet())
		{
			builder.append('\n');
			builder.append(entry.getKey());
			builder.append('=');
			builder.append(entry.getValue());
		}

		try
		{
			logger.info("Writing .env file to {}", target.toString());
			Files.writeString(target, builder.toString());
		}
		catch (IOException e)
		{
			logger.error("Error while writing .env file to " + target.toString(), e);
			throw new RuntimeException(e);
		}
	}
}
