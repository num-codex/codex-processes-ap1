package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition.VERSION;

public interface ConstantsDataTransfer
{
	String BPMN_EXECUTION_VARIABLE_PSEUDONYMS_LIST = "pseudonyms";
	String BPMN_EXECUTION_VARIABLE_PSEUDONYM = "pseudonym";
	String BPMN_EXECUTION_VARIABLE_STOP_TIMER = "stopTimer";
	String BPMN_EXECUTION_VARIABLE_EXPORT_FROM = "exportFrom";
	String BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION = "exportFromPrecision";
	String BPMN_EXECUTION_VARIABLE_EXPORT_TO = "exportTo";
	String BPMN_EXECUTION_VARIABLE_BUNDLE = "bundle";
	String BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED = "usageAndTransferGranted";
	String BPMN_EXECUTION_VARIABLE_BINARY_URL = "binaryUrl";

	String NAMING_SYSTEM_NUM_CODEX_DIZ_PSEUDONYM = "http://netzwerk-universitaetsmedizin.de/fhir/NamingSystem/dic-pseudonym";
	String NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM = "http://netzwerk-universitaetsmedizin.de/fhir/NamingSystem/crr-pseudonym";

	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER = "http://netzwerk-universitaetsmedizin.de/fhir/CodeSystem/data-transfer";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM = "pseudonym";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM = "export-form";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO = "export-to";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE = "data-reference";

	String PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER = "http://netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-trigger";
	String PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER = "http://netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-stop-data-trigger";
	String PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI = "http://netzwerk-universitaetsmedizin.de/bpe/Process/dataTrigger/";
	String PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI
			+ VERSION;

	String PSEUDONYM_PLACEHOLDER = "${pseudonym}";
}
