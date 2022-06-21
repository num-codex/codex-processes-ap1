package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;

import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ValidationPackageClientWithFileSystemCache extends AbstractFileSystemCache<ValidationPackage>
		implements ValidationPackageClient, InitializingBean
{
	private final ObjectMapper mapper;
	private final ValidationPackageClient delegate;

	/**
	 * For JSON content with gzip compression using the <code>.json.xz</code> file name suffix.
	 * 
	 * @param cacheFolder
	 *            not <code>null</code>
	 * @param mapper
	 *            not <code>null</code>
	 * @param delegate
	 *            not <code>null</code>
	 * @see AbstractFileSystemCache#FILENAME_SUFFIX
	 * @see AbstractFileSystemCache#OUT_COMPRESSOR_FACTORY
	 * @see AbstractFileSystemCache#IN_COMPRESSOR_FACTORY
	 */
	public ValidationPackageClientWithFileSystemCache(Path cacheFolder, ObjectMapper mapper,
			ValidationPackageClient delegate)
	{
		super(cacheFolder);

		this.mapper = mapper;
		this.delegate = delegate;
	}

	public ValidationPackageClientWithFileSystemCache(Path cacheFolder, String fileNameSuffix,
			FunctionWithIoException<OutputStream, OutputStream> outCompressorFactory,
			FunctionWithIoException<InputStream, InputStream> inCompressorFactory, ObjectMapper mapper,
			ValidationPackageClient delegate)
	{
		super(cacheFolder, fileNameSuffix, outCompressorFactory, inCompressorFactory);

		this.mapper = mapper;
		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(mapper, "mapper");
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ValidationPackage download(ValidationPackageIdentifier identifier)
			throws IOException, WebApplicationException
	{
		Objects.requireNonNull(identifier, "identifier");

		ValidationPackage read = readFromCache(identifier.toString(), "validation package",
				r -> mapper.readValue(r, ValidationPackage.class));

		if (read != null)
			return read;
		else
			return writeToCache(delegate.download(identifier), p -> p.getIdentifier().toString(),
					p -> "validation package", mapper::writeValue);
	}
}
