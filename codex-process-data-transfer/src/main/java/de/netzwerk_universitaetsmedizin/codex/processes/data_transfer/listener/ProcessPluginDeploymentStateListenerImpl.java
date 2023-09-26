package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.listener;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config.ValidationConfig.TerminologyServerConnectionTestStatus;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.BundleValidatorFactory;
import dev.dsf.bpe.v1.ProcessPluginDeploymentStateListener;

public class ProcessPluginDeploymentStateListenerImpl implements ProcessPluginDeploymentStateListener, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginDeploymentStateListenerImpl.class);

	private final DataStoreClientFactory dataStoreClientFactory;
	private final FttpClientFactory fttpClientFactory;
	private final TerminologyServerConnectionTestStatus terminologyServerConnectionTestStatus;
	private final BundleValidatorFactory bundleValidatorFactory;

	public ProcessPluginDeploymentStateListenerImpl(DataStoreClientFactory dataStoreClientFactory,
			FttpClientFactory fttpClientFactory,
			TerminologyServerConnectionTestStatus terminologyServerConnectionTestStatus,
			BundleValidatorFactory bundleValidatorFactory)
	{
		this.dataStoreClientFactory = dataStoreClientFactory;
		this.fttpClientFactory = fttpClientFactory;
		this.terminologyServerConnectionTestStatus = terminologyServerConnectionTestStatus;
		this.bundleValidatorFactory = bundleValidatorFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dataStoreClientFactory, "dataStoreClientFactory");
		Objects.requireNonNull(fttpClientFactory, "fttpClientFactory");
		Objects.requireNonNull(terminologyServerConnectionTestStatus, "terminologyServerConnectionTestStatus");
		Objects.requireNonNull(bundleValidatorFactory, "bundleValidatorFactory");
	}

	@Override
	public void onProcessesDeployed(List<String> processes)
	{
		if (processes.contains("wwwnetzwerk-universitaetsmedizinde_dataSend")
				|| processes.contains("wwwnetzwerk-universitaetsmedizinde_dataReceive"))
		{
			dataStoreClientFactory.testConnection();
		}

		if (processes.contains("wwwnetzwerk-universitaetsmedizinde_dataSend")
				|| processes.contains("wwwnetzwerk-universitaetsmedizinde_dataTranslate"))
		{
			fttpClientFactory.testConnection();
		}

		if (processes.contains("wwwnetzwerk-universitaetsmedizinde_dataSend"))
		{

			if (TerminologyServerConnectionTestStatus.OK.equals(terminologyServerConnectionTestStatus))
				bundleValidatorFactory.init();
			else if (TerminologyServerConnectionTestStatus.NOT_OK.equals(terminologyServerConnectionTestStatus))
				logger.warn(
						"Due to an error while testing the connection to the terminology server {} was not initialized, validation of bundles will be skipped.",
						BundleValidatorFactory.class.getSimpleName());
		}
	}
}
