package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.OutcomeLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;

public abstract class AbstractComplexFhirClient extends AbstractFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractComplexFhirClient.class);
	private static final OutcomeLogger outcomeLogger = new OutcomeLogger(logger);

	/**
	 * @param dataClient
	 *            not <code>null</code>
	 * @param dataLogger
	 *            not <code>null</code>
	 */
	public AbstractComplexFhirClient(DataStoreClient dataClient, DataLogger dataLogger)
	{
		super(dataClient, dataLogger);
	}

	protected Resource setSubject(Resource resource, Reference patientRef)
	{
		if (resource instanceof Condition c)
			c.setSubject(patientRef);
		else if (resource instanceof Consent c)
			c.setPatient(patientRef);
		else if (resource instanceof DiagnosticReport dr)
			dr.setSubject(patientRef);
		else if (resource instanceof Encounter e)
			e.setSubject(patientRef);
		else if (resource instanceof Immunization i)
			i.setPatient(patientRef);
		else if (resource instanceof Medication)
			; // nothing to do
		else if (resource instanceof MedicationAdministration ma)
			ma.setSubject(patientRef);
		else if (resource instanceof MedicationStatement ms)
			ms.setSubject(patientRef);
		else if (resource instanceof Observation o)
			o.setSubject(patientRef);
		else if (resource instanceof Procedure p)
			p.setSubject(patientRef);
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");

		return resource;
	}

	protected Optional<Reference> getSubject(Resource resource)
	{
		if (resource instanceof Condition c)
			return Optional.of(c.getSubject());
		else if (resource instanceof Consent c)
			return Optional.of(c.getPatient());
		else if (resource instanceof DiagnosticReport dr)
			return Optional.of(dr.getSubject());
		else if (resource instanceof Encounter e)
			return Optional.of(e.getSubject());
		else if (resource instanceof Immunization i)
			return Optional.of(i.getPatient());
		else if (resource instanceof Medication)
			return Optional.empty();
		else if (resource instanceof MedicationAdministration ma)
			return Optional.of(ma.getSubject());
		else if (resource instanceof MedicationStatement ms)
			return Optional.of(ms.getSubject());
		else if (resource instanceof Observation o)
			return Optional.of(o.getSubject());
		else if (resource instanceof Procedure p)
			return Optional.of(p.getSubject());
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
	}

	protected String findPseudonym(Bundle bundle)
	{
		Optional<String> opt = bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).map(this::getSubject).flatMap(Optional::stream)
				.filter(Reference::hasIdentifier).map(Reference::getIdentifier)
				.filter(i -> NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM.equals(i.getSystem())).map(Identifier::getValue)
				.findFirst();

		if (opt.isEmpty() && bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).anyMatch(r -> !(r instanceof Medication)))
			throw new RuntimeException("No resource in Bundle has subject with pseudonym");
		else
			return opt.get();
	}

	protected String getPseudonym(Patient patient)
	{
		Objects.requireNonNull(patient, "patient");

		return patient.getIdentifier().stream().filter(Identifier::hasValue).filter(Identifier::hasSystem)
				.filter(i -> NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM.equals(i.getSystem())).findFirst()
				.map(Identifier::getValue).orElseThrow(() -> new RuntimeException("Patient has no pseudonym"));
	}

	protected Optional<Patient> findPatientInLocalFhirStore(String pseudonym)
	{
		try
		{
			Bundle patientBundle = dataClient
					.getGenericFhirClient().search().forResource(Patient.class).where(Patient.IDENTIFIER.exactly()
							.systemAndIdentifier(NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM, pseudonym))
					.sort().descending("_lastUpdated").count(1).returnBundle(Bundle.class).execute();

			dataLogger.logData("Patient search-bundle result", patientBundle);

			if (patientBundle.getTotal() > 0)
			{
				if (patientBundle.getTotal() > 1)
					logger.warn("FHIR store has more than one Patient with pseudonym {}|{}, using last updated",
							NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM, pseudonym);

				if (patientBundle.getEntryFirstRep().hasResource()
						&& patientBundle.getEntryFirstRep().getResource() instanceof Patient)
					return Optional.of((Patient) patientBundle.getEntryFirstRep().getResource());
				else
				{
					logger.warn("Error while search for Patient with pseudonym {}|{}, bundle has no Patient resource",
							NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM, pseudonym);

					if (patientBundle.getEntryFirstRep().getResponse().hasOutcome()
							&& patientBundle.getEntryFirstRep().getResponse().getOutcome() instanceof OperationOutcome)
						outcomeLogger.logOutcome(
								(OperationOutcome) patientBundle.getEntryFirstRep().getResponse().getOutcome());

					return Optional.empty();
				}
			}
			else
			{
				logger.info("FHIR store has no Patient with pseudonym {}|{}", NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM,
						pseudonym);
				return Optional.empty();
			}
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Error while searching for Patient with pseudonym {}|{}, message: {}, status: {}",
					NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM, pseudonym, e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Error while searching for Patient with pseudonym {}|{}, message: {}, status: {}",
					NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM, pseudonym, e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Error while searching for Patient with pseudonym {}|{}", NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM,
					pseudonym, e);
			throw e;
		}
	}

	@Override
	public Stream<DomainResource> getNewData(String pseudonym, DateWithPrecision exportFrom, Date exportTo)
	{
		Optional<Patient> localPatient = findPatientInLocalFhirStore(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM, pseudonym);
		if (localPatient.isEmpty())
		{
			logger.warn(
					"Error while retrieving patient for pseudonym {}, result bundle total not 1 or first entry not patient",
					pseudonym);
			throw new RuntimeException("Error while retrieving patient for pseudonym " + pseudonym);
		}

		Bundle searchBundle = getSearchBundleWithPatientId(localPatient.get().getIdElement().getIdPart(), exportFrom,
				exportTo);

		dataLogger.logData("Executing Search-Bundle", searchBundle);

		Bundle resultBundle = dataClient.getGenericFhirClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		dataLogger.logData("Search-Bundle result", resultBundle);

		return distinctById(Stream.concat(Stream.of(localPatient.get()),
				resultBundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
						.map(BundleEntryComponent::getResource).filter(r -> r instanceof Bundle).map(r -> (Bundle) r)
						.flatMap(this::getDomainResources)));
	}

	private Optional<Patient> findPatientInLocalFhirStore(String system, String pseudonym)
	{
		Bundle patientBundle = (Bundle) dataClient.getGenericFhirClient().search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly().systemAndIdentifier(system, pseudonym)).execute();

		dataLogger.logData("Patient search-bundle result", patientBundle);

		if (patientBundle.getTotal() != 1 || !(patientBundle.getEntryFirstRep().getResource() instanceof Patient))
			return Optional.empty();
		else
			return Optional.of((Patient) patientBundle.getEntryFirstRep().getResource());
	}
}
