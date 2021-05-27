package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_RECORD_BLOOM_FILTER;

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

import ca.uhn.fhir.rest.api.MethodOutcome;
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

		Patient patient = getPatient(reference);
		String pseudonym = getPseudonym(patient).or(() -> resolvePseudonym(patient))
				.orElseThrow(() -> new RuntimeException("Could not resolve DIC pseudonym")).getValue();

		logger.info("Absolut patient reference {} has DIC pseudonym {}", reference, pseudonym);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM, Variables.stringValue(pseudonym));
	}

	private Patient getPatient(String reference)
	{
		return fhirClientFactory.getFhirClient().getPatient(reference);
	}

	private Optional<Identifier> getPseudonym(Patient patient)
	{
		return patient.getIdentifier().stream().filter(i -> i.getSystem().equals(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM))
				.findFirst();
	}

	private Optional<Identifier> resolvePseudonym(Patient patient)
	{
		String bloomFilter = getBloomFilter(patient);
		String pseudonym = resolveBloomFilter(bloomFilter);
		Patient updatedPatient = storePseudonym(patient, pseudonym);

		return getPseudonym(updatedPatient);
	}

	private String getBloomFilter(Patient patient)
	{
		return patient.getIdentifier().stream().filter(Identifier::hasSystem)
				.filter(identifier -> identifier.getSystem().equals(NAMING_SYSTEM_NUM_CODEX_RECORD_BLOOM_FILTER))
				.findFirst().orElseThrow(() -> new RuntimeException("No bloom filter present in patient")).getValue();
	}

	private String resolveBloomFilter(String bloomFilter)
	{
		return fttpClientFactory.getFttpClient().getDicPseudonym(bloomFilter)
				.orElseThrow(() -> new RuntimeException("Could not resolve bloom filter to DIC pseudonym"));
	}

	private Patient storePseudonym(Patient patient, String pseudonym)
	{
		logger.info("Storing DIC pseudonym for absolut patient reference {}",
				patient.getIdElement().toVersionless().getValue());

		patient.addIdentifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue(pseudonym);
		MethodOutcome outcome = fhirClientFactory.getFhirClient().updatePatient(patient);

		return (Patient) outcome.getResource();
	}
}
