package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.listener.ProcessPluginDeploymentStateListenerImpl;
import dev.dsf.bpe.v1.ProcessPluginDeploymentStateListener;

@Configuration
public class ProcessPluginDeploymentConfig
{
	@Autowired
	private TransferDataConfig transferDataConfig;

	@Autowired
	private ValidationConfig validationConfig;

	@Bean
	public ProcessPluginDeploymentStateListener pluginDeploymentStateListener()
	{
		return new ProcessPluginDeploymentStateListenerImpl(transferDataConfig.dataStoreClientFactory(),
				transferDataConfig.fttpClientFactory(), validationConfig.testConnectionToTerminologyServer(),
				validationConfig.bundleValidatorFactory());
	}
}
