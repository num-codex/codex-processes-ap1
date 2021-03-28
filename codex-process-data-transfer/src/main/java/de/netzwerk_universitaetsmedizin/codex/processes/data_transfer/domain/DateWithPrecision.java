package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain;

import java.util.Date;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

@SuppressWarnings("serial")
public final class DateWithPrecision extends Date
{
	private final TemporalPrecisionEnum precision;

	public DateWithPrecision(Date exportFrom, TemporalPrecisionEnum precision)
	{
		super(exportFrom.getTime());

		this.precision = precision;
	}

	public TemporalPrecisionEnum getPrecision()
	{
		return precision;
	}
}