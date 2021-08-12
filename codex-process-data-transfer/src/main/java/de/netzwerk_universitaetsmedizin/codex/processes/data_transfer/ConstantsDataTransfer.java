package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition.VERSION;

public interface ConstantsDataTransfer
{
	String BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE_LIST = "patientReferenceList";
	String BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE = "patientReference";
	String BPMN_EXECUTION_VARIABLE_PSEUDONYM = "pseudonym";
	String BPMN_EXECUTION_VARIABLE_STOP_TIMER = "stopTimer";
	String BPMN_EXECUTION_VARIABLE_LAST_EXPORT_TO = "lastExportTo";
	String BPMN_EXECUTION_VARIABLE_EXPORT_FROM = "exportFrom";
	String BPMN_EXECUTION_VARIABLE_EXPORT_FROM_PRECISION = "exportFromPrecision";
	String BPMN_EXECUTION_VARIABLE_EXPORT_TO = "exportTo";
	String BPMN_EXECUTION_VARIABLE_BUNDLE = "bundle";
	String BPMN_EXECUTION_VARIABLE_IDAT_MERGE_GRANTED = "idatMergeGranted";
	String BPMN_EXECUTION_VARIABLE_USAGE_AND_TRANSFER_GRANTED = "usageAndTransferGranted";
	String BPMN_EXECUTION_VARIABLE_BINARY_URL = "binaryUrl";

	String NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM = "http://www.netzwerk-universitaetsmedizin.de/sid/dic-pseudonym";
	String NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM = "http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym";
	String NAMING_SYSTEM_NUM_CODEX_BLOOM_FILTER = "http://www.netzwerk-universitaetsmedizin.de/sid/bloom-filter";

	String RFC_4122_SYSTEM = "urn:ietf:rfc:4122";

	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER = "http://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/data-transfer";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PATIENT = "patient";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM = "pseudonym";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_FROM = "export-from";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO = "export-to";
	String CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_DATA_REFERENCE = "data-reference";

	// TODO: the following three constants should be moved into ConstantsBase of dsf-bpe-process-base
	String NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_NUM_CODEX_CONSORTIUM = "netzwerk-universitaetsmedizin.de";
	String CODESYSTEM_HIGHMED_ORGANIZATION_TYPE_VALUE_DTS = "DTS";
	String CODESYSTEM_HIGHMED_ORGANIZATION_TYPE_VALUE_CRR = "CRR";

	String PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER = "http://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-trigger";
	String PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER = "http://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-stop-data-trigger";
	String PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI = "http://highmed.org/bpe/Process/dataTrigger/";
	String PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_TRIGGER_PROCESS_URI
			+ VERSION;
	String PROFILE_NUM_CODEX_TASK_START_DATA_TRIGGER_MESSAGE_NAME = "startDataTrigger";
	String PROFILE_NUM_CODEX_TASK_STOP_DATA_TRIGGER_MESSAGE_NAME = "stopDataTrigger";

	String PROFILE_NUM_CODEX_TASK_START_DATA_SEND = "http://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-send";
	String PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI = "http://highmed.org/bpe/Process/dataSend/";
	String PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_SEND_PROCESS_URI
			+ VERSION;
	String PROFILE_NUM_CODEX_TASK_START_DATA_SEND_MESSAGE_NAME = "startDataSend";

	String PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE = "http://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-translate";
	String PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI = "http://highmed.org/bpe/Process/dataTranslate/";
	String PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI_AND_LATEST_VERSION = PROFILE_NUM_CODEX_TASK_DATA_TRANSLATE_PROCESS_URI
			+ VERSION;
	String PROFILE_NUM_CODEX_TASK_START_DATA_TRANSLATE_MESSAGE_NAME = "startDataTranslate";

	String PROFILE_NUM_CODEX_TASK_START_DATA_RECEIVE = "http://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/task-start-data-receive";
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
