package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceListSerializer;

@Configuration
public class TransferDataSerializerConfig
{
	@Autowired
	private ObjectMapper objectMapper;

	@Bean
	public PatientReferenceListSerializer pseudonymListSerializer()
	{
		return new PatientReferenceListSerializer(objectMapper);
	}
}
