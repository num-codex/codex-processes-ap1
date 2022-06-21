package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.AbstractResource;
import org.highmed.dsf.fhir.resources.ActivityDefinitionResource;
import org.highmed.dsf.fhir.resources.CodeSystemResource;
import org.highmed.dsf.fhir.resources.NamingSystemResource;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.highmed.dsf.fhir.resources.StructureDefinitionResource;
import org.highmed.dsf.fhir.resources.ValueSetResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertyResolver;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ReceiveConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.SendConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TransferDataConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TransferDataSerializerConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TranslateConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TriggerConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ValidationConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ValidationConfig.TerminologyServerConnectionTestStatus;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidatorFactory;

public class DataTransferProcessPluginDefinition implements ProcessPluginDefinition
{
	private static final Logger logger = LoggerFactory.getLogger(DataTransferProcessPluginDefinition.class);

	public static final String VERSION = "0.5.0";
	public static final LocalDate DATE = LocalDate.of(2022, 6, 21);

	@Override
	public String getName()
	{
		return "codex-process-data-transfer";
	}

	@Override
	public String getVersion()
	{
		return VERSION;
	}

	@Override
	public LocalDate getReleaseDate()
	{
		return DATE;
	}

	@Override
	public Stream<String> getBpmnFiles()
	{
		return Stream.of("bpe/trigger.bpmn", "bpe/send.bpmn", "bpe/translate.bpmn", "bpe/receive.bpmn");
	}

	@Override
	public Stream<Class<?>> getSpringConfigClasses()
	{
		return Stream.of(TransferDataConfig.class, TransferDataSerializerConfig.class, ValidationConfig.class,
				TriggerConfig.class, SendConfig.class, TranslateConfig.class, ReceiveConfig.class);
	}

	@Override
	public ResourceProvider getResourceProvider(FhirContext fhirContext, ClassLoader classLoader,
			PropertyResolver propertyResolver)
	{
		var aTri = ActivityDefinitionResource.file("fhir/ActivityDefinition/num-codex-data-trigger.xml");
		var aSen = ActivityDefinitionResource.file("fhir/ActivityDefinition/num-codex-data-send.xml");
		var aTra = ActivityDefinitionResource.file("fhir/ActivityDefinition/num-codex-data-translate.xml");
		var aRec = ActivityDefinitionResource.file("fhir/ActivityDefinition/num-codex-data-receive.xml");

		var cD = CodeSystemResource.file("fhir/CodeSystem/num-codex-data-transfer.xml");
		var cDeS = CodeSystemResource.file("fhir/CodeSystem/num-codex-data-transfer-error-source.xml");
		var cDe = CodeSystemResource.file("fhir/CodeSystem/num-codex-data-transfer-error.xml");

		var nD = NamingSystemResource.file("fhir/NamingSystem/num-codex-dic-pseudonym-identifier.xml");
		var nC = NamingSystemResource.file("fhir/NamingSystem/num-codex-crr-pseudonym-identifier.xml");
		var nB = NamingSystemResource.file("fhir/NamingSystem/num-codex-bloom-filter-identifier.xml");

		var sTexErMe = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-extension-error-metadata.xml");
		var sTstaDtri = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-start-data-trigger.xml");
		var sTstoDtri = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-stop-data-trigger.xml");
		var sTstaDsen = StructureDefinitionResource.file("fhir/StructureDefinition/num-codex-task-start-data-send.xml");
		var sTconDsen = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-continue-data-send.xml");
		var sTconDsenWvE = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-continue-data-send-with-validation-error.xml");
		var sTconDsenWe = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-continue-data-send-with-error.xml");
		var sTstaDtra = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-start-data-translate.xml");
		var sTconDtra = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-continue-data-translate.xml");
		var sTconDtraWvE = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-continue-data-translate-with-validation-error.xml");
		var sTconDtraWe = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-continue-data-translate-with-error.xml");
		var sTstaDrec = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-start-data-receive.xml");

		var vD = ValueSetResource.file("fhir/ValueSet/num-codex-data-transfer.xml");
		var vDeS = ValueSetResource.file("fhir/ValueSet/num-codex-data-transfer-error-source.xml");
		var vDe = ValueSetResource.file("fhir/ValueSet/num-codex-data-transfer-error.xml");

		Map<String, List<AbstractResource>> resourcesByProcessKeyAndVersion = Map.of(
				"wwwnetzwerk-universitaetsmedizinde_dataTrigger/" + VERSION,
				Arrays.asList(aTri, cD, nD, sTstaDtri, sTstoDtri, vD),
				"wwwnetzwerk-universitaetsmedizinde_dataSend/" + VERSION, Arrays.asList(aSen, cD, cDeS, cDe, nB, nD,
						sTexErMe, sTstaDsen, sTconDsen, sTconDsenWvE, sTconDsenWe, vD, vDeS, vDe),
				"wwwnetzwerk-universitaetsmedizinde_dataTranslate/" + VERSION,
				Arrays.asList(aTra, cD, cDeS, cDe, nD, nC, sTexErMe, sTstaDtra, sTconDtra, sTconDtraWvE, sTconDtraWe,
						vD),
				"wwwnetzwerk-universitaetsmedizinde_dataReceive/" + VERSION,
				Arrays.asList(aRec, cD, cDeS, cDe, nC, sTexErMe, sTstaDrec, vD));

		return ResourceProvider.read(VERSION, DATE,
				() -> fhirContext.newXmlParser().setStripVersionsFromReferences(false), classLoader, propertyResolver,
				resourcesByProcessKeyAndVersion);
	}

	@Override
	public void onProcessesDeployed(ApplicationContext pluginApplicationContext, List<String> activeProcesses)
	{
		if (activeProcesses.contains("wwwnetzwerk-universitaetsmedizinde_dataSend")
				|| activeProcesses.contains("wwwnetzwerk-universitaetsmedizinde_dataReceive"))
		{
			pluginApplicationContext.getBean(GeccoClientFactory.class).testConnection();
		}

		if (activeProcesses.contains("wwwnetzwerk-universitaetsmedizinde_dataSend")
				|| activeProcesses.contains("wwwnetzwerk-universitaetsmedizinde_dataTranslate"))
		{
			pluginApplicationContext.getBean(FttpClientFactory.class).testConnection();
		}

		if (activeProcesses.contains("wwwnetzwerk-universitaetsmedizinde_dataSend"))
		{
			TerminologyServerConnectionTestStatus status = pluginApplicationContext.getBean(ValidationConfig.class)
					.testConnectionToTerminologyServer();

			if (TerminologyServerConnectionTestStatus.OK.equals(status))
				pluginApplicationContext.getBean(BundleValidatorFactory.class).init();
			else if (TerminologyServerConnectionTestStatus.NOT_OK.equals(status))
				logger.warn(
						"Due to an error while testing the connection to the terminology server {} was not initialized, validation of bundles will be skipped.",
						BundleValidatorFactory.class.getSimpleName());
		}
	}
}
