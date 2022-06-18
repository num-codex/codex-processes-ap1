package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationPackageDescriptor
{
	private final String author;
	private final String canonical;
	private final Map<String, String> dependencies = new HashMap<>();
	private final String description;
	// fhir-version-list
	private final List<String> fhirVersions = new ArrayList<>();
	private final String jurisdiction;
	private final List<String> keywords = new ArrayList<>();
	private final String license;
	private final List<ValidationPackageDescriptorMaintainer> maintainers = new ArrayList<>();
	private final String name;
	private final String title;
	private final String url;
	private final String version;

	@JsonCreator
	public ValidationPackageDescriptor(@JsonProperty("author") String author,
			@JsonProperty("canonical") String canonical, @JsonProperty("dependencies") Map<String, String> dependencies,
			@JsonProperty("description") String description,
			@JsonProperty("fhirVersions") @JsonAlias("fhir-version-list") List<String> fhirVersions,
			@JsonProperty("jurisdiction") String jurisdiction, @JsonProperty("keywords") List<String> keywords,
			@JsonProperty("license") String license,
			@JsonProperty("maintainers") List<ValidationPackageDescriptorMaintainer> maintainers,
			@JsonProperty("name") String name, @JsonProperty("title") String title, @JsonProperty("url") String url,
			@JsonProperty("version") String version)
	{
		this.author = author;
		this.canonical = canonical;

		if (dependencies != null)
			this.dependencies.putAll(dependencies);

		this.description = description;

		if (fhirVersions != null)
			this.fhirVersions.addAll(fhirVersions);

		this.jurisdiction = jurisdiction;

		if (keywords != null)
			this.keywords.addAll(keywords);

		this.license = license;

		if (maintainers != null)
			this.maintainers.addAll(maintainers);

		this.name = name;
		this.title = title;
		this.url = url;
		this.version = version;
	}

	@JsonProperty("author")
	public String getAuthor()
	{
		return author;
	}

	@JsonProperty("canonical")
	public String getCanonical()
	{
		return canonical;
	}

	@JsonProperty("dependencies")
	public Map<String, String> getDependencies()
	{
		return Collections.unmodifiableMap(dependencies);
	}

	@JsonIgnore
	public List<ValidationPackageIdentifier> getDependencyIdentifiers()
	{
		return dependencies.entrySet().stream().map(e -> new ValidationPackageIdentifier(e.getKey(), e.getValue()))
				.collect(Collectors.toUnmodifiableList());
	}

	@JsonProperty("description")
	public String getDescription()
	{
		return description;
	}

	@JsonProperty("fhirVersions")
	public List<String> getFhirVersions()
	{
		return Collections.unmodifiableList(fhirVersions);
	}

	@JsonProperty("jurisdiction")
	public String getJurisdiction()
	{
		return jurisdiction;
	}

	@JsonProperty("keywords")
	public List<String> getKeywords()
	{
		return Collections.unmodifiableList(keywords);
	}

	@JsonProperty("license")
	public String getLicense()
	{
		return license;
	}

	@JsonProperty("maintainers")
	public List<ValidationPackageDescriptorMaintainer> getMaintainers()
	{
		return Collections.unmodifiableList(maintainers);
	}

	@JsonProperty("name")
	public String getName()
	{
		return name;
	}

	@JsonProperty("title")
	public String getTitle()
	{
		return title;
	}

	@JsonProperty("url")
	public String getUrl()
	{
		return url;
	}

	@JsonProperty("version")
	public String getVersion()
	{
		return version;
	}
}
