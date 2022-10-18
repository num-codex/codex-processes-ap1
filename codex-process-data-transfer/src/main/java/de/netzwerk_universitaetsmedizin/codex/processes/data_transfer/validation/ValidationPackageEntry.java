package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationPackageEntry
{
	/**
	 * Does not close the input stream.
	 *
	 * @param entry
	 *            not <code>null</code>
	 * @param in
	 *            not <code>null</code>
	 * @return {@link ValidationPackageEntry} for the given {@link ArchiveEntry} from the given
	 *         {@link TarArchiveInputStream}, <code>null</code> if the given entry can't be read from the input stream.
	 * @throws IOException
	 * @see {@link TarArchiveInputStream#canReadEntryData(ArchiveEntry)}
	 */
	public static ValidationPackageEntry from(ArchiveEntry entry, TarArchiveInputStream in) throws IOException
	{
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(in, "in");

		if (!in.canReadEntryData(entry))
			return null;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(in, out);
		return new ValidationPackageEntry(entry.getName(), entry.getLastModifiedDate(), out.toByteArray());
	}

	private final String fileName;
	private final Date lastModified;
	private final byte[] content;

	@JsonCreator
	public ValidationPackageEntry(@JsonProperty("fileName") String fileName,
			@JsonProperty("lastModified") Date lastModified, @JsonProperty("content") byte[] content)
	{
		this.fileName = fileName;
		this.lastModified = lastModified;
		this.content = content;
	}

	@JsonProperty("fileName")
	public String getFileName()
	{
		return fileName;
	}

	@JsonProperty("lastModified")
	public Date getLastModified()
	{
		return lastModified;
	}

	@JsonProperty("content")
	public byte[] getContent()
	{
		return content;
	}
}
