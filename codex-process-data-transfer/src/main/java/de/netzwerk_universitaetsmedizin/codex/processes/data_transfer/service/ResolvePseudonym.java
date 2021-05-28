package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_BLOOM_FILTER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;

public class ResolvePseudonym extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ResolvePseudonym.class);

	private final FhirClientFactory fhirClientFactory;
	private final FttpClientFactory fttpClientFactory;

	public ResolvePseudonym(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			FhirClientFactory fhirClientFactory, FttpClientFactory fttpClientFactory)
	{
		super(clientProvider, taskHelper);

		this.fhirClientFactory = fhirClientFactory;
		this.fttpClientFactory = fttpClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirClientFactory, "fhirClientFactory");
		Objects.requireNonNull(fttpClientFactory, "fttpClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws Exception
	{
		String reference = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE);

		logger.info("Resolving DIC pseudonym for absolut patient reference {}", reference);

		Optional<Patient> optPatient = getPatient(reference);
		optPatient.ifPresentOrElse(patient ->
		{
			Optional<String> optPseudonym = getPseudonym(patient);
			optPseudonym.ifPresentOrElse(pseudonym ->
			{
				logger.debug("Patient with absolute reference {} has DIC pseudonym {}", reference, pseudonym);
				execution.setVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM, Variables.stringValue(pseudonym));
			}, () ->
			{
				logger.debug("Patient with absolute reference {} has no DIC pseudonym", reference);
				Patient updatedPatient = resolvePseudonymAndUpdatePatient(optPatient.get());
				String pseudonym = getPseudonym(updatedPatient).orElseThrow();
				execution.setVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM, Variables.stringValue(pseudonym));
			});
		}, () ->
		{
			logger.error("Patient with absolute reference {} not found", reference);
			throw new RuntimeException("Patient with absolute reference " + reference + " not found");
		});
	}

	private Optional<Patient> getPatient(String reference)
	{
		return fhirClientFactory.getFhirClient().getPatient(reference);
	}

	private Optional<String> getPseudonym(Patient patient)
	{
		return patient.getIdentifier().stream().filter(i -> i.getSystem().equals(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM))
				.findFirst().map(Identifier::getValue);
	}

	private Patient resolvePseudonymAndUpdatePatient(Patient patient)
	{
		String bloomFilter = getBloomFilter(patient);
		String pseudonym = resolveBloomFilter(bloomFilter);
		return storePseudonym(patient, pseudonym);
	}

	private String getBloomFilter(Patient patient)
	{
		return patient.getIdentifier().stream().filter(Identifier::hasSystem)
				.filter(i -> NAMING_SYSTEM_NUM_CODEX_BLOOM_FILTER.equals(i.getSystem()) && i.hasValue()).findFirst()
				.map(Identifier::getValue)
				.orElseThrow(() -> new RuntimeException("No bloom filter present in patient"));
	}

	private String resolveBloomFilter(String bloomFilter)
	{
		return fttpClientFactory.getFttpClient().getDicPseudonym(bloomFilter)
				.orElseThrow(() -> new RuntimeException("Could not get DIC pseudonym with bloom filter"));
	}

	private Patient storePseudonym(Patient patient, String pseudonym)
	{
		logger.info("Storing DIC pseudonym patient with absolute reference {}",
				patient.getIdElement().toVersionless().getValue());
		patient.addIdentifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym);

		Optional<Patient> updatePatient = fhirClientFactory.getFhirClient().updatePatient(patient);

		return updatePatient.orElseThrow(() -> new RuntimeException("Unable to update Patient with absolute reference "
				+ patient.getIdElement().toVersionless().getValue()));
	}
}
