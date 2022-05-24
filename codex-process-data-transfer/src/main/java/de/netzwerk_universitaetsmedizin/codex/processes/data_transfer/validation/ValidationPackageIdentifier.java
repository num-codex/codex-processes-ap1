package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.Objects;

public class ValidationPackageIdentifier
{
	public static ValidationPackageIdentifier fromString(String nameAndVersion)
	{
		String[] split = nameAndVersion.split("\\|");

		if (split.length != 2)
			throw new IllegalArgumentException("Validation package not specified as 'name|version'");

		return new ValidationPackageIdentifier(split[0], split[1]);
	}

	private final String name;
	private final String version;

	public ValidationPackageIdentifier(String name, String version)
	{
		this.name = Objects.requireNonNull(name, "name");
		this.version = Objects.requireNonNull(version, "version");
	}

	public String getName()
	{
		return name;
	}

	public String getVersion()
	{
		return version;
	}

	@Override
	public String toString()
	{
		return name + "|" + version;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValidationPackageIdentifier other = (ValidationPackageIdentifier) obj;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (version == null)
		{
			if (other.version != null)
				return false;
		}
		else if (!version.equals(other.version))
			return false;
		return true;
	}
}