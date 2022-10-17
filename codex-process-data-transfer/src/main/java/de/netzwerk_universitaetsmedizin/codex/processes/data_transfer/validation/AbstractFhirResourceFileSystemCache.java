package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.parser.IParser;

public abstract class AbstractFhirResourceFileSystemCache<T, R extends Resource> extends AbstractFileSystemCache<T>
		implements InitializingBean
{
	private final Class<R> resourceType;
	private final FhirContext fhirContext;

	/**
	 * For JSON content with gzip compression using the <code>.json.gz</code> file name suffix.
	 *
	 * @param cacheFolder
	 *            not <code>null</code>
	 * @param resourceType
	 *            not <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @see AbstractFileSystemCache#FILENAME_SUFFIX
	 * @see AbstractFileSystemCache#OUT_COMPRESSOR_FACTORY
	 * @see AbstractFileSystemCache#IN_COMPRESSOR_FACTORY
	 */
	public AbstractFhirResourceFileSystemCache(Path cacheFolder, Class<R> resourceType, FhirContext fhirContext)
	{
		super(cacheFolder, AbstractFileSystemCache.FILENAME_SUFFIX, AbstractFileSystemCache.OUT_COMPRESSOR_FACTORY,
				AbstractFileSystemCache.IN_COMPRESSOR_FACTORY);

		this.resourceType = resourceType;
		this.fhirContext = fhirContext;
	}

	public AbstractFhirResourceFileSystemCache(Path cacheFolder, String fileNameSuffix,
			FunctionWithIoException<OutputStream, OutputStream> outCompressorFactory,
			FunctionWithIoException<InputStream, InputStream> inCompressorFactory, Class<R> resourceType,
			FhirContext fhirContext)
	{
		super(cacheFolder, fileNameSuffix, outCompressorFactory, inCompressorFactory);

		this.resourceType = resourceType;
		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	protected IParser getJsonParser()
	{
		return fhirContext.newJsonParser();
	}

	protected T readResourceFromCache(String url, String version, Function<R, T> fromResource) throws IOException
	{
		return readFromCache(url + "|" + version, resourceType.getAnnotation(ResourceDef.class).name(),
				reader -> getJsonParser().parseResource(resourceType, reader), fromResource);
	}

	protected T writeRsourceToCache(T value, Function<T, R> toResource, Function<R, String> toUrl,
			Function<R, String> toVersion) throws IOException
	{
		return writeToCache(value, r -> toUrl.apply(r) + "|" + toVersion.apply(r), r -> r.getResourceType().name(),
				(w, r) -> getJsonParser().encodeResourceToWriter(r, w), toResource);
	}
}
