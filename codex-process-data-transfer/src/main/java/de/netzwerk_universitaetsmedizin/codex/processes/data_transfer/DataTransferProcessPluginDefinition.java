package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer;

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
import org.springframework.core.env.PropertyResolver;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TransferDataConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TransferDataSerializerConfig;

public class DataTransferProcessPluginDefinition implements ProcessPluginDefinition
{
	public static final String VERSION = "0.4.1";

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
	public Stream<String> getBpmnFiles()
	{
		return Stream.of("bpe/trigger.bpmn", "bpe/send.bpmn", "bpe/translate.bpmn", "bpe/receive.bpmn");
	}

	@Override
	public Stream<Class<?>> getSpringConfigClasses()
	{
		return Stream.of(TransferDataConfig.class, TransferDataSerializerConfig.class);
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

		var nD = NamingSystemResource.file("fhir/NamingSystem/num-codex-dic-pseudonym-identifier.xml");
		var nC = NamingSystemResource.file("fhir/NamingSystem/num-codex-crr-pseudonym-identifier.xml");
		var nB = NamingSystemResource.file("fhir/NamingSystem/num-codex-bloom-filter-identifier.xml");

		var sTstaDtri = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-start-data-trigger.xml");
		var sTstoDtri = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-stop-data-trigger.xml");
		var sTstaDsen = StructureDefinitionResource.file("fhir/StructureDefinition/num-codex-task-start-data-send.xml");
		var sTstaDtra = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-start-data-translate.xml");
		var sTstaDrec = StructureDefinitionResource
				.file("fhir/StructureDefinition/num-codex-task-start-data-receive.xml");

		var vD = ValueSetResource.file("fhir/ValueSet/num-codex-data-transfer.xml");

		Map<String, List<AbstractResource>> resourcesByProcessKeyAndVersion = Map.of( //
				"wwwnetzwerk-universitaetsmedizinde_dataTrigger/" + VERSION,
				Arrays.asList(aTri, cD, nD, sTstaDtri, sTstoDtri, vD), //
				"wwwnetzwerk-universitaetsmedizinde_dataSend/" + VERSION,
				Arrays.asList(aSen, cD, nD, nB, sTstaDsen, vD), //
				"wwwnetzwerk-universitaetsmedizinde_dataTranslate/" + VERSION,
				Arrays.asList(aTra, cD, nD, nC, sTstaDtra, vD), //
				"wwwnetzwerk-universitaetsmedizinde_dataReceive/" + VERSION,
				Arrays.asList(aRec, cD, nC, sTstaDrec, vD));

		return ResourceProvider.read(VERSION, () -> fhirContext.newXmlParser().setStripVersionsFromReferences(false),
				classLoader, propertyResolver, resourcesByProcessKeyAndVersion);
	}
}
