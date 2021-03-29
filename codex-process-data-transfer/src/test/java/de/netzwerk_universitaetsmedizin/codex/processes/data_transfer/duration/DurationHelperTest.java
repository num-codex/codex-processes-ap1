package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.camunda.bpm.engine.impl.calendar.DurationHelper;
import org.junit.Test;

public class DurationHelperTest
{
	@Test
	public void testParseDuration() throws Exception
	{
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime nowPlus24 = now.plusHours(24);

		DurationHelper durationHelper = new DurationHelper("PT24H",
				Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
		Date next = durationHelper.getDateAfter();

		assertNotNull(next);
		assertEquals(nowPlus24.truncatedTo(ChronoUnit.MILLIS),
				LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault()));
	}
}
