package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ProcessPluginDeploymentConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ReceiveConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.SendConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TransferDataConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TransferDataSerializerConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TranslateConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.TriggerConfig;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ValidationConfig;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

public class DataTransferProcessPluginDefinition implements ProcessPluginDefinition
{
	public static final String VERSION = "1.1.0.0";
	public static final LocalDate DATE = LocalDate.of(2024, 3, 18);

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
	public List<String> getProcessModels()
	{
		return List.of("bpe/trigger.bpmn", "bpe/send.bpmn", "bpe/translate.bpmn", "bpe/receive.bpmn");
	}

	@Override
	public List<Class<?>> getSpringConfigurations()
	{
		return List.of(TransferDataConfig.class, TransferDataSerializerConfig.class, ValidationConfig.class,
				TriggerConfig.class, SendConfig.class, TranslateConfig.class, ReceiveConfig.class,
				ProcessPluginDeploymentConfig.class);
	}

	@Override
	public Map<String, List<String>> getFhirResourcesByProcessId()
	{
		var aTri = "fhir/ActivityDefinition/data-trigger.xml";
		var aSen = "fhir/ActivityDefinition/data-send.xml";
		var aTra = "fhir/ActivityDefinition/data-translate.xml";
		var aRec = "fhir/ActivityDefinition/data-receive.xml";

		var cD = "fhir/CodeSystem/data-transfer.xml";
		var cDeS = "fhir/CodeSystem/data-transfer-error-source.xml";
		var cDe = "fhir/CodeSystem/data-transfer-error.xml";

		var nD = "fhir/NamingSystem/dic-pseudonym-identifier.xml";
		var nC = "fhir/NamingSystem/crr-pseudonym-identifier.xml";
		var nB = "fhir/NamingSystem/bloom-filter-identifier.xml";

		var sTexErMe = "fhir/StructureDefinition/extension-error-metadata.xml";
		var sTstaDtri = "fhir/StructureDefinition/task-start-data-trigger.xml";
		var sTstoDtri = "fhir/StructureDefinition/task-stop-data-trigger.xml";
		var sTstaDsen = "fhir/StructureDefinition/task-start-data-send.xml";
		var sTconDsen = "fhir/StructureDefinition/task-continue-data-send.xml";
		var sTconDsenWvE = "fhir/StructureDefinition/task-continue-data-send-with-validation-error.xml";
		var sTconDsenWe = "fhir/StructureDefinition/task-continue-data-send-with-error.xml";
		var sTstaDtra = "fhir/StructureDefinition/task-start-data-translate.xml";
		var sTconDtra = "fhir/StructureDefinition/task-continue-data-translate.xml";
		var sTconDtraWvE = "fhir/StructureDefinition/task-continue-data-translate-with-validation-error.xml";
		var sTconDtraWe = "fhir/StructureDefinition/task-continue-data-translate-with-error.xml";
		var sTstaDrec = "fhir/StructureDefinition/task-start-data-receive.xml";

		var tStartSendAbsoluteReference = "fhir/Task/TaskStartDataSendWithAbsoluteReference.xml";
		var tStartSendIdentifierReference = "fhir/Task/TaskStartDataSendWithIdentifierReference.xml";
		var tStartTrigger = "fhir/Task/TaskStartDataTrigger.xml";
		var tStopTrigger = "fhir/Task/TaskStopDataTrigger.xml";

		var vD = "fhir/ValueSet/data-transfer.xml";
		var vDeS = "fhir/ValueSet/data-transfer-error-source.xml";
		var vDe = "fhir/ValueSet/data-transfer-error.xml";

		return Map.of("wwwnetzwerk-universitaetsmedizinde_dataTrigger",
				Arrays.asList(aTri, cD, nD, sTstaDtri, sTstoDtri, tStartTrigger, tStopTrigger, vD),
				"wwwnetzwerk-universitaetsmedizinde_dataSend",
				Arrays.asList(aSen, cD, cDeS, cDe, nB, nD, sTexErMe, sTstaDsen, sTconDsen, sTconDsenWvE, sTconDsenWe,
						tStartSendAbsoluteReference, tStartSendIdentifierReference, vD, vDeS, vDe),
				"wwwnetzwerk-universitaetsmedizinde_dataTranslate",
				Arrays.asList(aTra, cD, cDeS, cDe, nD, nC, sTexErMe, sTstaDtra, sTconDtra, sTconDtraWvE, sTconDtraWe,
						vD),
				"wwwnetzwerk-universitaetsmedizinde_dataReceive",
				Arrays.asList(aRec, cD, cDeS, cDe, nC, sTexErMe, sTstaDrec, vD));
	}
}
