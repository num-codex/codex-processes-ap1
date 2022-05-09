package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.reference;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;

public class PatientReferenceResolverImpl implements PatientReferenceResolver
{
	@Override
	public Optional<Condition> convertLiteralTologicalReference(Condition condition, List<Patient> patients)
	{
		return doConvertLiteralTologicalReference(condition::getSubject, condition::setSubject, patients);
	}

	@Override
	public Optional<DiagnosticReport> convertLiteralTologicalReference(DiagnosticReport diagnosticReport,
			List<Patient> patients)
	{
		return doConvertLiteralTologicalReference(diagnosticReport::getSubject, diagnosticReport::setSubject, patients);
	}

	@Override
	public Optional<Immunization> convertLiteralTologicalReference(Immunization immunization, List<Patient> patients)
	{
		return doConvertLiteralTologicalReference(immunization::getPatient, immunization::setPatient, patients);
	}

	@Override
	public Optional<MedicationStatement> convertLiteralTologicalReference(MedicationStatement medicationStatement,
			List<Patient> patients)
	{
		return doConvertLiteralTologicalReference(medicationStatement::getSubject, medicationStatement::setSubject,
				patients);
	}

	@Override
	public Optional<Observation> convertLiteralTologicalReference(Observation observation, List<Patient> patients)
	{
		return doConvertLiteralTologicalReference(observation::getSubject, observation::setSubject, patients);
	}

	@Override
	public Optional<Procedure> convertLiteralTologicalReference(Procedure procedure, List<Patient> patients)
	{
		return doConvertLiteralTologicalReference(procedure::getSubject, procedure::setSubject, patients);
	}

	@Override
	public Optional<Consent> convertLiteralTologicalReference(Consent consent, List<Patient> patients)
	{
		return doConvertLiteralTologicalReference(consent::getPatient, consent::setPatient, patients);
	}

	private <R extends DomainResource> Optional<R> doConvertLiteralTologicalReference(Supplier<Reference> getPatientRef,
			Function<Reference, R> setPatientRef, List<Patient> patients)
	{
		Objects.requireNonNull(patients, "patients");

		String pseudonym = getDicPseudonym(getPatientRef);
		if (pseudonym == null || pseudonym.isBlank())
		{
			String ref = getLiteralReference(getPatientRef);
			pseudonym = getDicPseudonym(ref, patients);
		}

		if (pseudonym == null || pseudonym.isBlank())
			return Optional.empty();

		Reference ref = new Reference()
				.setIdentifier(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym))
				.setType("Patient");

		return Optional.of(setPatientRef.apply(ref));
	}

	private <R extends DomainResource> String getDicPseudonym(Supplier<Reference> getPatientRef)
	{
		Reference ref = getPatientRef.get();
		if (ref == null)
			return null;

		Identifier id = ref.getIdentifier();
		if (id != null && NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM.equals(id.getSystem()))
			return id.getValue();

		return null;
	}

	private <R extends DomainResource> String getLiteralReference(Supplier<Reference> getPatientRef)
	{
		Reference ref = getPatientRef.get();
		if (ref == null)
			return null;

		return ref.getReference();
	}

	private String getDicPseudonym(String reference, List<Patient> patients)
	{
		if (reference == null || reference.isBlank())
			return null;

		for (Patient p : patients)
		{
			if (reference.equals(p.getIdElement().getValue()))
			{
				for (Identifier identifier : p.getIdentifier())
				{
					if (NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM.equals(identifier.getSystem()))
						return identifier.getValue();
				}
			}
		}

		return null;
	}
}
