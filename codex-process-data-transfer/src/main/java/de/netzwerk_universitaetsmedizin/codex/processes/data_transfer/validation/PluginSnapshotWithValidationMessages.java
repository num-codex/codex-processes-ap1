package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.utilities.validation.ValidationMessage;

public class PluginSnapshotWithValidationMessages
{
	private final StructureDefinition snapshot;
	private final List<ValidationMessage> messages;

	PluginSnapshotWithValidationMessages(StructureDefinition snapshot, List<ValidationMessage> messages)
	{
		this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
		this.messages = Objects.requireNonNull(messages, "messages");
	}

	public StructureDefinition getSnapshot()
	{
		return snapshot;
	}

	public List<ValidationMessage> getMessages()
	{
		return Collections.unmodifiableList(messages);
	}
}