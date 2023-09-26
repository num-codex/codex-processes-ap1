package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message;

import dev.dsf.bpe.v1.ProcessPluginApi;

public class ContinueTranslateProcess extends AbstractContinueTranslateProcess
{
	public ContinueTranslateProcess(ProcessPluginApi api, String dtsIdentifierValue)
	{
		super(api, dtsIdentifierValue);
	}
}
