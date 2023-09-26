package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceListSerializer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceSerializer;
import dev.dsf.bpe.v1.ProcessPluginApi;

@Configuration
public class TransferDataSerializerConfig
{
	@Autowired
	private ProcessPluginApi api;

	@Bean
	public PatientReferenceSerializer patientReferenceSerializer()
	{
		return new PatientReferenceSerializer(api.getObjectMapper());
	}

	@Bean
	public PatientReferenceListSerializer patientReferenceListSerializer()
	{
		return new PatientReferenceListSerializer(api.getObjectMapper());
	}
}
