package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.error;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_MEDIC;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_TYPE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_TYPE_VALUE_VALIDATION_FAILED;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_SOURCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.EXTENSION_ERROR_METADATA_TYPE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

public class ErrorOutputParameterGenerator
{
	public Stream<TaskOutputComponent> createMeDicValidationError(IdType sourceId, OperationOutcome outcome)
	{
		return outcome.getIssue().stream()
				.filter(i -> IssueSeverity.FATAL.equals(i.getSeverity()) || IssueSeverity.ERROR.equals(i.getSeverity()))
				.map(i -> createMeDicValidationError(sourceId, i));
	}

	private TaskOutputComponent createMeDicValidationError(IdType sourceId, OperationOutcomeIssueComponent i)
	{
		TaskOutputComponent output = new TaskOutputComponent();
		output.getType().getCodingFirstRep().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR);

		Extension metaData = output.addExtension();
		metaData.setUrl(EXTENSION_ERROR_METADATA);
		metaData.addExtension().setUrl(EXTENSION_ERROR_METADATA_TYPE)
				.setValue(new Coding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_TYPE)
						.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_TYPE_VALUE_VALIDATION_FAILED));
		metaData.addExtension().setUrl(EXTENSION_ERROR_METADATA_SOURCE)
				.setValue(new Coding().setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE)
						.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_SOURCE_VALUE_MEDIC));

		if (sourceId != null)
			metaData.addExtension().setUrl(EXTENSION_ERROR_METADATA_REFERENCE)
					.setValue(new Reference().setReferenceElement(sourceId));

		output.setValue(new StringType(
				"Validation faild at " + i.getLocation().stream().map(StringType::getValue).findFirst().orElse("?")));

		return output;
	}
}