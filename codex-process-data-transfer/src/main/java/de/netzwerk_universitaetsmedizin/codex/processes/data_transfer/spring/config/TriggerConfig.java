package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartSendProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.FindNewData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.SaveLastExportTo;
import dev.dsf.bpe.v1.ProcessPluginApi;

@Configuration
public class TriggerConfig
{
	@Autowired
	private ProcessPluginApi api;

	@Autowired
	private TransferDataConfig transferDataConfig;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public FindNewData findNewData()
	{
		return new FindNewData(api, transferDataConfig.dataStoreClientFactory());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StartSendProcess startSendProcess()
	{
		return new StartSendProcess(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SaveLastExportTo saveLastExportTo()
	{
		return new SaveLastExportTo(api);
	}
}
