package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.ValueSet;

public interface ValueSetExpansionClient
{
	/**
	 * @param valueSet
	 *            not <code>null</code>
	 * @return expanded {@link ValueSet}, never <code>null</code>
	 * @throws IOException
	 * @throws WebApplicationException
	 */
	ValueSet expand(ValueSet valueSet) throws IOException, WebApplicationException;

	CapabilityStatement getMetadata() throws WebApplicationException;
}
