package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.message.StartSendProcess;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.FindNewData;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.trigger.SaveLastExportTo;

@Configuration
public class TriggerConfig
{
	@Autowired
	private TransferDataConfig transferDataConfig;

	@Bean
	public FindNewData findNewData()
	{
		return new FindNewData(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.organizationProvider(),
				transferDataConfig.endpointProvider(), transferDataConfig.geccoClientFactory());
	}

	@Bean
	public StartSendProcess startSendProcess()
	{
		return new StartSendProcess(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper(), transferDataConfig.organizationProvider(),
				transferDataConfig.fhirContext());
	}

	@Bean
	public SaveLastExportTo saveLastExportTo()
	{
		return new SaveLastExportTo(transferDataConfig.fhirClientProvider(), transferDataConfig.taskHelper(),
				transferDataConfig.readAccessHelper());
	}
}
