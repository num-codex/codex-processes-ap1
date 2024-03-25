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
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;

public class PluginSnapshotGeneratorWithFileSystemCache
		extends AbstractFhirResourceFileSystemCache<SnapshotWithValidationMessages, StructureDefinition>
		implements SnapshotGenerator, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationPackageClientWithFileSystemCache.class);

	private final SnapshotGenerator delegate;

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
			SnapshotGenerator delegate)
	{
		super(cacheFolder, StructureDefinition.class, fhirContext);

		this.delegate = delegate;
	}

	public PluginSnapshotGeneratorWithFileSystemCache(Path cacheFolder, String fileNameSuffix,
			FunctionWithIoException<OutputStream, OutputStream> outCompressorFactory,
			FunctionWithIoException<InputStream, InputStream> inCompressorFactory, FhirContext fhirContext,
			SnapshotGenerator delegate)
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
	public SnapshotWithValidationMessages generateSnapshot(StructureDefinition structureDefinition)
	{
		Objects.requireNonNull(structureDefinition, "differential");

		if (structureDefinition.hasSnapshot())
		{
			logger.debug("StructureDefinition {}|{} has snapshot", structureDefinition.getUrl(),
					structureDefinition.getVersion());
			return new SnapshotWithValidationMessages(structureDefinition, Collections.emptyList());
		}

		Objects.requireNonNull(structureDefinition.getUrl(), "structureDefinition.url");
		Objects.requireNonNull(structureDefinition.getVersion(), "structureDefinition.version");

		try
		{
			SnapshotWithValidationMessages read = readResourceFromCache(structureDefinition.getUrl(),
					structureDefinition.getVersion(),
					// needs to return original structureDefinition object with included snapshot
					sd -> new SnapshotWithValidationMessages(structureDefinition.setSnapshot(sd.getSnapshot()),
							Collections.emptyList()));
			if (read != null)
				return read;
			else
				return generateSnapshotAndWriteToCache(structureDefinition);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private SnapshotWithValidationMessages generateSnapshotAndWriteToCache(StructureDefinition structureDefinition)
			throws IOException
	{
		SnapshotWithValidationMessages snapshot = delegate.generateSnapshot(structureDefinition);

		if (PublicationStatus.DRAFT.equals(snapshot.getSnapshot().getStatus()))
		{
			logger.warn("Not writing StructureDefinition {}|{} with snapshot and status {} to cache",
					snapshot.getSnapshot().getUrl(), snapshot.getSnapshot().getVersion(),
					snapshot.getSnapshot().getStatus());
			return snapshot;
		}
		else if (!snapshot.getSnapshot().hasSnapshot())
		{
			logger.info("Not writing StructureDefinition {}|{} without snapshot to cache",
					snapshot.getSnapshot().getUrl(), snapshot.getSnapshot().getVersion());
			return snapshot;
		}
		else
			return writeRsourceToCache(snapshot, SnapshotWithValidationMessages::getSnapshot,
					StructureDefinition::getUrl, StructureDefinition::getVersion);
	}

	@Override
	public SnapshotWithValidationMessages generateSnapshot(StructureDefinition differential,
			String baseAbsoluteUrlPrefix)
	{
		throw new UnsupportedOperationException("not implemented");
	}
}
