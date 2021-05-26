package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition.VERSION;

public interface ConstantsDataTransfer
{
	String BPMN_EXECUTION_VARIABLE_PSEUDONYMS_LIST = "pseudonyms";
	String BPMN_EXECUTION_VARIABLE_PSEUDONYM = "pseudonym";
	String BPMN_EXECUTION_VARIABLE_STOP_TIMER = "stopTimer";
	String BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO = "lastExportTo";
	String BPMN_EXECUTION_VARIABLE_EXPORT_FROM = "exportFrom";
	String BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION = "exportFromPrecision";
	String BPMN_EXECUTION_VARIABLE_EXPORT_TO = "exportTo";
	String BPMN_EXECUTION_VARIABLE_BUNDLE = "bundle";
	String BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED = "usageAndTransferGranted";
	String BPMN_EXECUTION_VARIABLE_BINARY_URL = "binaryUrl";

	String NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM = "http://netzwerk-universitaetsmedizin.de/fhir/NamingSystem/dic-pseudonym";
	String NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM = "http://netzwerk-universitaetsmedizin.de/fhir/NamingSystem/crr-pseudonym";
	String NAMING_SYSTEM_NUM_CODEX_RECORD_BLOOM_FILTER = "http://netzwerk-universitaetsmedizin.de/fhir/NamingSystem/record-bloom-filter";

	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER = "http://netzwerk-universitaetsmedizin.de/fhir/CodeSystem/data-transfer";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM = "pseudonym";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_RECORD_BLOOM_FILTER = "record-bloom-filter";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM = "export-from";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO = "export-to";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE = "data-reference";

	String PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER = "http://netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-trigger";
	String PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER = "http://netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-stop-data-trigger";
	String PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI = "http://highmed.org/bpe/Process/dataTrigger/";
	String PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI
			+ VERSION;
	String PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER_MESSAGE_NAME = "startDataTrigger";
	String PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER_MESSAGE_NAME = "stopDataTrigger";

	String PROFILE_NUM_CODEX_TASK_START_DATA_SEND = "http://netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-send";
	String PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI = "http://highmed.org/bpe/Process/dataSend/";
	String PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI
			+ VERSION;
	String PROFILE_NUM_CODEX_TASK_START_DATA_SEND_MESSAGE_NAME = "startDataSend";

	String PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE = "http://netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-translate";
	String PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI = "http://highmed.org/bpe/Process/dataTranslate/";
	String PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI
			+ VERSION;
	String PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE_MESSAGE_NAME = "startDataTranslate";

	String PROFILE_NUM_CODEX_TASK_START_DATA_RECEIVE = "http://netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-receive";
	String PROFILE_NUM_CODEX_TASK_DATA_RECEIVE_PROCESS_URI = "http://highmed.org/bpe/Process/dataReceive/";
	String PROFILE_NUM_CODEX_TASK_DATA_RECEIVE_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_RECEIVE_PROCESS_URI
			+ VERSION;
	String PROFILE_NUM_CODEX_TASK_START_DATA_RECEIVE_MESSAGE_NAME = "startDataReceive";

	String PSEUDONYM_PLACEHOLDER = "${pseudonym}";
	/**
	 * dic-source/dic-pseudonym-original
	 */
	String PSEUDONYM_PATTERN_STRING = "(?<source>[^/]+)/(?<original>[^/]+)";
}
