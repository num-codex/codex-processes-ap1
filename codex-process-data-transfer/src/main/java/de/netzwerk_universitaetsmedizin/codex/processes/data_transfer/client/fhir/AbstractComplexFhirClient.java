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
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.OutcomeLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.domain.DateWithPrecision;

public abstract class AbstractComplexFhirClient extends AbstractFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractComplexFhirClient.class);
	private static final OutcomeLogger outcomeLogger = new OutcomeLogger(logger);

	/**
	 * @param geccoClient
	 *            not <code>null</code>
	 */
	public AbstractComplexFhirClient(GeccoClient geccoClient)
	{
		super(geccoClient);
	}

	protected Resource setSubject(Resource resource, Reference patientRef)
	{
		if (resource instanceof Condition)
		{
			((Condition) resource).setSubject(patientRef);
			return resource;
		}
		else if (resource instanceof Consent)
		{
			((Consent) resource).setPatient(patientRef);
			return resource;
		}
		else if (resource instanceof DiagnosticReport)
		{
			((DiagnosticReport) resource).setSubject(patientRef);
			return resource;
		}
		else if (resource instanceof Immunization)
		{
			((Immunization) resource).setPatient(patientRef);
			return resource;
		}
		else if (resource instanceof MedicationStatement)
		{
			((MedicationStatement) resource).setSubject(patientRef);
			return resource;
		}
		else if (resource instanceof Observation)
		{
			((Observation) resource).setSubject(patientRef);
			return resource;
		}
		else if (resource instanceof Procedure)
		{
			((Procedure) resource).setSubject(patientRef);
			return resource;
		}
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
	}

	protected Reference getSubject(Resource resource)
	{
		if (resource instanceof Condition)
			return ((Condition) resource).getSubject();
		else if (resource instanceof Consent)
			return ((Consent) resource).getPatient();
		else if (resource instanceof DiagnosticReport)
			return ((DiagnosticReport) resource).getSubject();
		else if (resource instanceof Immunization)
			return ((Immunization) resource).getPatient();
		else if (resource instanceof MedicationStatement)
			return ((MedicationStatement) resource).getSubject();
		else if (resource instanceof Observation)
			return ((Observation) resource).getSubject();
		else if (resource instanceof Procedure)
			return ((Procedure) resource).getSubject();
		else
			throw new RuntimeException("Resource of type " + resource.getResourceType().name() + " not supported");
	}

	protected String findPseudonym(Bundle bundle)
	{
		return bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).map(this::getSubject).filter(Reference::hasIdentifier)
				.map(Reference::getIdentifier).filter(i -> NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst()
				.orElseThrow(() -> new RuntimeException("No resource in Bundle has subject with pseudonym"));
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
			Bundle patientBundle = geccoClient
					.getGenericFhirClient().search().forResource(Patient.class).where(Patient.IDENTIFIER.exactly()
							.systemAndIdentifier(NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM, pseudonym))
					.sort().descending("_lastUpdated").count(1).returnBundle(Bundle.class).execute();

			if (logger.isDebugEnabled())
				logger.debug("Patient search-bundle result: {}",
						geccoClient.getFhirContext().newJsonParser().encodeResourceToString(patientBundle));

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
			logger.warn("Error while searching for Patient with pseudonym " + NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM
					+ "|" + pseudonym, e);
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

		if (logger.isDebugEnabled())
			logger.debug("Executing Search-Bundle: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(searchBundle));

		Bundle resultBundle = geccoClient.getGenericFhirClient().transaction().withBundle(searchBundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();

		if (logger.isDebugEnabled())
			logger.debug("Search-Bundle result: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(resultBundle));

		return distinctById(Stream.concat(Stream.of(localPatient.get()),
				resultBundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
						.map(BundleEntryComponent::getResource).filter(r -> r instanceof Bundle).map(r -> (Bundle) r)
						.flatMap(this::getDomainResources)));
	}

	private Optional<Patient> findPatientInLocalFhirStore(String system, String pseudonym)
	{
		Bundle patientBundle = (Bundle) geccoClient.getGenericFhirClient().search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly().systemAndIdentifier(system, pseudonym)).execute();

		if (logger.isDebugEnabled())
			logger.debug("Patient search-bundle result: {}",
					geccoClient.getFhirContext().newJsonParser().encodeResourceToString(patientBundle));

		if (patientBundle.getTotal() != 1 || !(patientBundle.getEntryFirstRep().getResource() instanceof Patient))
			return Optional.empty();
		else
			return Optional.of((Patient) patientBundle.getEntryFirstRep().getResource());
	}

}
