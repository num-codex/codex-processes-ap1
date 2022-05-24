package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import org.hl7.fhir.r4.model.StructureDefinition;

public interface PluginSnapshotGenerator
{
	PluginSnapshotWithValidationMessages generateSnapshot(StructureDefinition differential);
}