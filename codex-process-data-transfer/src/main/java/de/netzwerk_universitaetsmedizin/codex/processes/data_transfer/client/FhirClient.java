package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Patient;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceList;

public interface FhirClient
{
	/**
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	PatientReferenceList getPatientReferencesWithNewData(DateWithPrecision exportFrom, Date exportTo);

	/**
	 * @param pseudonym
	 *            not <code>null</code>
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	Stream<DomainResource> getNewData(String pseudonym, DateWithPrecision exportFrom, Date exportTo);

	void storeBundle(Bundle bundle);

	/**
	 * @param reference
	 *            Absolute reference, not <code>null</code>
	 * @return
	 */
	Optional<Patient> getPatient(String reference);

	/**
	 * @param patient
	 *            containing an identifier with system
	 *            {@link de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer#NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM}
	 *            not <code>null</code>
	 */
	void updatePatient(Patient patient);
}
