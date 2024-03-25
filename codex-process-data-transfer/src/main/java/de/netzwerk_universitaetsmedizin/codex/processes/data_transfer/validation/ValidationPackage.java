package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

public class ValidationPackage
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationPackage.class);

	private static final String PACKAGE_JSON_FILENAME = "package/package.json";

	public static ValidationPackage from(String name, String version, InputStream in) throws IOException
	{
		try (BufferedInputStream bufferedIn = new BufferedInputStream(in);
				GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(bufferedIn);
				TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn))
		{
			List<ValidationPackageEntry> entries = new ArrayList<>();

			ArchiveEntry entry;
			while ((entry = tarIn.getNextEntry()) != null)
			{
				ValidationPackageEntry pEntry = ValidationPackageEntry.from(entry, tarIn);
				if (pEntry != null)
					entries.add(pEntry);
			}

			return new ValidationPackage(name, version, entries);
		}
	}

	private final String name;
	private final String version;
	private final List<ValidationPackageEntry> entries = new ArrayList<>();

	private Map<String, ValidationPackageEntry> entriesByFileName;

	private ValidationSupportResources resources;

	/**
	 * @param name
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param entries
	 *            may be <code>null</code>
	 */
	@JsonCreator
	public ValidationPackage(@JsonProperty("name") String name, @JsonProperty("version") String version,
			@JsonProperty("entries") Collection<? extends ValidationPackageEntry> entries)
	{
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(version, "version");

		this.name = name;
		this.version = version;

		if (entries != null)
			this.entries.addAll(entries);
	}

	@JsonProperty("name")
	public String getName()
	{
		return name;
	}

	@JsonProperty("version")
	public String getVersion()
	{
		return version;
	}

	@JsonIgnore
	public ValidationPackageIdentifier getIdentifier()
	{
		return new ValidationPackageIdentifier(name, version);
	}

	@JsonProperty("entries")
	public List<ValidationPackageEntry> getEntries()
	{
		return Collections.unmodifiableList(entries);
	}

	@JsonIgnore
	public Map<String, ValidationPackageEntry> getEntriesByFileName()
	{
		if (entriesByFileName == null)
			entriesByFileName = getEntries().stream().collect(Collectors
					.toUnmodifiableMap(ValidationPackageEntry::getFileName, Function.identity(), (e0, e1) -> e1));

		return entriesByFileName;
	}

	@JsonIgnore
	public ValidationPackageDescriptor getDescriptor(ObjectMapper mapper) throws IOException
	{
		ValidationPackageEntry packageJson = getEntriesByFileName().get(PACKAGE_JSON_FILENAME);
		return mapper.readValue(packageJson.getContent(), ValidationPackageDescriptor.class);
	}

	public void parseResources(FhirContext context)
	{
		if (resources == null)
		{
			List<CodeSystem> codeSystems = new ArrayList<>();
			List<NamingSystem> namingSystems = new ArrayList<>();
			List<StructureDefinition> structureDefinitions = new ArrayList<>();
			List<ValueSet> valueSets = new ArrayList<>();

			getEntries()
					.forEach(doParseResources(context, codeSystems, namingSystems, structureDefinitions, valueSets));

			resources = new ValidationSupportResources(codeSystems, namingSystems, structureDefinitions, valueSets);
		}
	}

	private Consumer<ValidationPackageEntry> doParseResources(FhirContext context, List<CodeSystem> codeSystems,
			List<NamingSystem> namingSystems, List<StructureDefinition> structureDefinitions, List<ValueSet> valueSets)
	{
		return entry ->
		{
			if ("package/package.json".equals(entry.getFileName())
					|| (entry.getFileName() != null && (entry.getFileName().startsWith("package/example")
							|| entry.getFileName().endsWith(".index.json") || !entry.getFileName().endsWith(".json"))))
			{
				logger.debug("Ignoring " + entry.getFileName());
				return;
			}

			logger.debug("Reading " + entry.getFileName());

			try
			{
				String resourceString = new String(entry.getContent(), StandardCharsets.UTF_8);
				// fix profiles because their text contains invalid html
				resourceString = resourceString.replaceAll("<h2>[\\s\\w\\[\\]]*</tt>", "");
				IBaseResource resource = context.newJsonParser().parseResource(resourceString);

				if (resource instanceof CodeSystem)
					codeSystems.add((CodeSystem) resource);
				else if (resource instanceof NamingSystem)
					namingSystems.add((NamingSystem) resource);
				else if (resource instanceof StructureDefinition)
				{
					if (!StructureDefinitionKind.LOGICAL.equals(((StructureDefinition) resource).getKind()))
						structureDefinitions.add((StructureDefinition) resource);
					else
						logger.debug("Ignoring StructureDefinition with kind = logical");
				}
				else if (resource instanceof ValueSet)
					valueSets.add((ValueSet) resource);
				else
					logger.debug("Ignoring resource of type {}", resource.getClass().getName());
			}
			catch (Exception e)
			{
				logger.warn("Ignoring resource with error while parsing, {}: {}", e.getClass().getName(),
						e.getMessage());
			}
		};
	}

	@JsonIgnore
	public ValidationSupportResources getValidationSupportResources()
	{
		if (resources == null)
			throw new IllegalStateException("Resources not parsed");

		return resources;
	}
}
