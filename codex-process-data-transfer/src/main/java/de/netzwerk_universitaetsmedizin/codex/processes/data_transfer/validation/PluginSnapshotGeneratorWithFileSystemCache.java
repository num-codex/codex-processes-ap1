package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;

public class PluginSnapshotGeneratorWithFileSystemCache
		extends AbstractFhirResourceFileSystemCache<PluginSnapshotWithValidationMessages, StructureDefinition>
		implements PluginSnapshotGenerator, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationPackageClientWithFileSystemCache.class);

	private final PluginSnapshotGenerator delegate;

	/**
	 * For JSON content with gzip compression using the <code>.json.xz</code> file name suffix.
	 * 
	 * @param cacheFolder
	 *            not <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param delegate
	 *            not <code>null</code>
	 * @see AbstractFileSystemCache#FILENAME_SUFFIX
	 * @see AbstractFileSystemCache#OUT_COMPRESSOR_FACTORY
	 * @see AbstractFileSystemCache#IN_COMPRESSOR_FACTORY
	 */
	public PluginSnapshotGeneratorWithFileSystemCache(Path cacheFolder, FhirContext fhirContext,
			PluginSnapshotGenerator delegate)
	{
		super(cacheFolder, StructureDefinition.class, fhirContext);

		this.delegate = delegate;
	}

	public PluginSnapshotGeneratorWithFileSystemCache(Path cacheFolder, String fileNameSuffix,
			FunctionWithIoException<OutputStream, OutputStream> outCompressorFactory,
			FunctionWithIoException<InputStream, InputStream> inCompressorFactory, FhirContext fhirContext,
			PluginSnapshotGenerator delegate)
	{
		super(cacheFolder, fileNameSuffix, outCompressorFactory, inCompressorFactory, StructureDefinition.class,
				fhirContext);

		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public PluginSnapshotWithValidationMessages generateSnapshot(StructureDefinition structureDefinition)
	{
		Objects.requireNonNull(structureDefinition, "differential");

		if (structureDefinition.hasSnapshot())
		{
			logger.debug("StructureDefinition {}|{} has snapshot", structureDefinition.getUrl(),
					structureDefinition.getVersion());
			return new PluginSnapshotWithValidationMessages(structureDefinition, Collections.emptyList());
		}

		Objects.requireNonNull(structureDefinition.getUrl(), "structureDefinition.url");
		Objects.requireNonNull(structureDefinition.getVersion(), "structureDefinition.version");

		try
		{
			PluginSnapshotWithValidationMessages read = readResourceFromCache(structureDefinition.getUrl(),
					structureDefinition.getVersion(),
					// needs to return original structureDefinition object with included snapshot
					sd -> new PluginSnapshotWithValidationMessages(structureDefinition.setSnapshot(sd.getSnapshot()),
							Collections.emptyList()));
			if (read != null)
				return read;
			else
				return downloadAndWriteToCache(structureDefinition);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private PluginSnapshotWithValidationMessages downloadAndWriteToCache(StructureDefinition structureDefinition)
			throws IOException
	{
		PluginSnapshotWithValidationMessages snapshot = delegate.generateSnapshot(structureDefinition);

		if (PublicationStatus.DRAFT.equals(snapshot.getSnapshot().getStatus()))
		{
			logger.info("Not writing StructureDefinition {}|{} with snapshot and status {} to cache",
					snapshot.getSnapshot().getUrl(), snapshot.getSnapshot().getVersion(),
					snapshot.getSnapshot().getStatus());
			return snapshot;
		}
		else
			return writeRsourceToCache(snapshot, PluginSnapshotWithValidationMessages::getSnapshot,
					StructureDefinition::getUrl, StructureDefinition::getVersion);
	}
}
