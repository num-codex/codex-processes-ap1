package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;

import org.highmed.dsf.fhir.validation.ValueSetExpander;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;

public class ValueSetExpanderWithFileSystemCache
		extends AbstractFhirResourceFileSystemCache<ValueSetExpansionOutcome, ValueSet>
		implements ValueSetExpander, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationPackageClientWithFileSystemCache.class);

	private final ValueSetExpander delegate;

	/**
	 * For JSON content with gzip compression using the <code>.json.xz</code> file name suffix.
	 *
	 * @param cacheFolder
	 *            not <code>null</code>
	 * @param resourceType
	 *            not <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param delegate
	 *            not <code>null</code>
	 * @see AbstractFileSystemCache#FILENAME_SUFFIX
	 * @see AbstractFileSystemCache#OUT_COMPRESSOR_FACTORY
	 * @see AbstractFileSystemCache#IN_COMPRESSOR_FACTORY
	 */
	public ValueSetExpanderWithFileSystemCache(Path cacheFolder, FhirContext fhirContext, ValueSetExpander delegate)
	{
		super(cacheFolder, ValueSet.class, fhirContext);

		this.delegate = delegate;
	}

	public ValueSetExpanderWithFileSystemCache(Path cacheFolder, String fileNameSuffix,
			FunctionWithIoException<OutputStream, OutputStream> outCompressorFactory,
			FunctionWithIoException<InputStream, InputStream> inCompressorFactory, FhirContext fhirContext,
			ValueSetExpander delegate)
	{
		super(cacheFolder, fileNameSuffix, outCompressorFactory, inCompressorFactory, ValueSet.class, fhirContext);

		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ValueSetExpansionOutcome expand(ValueSet valueSet)
	{
		Objects.requireNonNull(valueSet, "valueSet");

		if (valueSet.hasExpansion())
		{
			logger.debug("ValueSet {}|{} already expanded", valueSet.getUrl(), valueSet.getVersion());
			return new ValueSetExpansionOutcome(valueSet);
		}

		Objects.requireNonNull(valueSet.getUrl(), "valueSet.url");
		Objects.requireNonNull(valueSet.getVersion(), "valueSet.version");

		try
		{
			// ValueSetExpansionOutcome read = readFromCache(valueSet.getUrl(), valueSet.getVersion(), ValueSet.class,
			// ValueSetExpansionOutcome::new);
			ValueSetExpansionOutcome read = readResourceFromCache(valueSet.getUrl(), valueSet.getVersion(),
					ValueSetExpansionOutcome::new);

			if (read != null)
				return read;
			else
				return downloadAndWriteToCache(valueSet);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private ValueSetExpansionOutcome downloadAndWriteToCache(ValueSet valueSet) throws IOException
	{
		ValueSetExpansionOutcome expanded = delegate.expand(valueSet);

		if (PublicationStatus.DRAFT.equals(expanded.getValueset().getStatus()))
		{
			logger.info("Not writing expanded ValueSet {}|{} with status {} to cache", expanded.getValueset().getUrl(),
					expanded.getValueset().getVersion(), expanded.getValueset().getStatus());
			return expanded;
		}
		else
			// return writeToCache(expanded, ValueSetExpansionOutcome::getValueset, ValueSet::getUrl,
			// ValueSet::getVersion);
			return writeRsourceToCache(expanded, ValueSetExpansionOutcome::getValueset, ValueSet::getUrl,
					ValueSet::getVersion);
	}
}
