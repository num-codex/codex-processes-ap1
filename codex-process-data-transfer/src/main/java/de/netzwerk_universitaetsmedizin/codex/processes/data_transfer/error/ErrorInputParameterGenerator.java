package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_MEDIC;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_TYPE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

public class ErrorInputParameterGenerator
{
	public ParameterComponent createMeDicError(String errorCode, String errorMessage)
	{
		return createError(errorCode, errorMessage, CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_MEDIC);
	}

	public ParameterComponent createGthError(String errorCode, String errorMessage)
	{
		return createError(errorCode, errorMessage, CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_GTH);
	}

	public ParameterComponent createCrrError(String errorCode, String errorMessage)
	{
		return createError(errorCode, errorMessage, CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_CRR);
	}

	public ParameterComponent createError(String errorCode, String errorMessage, String source)
	{

		ParameterComponent input = new ParameterComponent();
		input.getType().getCodingFirstRep().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR);

		Extension metaData = input.addExtension();
		metaData.setUrl(EXTENSION_ERROR_METADATA);
		metaData.addExtension().setUrl(EXTENSION_ERROR_METADATA_TYPE)
				.setValue(new Coding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR).setCode(errorCode));
		metaData.addExtension().setUrl(EXTENSION_ERROR_METADATA_SOURCE)
				.setValue(new Coding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE).setCode(source));

		input.setValue(new StringType(errorMessage));

		return input;
	}
}
