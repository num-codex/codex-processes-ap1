package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractFileSystemCache<T> implements InitializingBean
{
	public static final String FILENAME_SUFFIX = ".json.gz";
	public static final FunctionWithIoException<OutputStream, OutputStream> OUT_COMPRESSOR_FACTORY = GzipCompressorOutputStream::new;
	public static final FunctionWithIoException<InputStream, InputStream> IN_COMPRESSOR_FACTORY = GzipCompressorInputStream::new;

	@FunctionalInterface
	public interface FunctionWithIoException<T, R>
	{
		R apply(T t) throws IOException;
	}

	@FunctionalInterface
	public interface BiConsumerWithIoException<T, U>
	{
		void accept(T t, U u) throws IOException;
	}

	private static final Logger logger = LoggerFactory.getLogger(AbstractFileSystemCache.class);

	private final Path cacheFolder;
	private final String filenameSuffix;
	private final FunctionWithIoException<OutputStream, OutputStream> outCompressorFactory;
	private final FunctionWithIoException<InputStream, InputStream> inCompressorFactory;

	/**
	 * For JSON content with gzip compression using the <code>.json.gz</code> file name suffix.
	 * 
	 * @param cacheFolder
	 *            not <code>null</code>
	 * @see #FILENAME_SUFFIX
	 * @see #OUT_COMPRESSOR_FACTORY
	 * @see #IN_COMPRESSOR_FACTORY
	 */
	public AbstractFileSystemCache(Path cacheFolder)
	{
		this(cacheFolder, FILENAME_SUFFIX, OUT_COMPRESSOR_FACTORY, IN_COMPRESSOR_FACTORY);
	}

	public AbstractFileSystemCache(Path cacheFolder, String filenameSuffix,
			FunctionWithIoException<OutputStream, OutputStream> outCompressorFactory,
			FunctionWithIoException<InputStream, InputStream> inCompressorFactory)
	{
		this.cacheFolder = cacheFolder;
		this.filenameSuffix = filenameSuffix;
		this.outCompressorFactory = outCompressorFactory;
		this.inCompressorFactory = inCompressorFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(cacheFolder, "cacheFolder");
		Objects.requireNonNull(filenameSuffix, "filenameSuffix");
		Objects.requireNonNull(outCompressorFactory, "outCompressorFactory");
		Objects.requireNonNull(inCompressorFactory, "inCompressorFactory");

		if (!Files.isWritable(cacheFolder))
			throw new IOException("Folder " + cacheFolder.toAbsolutePath().toString() + "not writable");
	}

	private Path cacheFile(String cacheEntryId)
	{
		cacheEntryId = cacheEntryId.replace("://", "_").replaceAll("/", "_").replace(":", "_").replace("|", "_")
				.replace("\\", "_");

		return cacheFolder.resolve(cacheEntryId + filenameSuffix);
	}

	protected final T readFromCache(String cacheEntryId, String cacheEntryType,
			FunctionWithIoException<Reader, T> decoder) throws IOException
	{
		return readFromCache(cacheEntryId, cacheEntryType, decoder, Function.identity());
	}

	protected final <R> T readFromCache(String cacheEntryId, String cacheEntryType,
			FunctionWithIoException<Reader, R> decoder, Function<R, T> fromResource) throws IOException
	{
		Path cacheFile = cacheFile(cacheEntryId);

		if (!Files.exists(cacheFile))
		{
			logger.debug("Cache file for {} {} does not exist", cacheEntryType, cacheEntryId);
			return null;
		}
		else if (Files.exists(cacheFile) && !Files.isReadable(cacheFile))
		{
			logger.error("Cache file for {} {} exist in cache but is not readable", cacheEntryType, cacheEntryId);
			return null;
		}

		try (InputStream in = Files.newInputStream(cacheFile);
				BufferedInputStream bIn = new BufferedInputStream(in);
				InputStream cIn = inCompressorFactory.apply(bIn);
				InputStreamReader reader = new InputStreamReader(cIn, StandardCharsets.UTF_8))
		{
			logger.debug("Reading {} {} from cache at {}", cacheEntryType, cacheEntryId, cacheFile.toString());
			return fromResource.apply(decoder.apply(reader));
		}
	}

	protected final T writeToCache(T value, Function<T, String> toCacheId, Function<T, String> toCacheEntryType,
			BiConsumerWithIoException<Writer, T> encoder) throws IOException
	{
		return writeToCache(value, toCacheId, toCacheEntryType, encoder, Function.identity());
	}

	protected final <R> T writeToCache(T value, Function<R, String> toCacheId, Function<R, String> toCacheEntryType,
			BiConsumerWithIoException<Writer, R> encoder, Function<T, R> toResource) throws IOException
	{
		R resource = toResource.apply(value);
		String cacheId = toCacheId.apply(resource);
		String cacheEntryType = toCacheEntryType.apply(resource);

		Path cacheFile = cacheFile(cacheId);

		try (OutputStream out = Files.newOutputStream(cacheFile, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				BufferedOutputStream bOut = new BufferedOutputStream(out);
				OutputStream cOut = outCompressorFactory.apply(bOut);
				OutputStreamWriter writer = new OutputStreamWriter(cOut, StandardCharsets.UTF_8))
		{
			logger.debug("Wirting {} {} to cache at {}", cacheEntryType, cacheId, cacheFile.toString());
			encoder.accept(writer, resource);
		}

		return value;
	}
}
