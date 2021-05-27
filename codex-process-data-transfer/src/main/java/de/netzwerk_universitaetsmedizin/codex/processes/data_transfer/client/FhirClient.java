package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client;

import java.util.Date;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Patient;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PseudonymList;

public interface FhirClient
{
	/**
	 * @param exportFrom
	 *            may be <code>null</code>
	 * @param exportTo
	 *            not <code>null</code>
	 * @return
	 */
	PseudonymList getPseudonymsWithNewData(DateWithPrecision exportFrom, Date exportTo);

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
	Patient getPatient(String reference);
}
