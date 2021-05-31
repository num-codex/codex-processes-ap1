package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PatientReference
{
	public static PatientReference from(Identifier identifier)
	{
		Objects.requireNonNull(identifier);

		if (!identifier.hasSystem() || !identifier.hasValue())
			throw new IllegalArgumentException("identifier has no system or value");

		return new PatientReference(identifier.getSystem(), identifier.getValue(), null);
	}

	public static PatientReference from(String absoluteReference)
	{
		if (StringUtils.isEmpty(absoluteReference))
			throw new IllegalArgumentException("absoluteReference null or empty");

		return new PatientReference(null, null, absoluteReference);
	}

	@JsonProperty("identifierValue")
	private final String identifierValue;

	@JsonProperty("identifierSystem")
	private final String identifierSystem;

	@JsonProperty("absoluteReference")
	private final String absoluteReference;

	@JsonCreator
	private PatientReference(@JsonProperty("identifierSystem") String identifierSystem,
			@JsonProperty("identifierValue") String identifierValue,
			@JsonProperty("absoluteReference") String absoluteReference)
	{
		this.identifierSystem = identifierSystem;
		this.identifierValue = identifierValue;
		this.absoluteReference = absoluteReference;
	}

	public boolean hasIdentifier()
	{
		return !StringUtils.isEmpty(identifierSystem) && !StringUtils.isEmpty(identifierValue);
	}

	@JsonIgnore
	public Identifier getIdentifier()
	{
		return new Identifier().setSystem(identifierSystem).setValue(identifierValue);
	}

	public boolean hasAbsoluteReference()
	{
		return !StringUtils.isEmpty(absoluteReference);
	}

	@JsonIgnore
	public String getAbsoluteReference()
	{
		return absoluteReference;
	}
}
