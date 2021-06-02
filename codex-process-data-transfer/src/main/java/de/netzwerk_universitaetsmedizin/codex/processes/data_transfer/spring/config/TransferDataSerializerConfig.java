package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceListSerializer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceSerializer;

@Configuration
public class TransferDataSerializerConfig
{
	@Autowired
	private ObjectMapper objectMapper;

	@Bean
	public PatientReferenceSerializer patientReferenceSerializer()
	{
		return new PatientReferenceSerializer(objectMapper);
	}

	@Bean
	public PatientReferenceListSerializer patientReferenceListSerializer()
	{
		return new PatientReferenceListSerializer(objectMapper);
	}
}
