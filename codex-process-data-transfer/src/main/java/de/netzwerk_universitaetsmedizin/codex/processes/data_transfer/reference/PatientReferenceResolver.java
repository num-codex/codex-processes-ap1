package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.reference;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;

public interface PatientReferenceResolver
{
	Optional<Condition> convertLiteralTologicalReference(Condition condition, List<Patient> patients);

	Optional<Consent> convertLiteralTologicalReference(Consent consent, List<Patient> patients);

	Optional<DiagnosticReport> convertLiteralTologicalReference(DiagnosticReport diagnosticReport,
			List<Patient> patients);

	Optional<Immunization> convertLiteralTologicalReference(Immunization immunization, List<Patient> patients);

	Optional<MedicationStatement> convertLiteralTologicalReference(MedicationStatement medicationStatement,
			List<Patient> patients);

	Optional<Observation> convertLiteralTologicalReference(Observation observation, List<Patient> patients);

	Optional<Procedure> convertLiteralTologicalReference(Procedure procedure, List<Patient> patients);
}
