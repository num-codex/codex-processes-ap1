package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.fhir.ucum.UcumService;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.TerminologyServiceException;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.formats.IParser;
import org.hl7.fhir.r4.formats.ParserType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.hl7.fhir.r4.utils.INarrativeGenerator;
import org.hl7.fhir.r4.utils.IResourceValidator;
import org.hl7.fhir.utilities.TranslationServices;
import org.hl7.fhir.utilities.validation.ValidationOptions;

public class PluginWorkerContext implements IWorkerContext
{
	private final IWorkerContext delegate;

	public PluginWorkerContext(IWorkerContext delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public String getVersion()
	{
		return delegate.getVersion();
	}

	@Override
	public UcumService getUcumService()
	{
		return delegate.getUcumService();
	}

	@Override
	public IParser getParser(ParserType type)
	{
		return delegate.getParser(type);
	}

	@Override
	public IParser getParser(String type)
	{
		return delegate.getParser(type);
	}

	@Override
	public IParser newJsonParser()
	{
		return delegate.newJsonParser();
	}

	@Override
	public IParser newXmlParser()
	{
		return delegate.newXmlParser();
	}

	@Override
	public INarrativeGenerator getNarrativeGenerator(String prefix, String basePath)
	{
		return delegate.getNarrativeGenerator(prefix, basePath);
	}

	@Override
	public IResourceValidator newValidator() throws FHIRException
	{
		return delegate.newValidator();
	}

	@Override
	public <T extends Resource> T fetchResource(Class<T> class_, String uri)
	{
		return delegate.fetchResource(class_, uri);
	}

	@Override
	public <T extends Resource> T fetchResourceWithException(Class<T> class_, String uri) throws FHIRException
	{
		return delegate.fetchResourceWithException(class_, uri);
	}

	@Override
	public Resource fetchResourceById(String type, String uri)
	{
		return delegate.fetchResourceById(type, uri);
	}

	@Override
	public <T extends Resource> boolean hasResource(Class<T> class_, String uri)
	{
		return delegate.hasResource(class_, uri);
	}

	@Override
	public void cacheResource(Resource res) throws FHIRException
	{
		delegate.cacheResource(res);
	}

	@Override
	public List<String> getResourceNames()
	{
		return delegate.getResourceNames();
	}

	@Override
	public Set<String> getResourceNamesAsSet()
	{
		return delegate.getResourceNamesAsSet();
	}

	@Override
	public List<String> getTypeNames()
	{
		return delegate.getTypeNames();
	}

	@Override
	public List<StructureDefinition> allStructures()
	{
		return delegate.allStructures();
	}

	@Override
	public List<StructureDefinition> getStructures()
	{
		return delegate.getStructures();
	}

	@Override
	public List<MetadataResource> allConformanceResources()
	{
		return delegate.allConformanceResources();
	}

	@Override
	public void generateSnapshot(StructureDefinition p) throws DefinitionException, FHIRException
	{
		delegate.generateSnapshot(p);
	}

	@Override
	public Parameters getExpansionParameters()
	{
		return delegate.getExpansionParameters();
	}

	@Override
	public void setExpansionProfile(Parameters expParameters)
	{
		delegate.setExpansionProfile(expParameters);
	}

	@Override
	public CodeSystem fetchCodeSystem(String system)
	{
		return delegate.fetchCodeSystem(system);
	}

	@Override
	public boolean supportsSystem(String system) throws TerminologyServiceException
	{
		return delegate.supportsSystem(system);
	}

	@Override
	public List<ConceptMap> findMapsForSource(String url) throws FHIRException
	{
		return delegate.findMapsForSource(url);
	}

	@Override
	public ValueSetExpansionOutcome expandVS(ValueSet source, boolean cacheOk, boolean heiarchical)
	{
		if (source.hasExpansion())
			return new ValueSetExpansionOutcome(source);
		else
			return new ValueSetExpansionOutcome(null);
	}

	@Override
	public ValueSetExpansionOutcome expandVS(ElementDefinitionBindingComponent binding, boolean cacheOk,
			boolean heiarchical) throws FHIRException
	{
		return delegate.expandVS(binding, cacheOk, heiarchical);
	}

	@Override

	public ValueSetExpansionOutcome expandVS(ConceptSetComponent inc, boolean heirarchical)
			throws TerminologyServiceException
	{
		return delegate.expandVS(inc, heirarchical);
	}

	@Override
	public Locale getLocale()
	{
		return delegate.getLocale();
	}

	@Override
	public void setLocale(Locale locale)
	{
		delegate.setLocale(locale);
	}

	@Override
	public String formatMessage(String theMessage, Object... theMessageArguments)
	{
		return delegate.formatMessage(theMessage, theMessageArguments);
	}

	@Override
	public void setValidationMessageLanguage(Locale locale)
	{
		delegate.setValidationMessageLanguage(locale);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, String system, String code, String display)
	{
		return delegate.validateCode(options, system, code, display);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, String system, String code, String display,
			ValueSet vs)
	{
		return delegate.validateCode(options, system, code, display, vs);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, String code, ValueSet vs)
	{
		return delegate.validateCode(options, code, vs);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, Coding code, ValueSet vs)
	{
		return delegate.validateCode(options, code, vs);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, CodeableConcept code, ValueSet vs)
	{
		return delegate.validateCode(options, code, vs);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, String system, String code, String display,
			ConceptSetComponent vsi)
	{
		return delegate.validateCode(options, system, code, display, vsi);
	}

	@Override
	public String getAbbreviation(String name)
	{
		return delegate.getAbbreviation(name);
	}

	@Override
	public Set<String> typeTails()
	{
		return delegate.typeTails();
	}

	@Override
	public String oid2Uri(String code)
	{
		return delegate.oid2Uri(code);
	}

	@Override
	public boolean hasCache()
	{
		return delegate.hasCache();
	}

	@Override
	public void setLogger(ILoggingService logger)
	{
		delegate.setLogger(logger);
	}

	@Override
	public ILoggingService getLogger()
	{
		return delegate.getLogger();
	}

	@Override
	public boolean isNoTerminologyServer()
	{
		return delegate.isNoTerminologyServer();
	}

	@Override
	public TranslationServices translator()
	{
		return delegate.translator();
	}

	@Override
	public List<StructureMap> listTransforms()
	{
		return delegate.listTransforms();
	}

	@Override
	public StructureMap getTransform(String url)
	{
		return delegate.getTransform(url);
	}

	@Override
	public String getOverrideVersionNs()
	{
		return delegate.getOverrideVersionNs();
	}

	@Override
	public void setOverrideVersionNs(String value)
	{
		delegate.setOverrideVersionNs(value);
	}

	@Override
	public StructureDefinition fetchTypeDefinition(String typeName)
	{
		return delegate.fetchTypeDefinition(typeName);
	}

	@Override
	public void setUcumService(UcumService ucumService)
	{
		delegate.setUcumService(ucumService);
	}

	@Override
	public String getLinkForUrl(String corePath, String s)
	{
		return delegate.getLinkForUrl(corePath, s);
	}
}
