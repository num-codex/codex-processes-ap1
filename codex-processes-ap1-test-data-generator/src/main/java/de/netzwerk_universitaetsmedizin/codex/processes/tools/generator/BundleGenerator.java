package de.netzwerk_universitaetsmedizin.codex.processes.tools.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.netzwerk_universitaetsmedizin.codex.processes.tools.generator.CertificateGenerator.CertificateFiles;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceExtractorImpl;

public class BundleGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(BundleGenerator.class);

	private final FhirContext fhirContext = FhirContext.forR4();
	private final ReferenceExtractor extractor = new ReferenceExtractorImpl();
	private final ReferenceCleaner cleaner = new ReferenceCleanerImpl(extractor);

	private Bundle bundle;

	private Bundle readAndCleanBundle(Path bundleTemplateFile)
	{
		try (InputStream in = Files.newInputStream(bundleTemplateFile))
		{
			Bundle bundle = newXmlParser().parseResource(Bundle.class, in);

			// FIXME hapi parser can't handle embedded resources and creates them while parsing bundles
			return cleaner.cleanReferenceResourcesIfBundle(bundle);
		}
		catch (IOException e)
		{
			logger.error("Error while reading bundle from " + bundleTemplateFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	private void writeBundle(Path bundleFile, Bundle bundle)
	{
		try (OutputStream out = Files.newOutputStream(bundleFile);
				OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
		{
			newXmlParser().encodeResourceToWriter(bundle, writer);
		}
		catch (IOException e)
		{
			logger.error("Error while writing bundle to " + bundleFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	private IParser newXmlParser()
	{
		IParser parser = fhirContext.newXmlParser();
		parser.setStripVersionsFromReferences(false);
		parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
		parser.setPrettyPrint(true);
		return parser;
	}

	public void createDockerTestBundles(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		createDockerTestBundle(clientCertificateFilesByCommonName);
	}

	private void createDockerTestBundle(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		Path medic3BundleTemplateFile = Paths.get("src/main/resources/bundle-templates/bundle.xml");

		bundle = readAndCleanBundle(medic3BundleTemplateFile);

		Organization organizationDts = (Organization) bundle.getEntry().get(0).getResource();
		Extension organizationDtsThumbprintExtension = organizationDts
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint");
		organizationDtsThumbprintExtension.setValue(new StringType(
				clientCertificateFilesByCommonName.get("dts-client").getCertificateSha512ThumbprintHex()));

		Organization organizationDic = (Organization) bundle.getEntry().get(1).getResource();
		Extension organizationDicthumbprintExtension = organizationDic
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint");
		organizationDicthumbprintExtension.setValue(new StringType(
				clientCertificateFilesByCommonName.get("dic-client").getCertificateSha512ThumbprintHex()));

		Organization organizationCrr = (Organization) bundle.getEntry().get(2).getResource();
		Extension organizationCrrthumbprintExtension = organizationCrr
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint");
		organizationCrrthumbprintExtension.setValue(new StringType(
				clientCertificateFilesByCommonName.get("crr-client").getCertificateSha512ThumbprintHex()));

		writeBundle(Paths.get("bundle/bundle.xml"), bundle);
	}

	public void copyDockerTestBundles()
	{
		Path crrBundleFile = Paths.get("../codex-processes-ap1-docker-test-setup/crr/fhir/conf/bundle.xml");
		logger.info("Copying fhir bundle to {}", crrBundleFile);
		writeBundle(crrBundleFile, bundle);

		Path dicBundleFile = Paths.get("../codex-processes-ap1-docker-test-setup/dic/fhir/conf/bundle.xml");
		logger.info("Copying fhir bundle to {}", dicBundleFile);
		writeBundle(dicBundleFile, bundle);

		Path gthBundleFile = Paths.get("../codex-processes-ap1-docker-test-setup/dts/fhir/conf/bundle.xml");
		logger.info("Copying fhir bundle to {}", gthBundleFile);
		writeBundle(gthBundleFile, bundle);

	}
}
