package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationPackageDescriptorMaintainer
{
	private final String email;
	private final String name;

	@JsonCreator
	public ValidationPackageDescriptorMaintainer(@JsonProperty("email") String email, @JsonProperty("name") String name)
	{
		this.email = email;
		this.name = name;
	}

	@JsonProperty("email")
	public String getEmail()
	{
		return email;
	}

	@JsonProperty("name")
	public String getName()
	{
		return name;
	}
}
