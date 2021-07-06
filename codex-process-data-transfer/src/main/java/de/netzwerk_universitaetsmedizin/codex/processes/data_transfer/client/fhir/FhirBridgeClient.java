package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.HapiFhirClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.OutcomeLogger;

public class FhirBridgeClient extends AbstractComplexFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(FhirBridgeClient.class);
	private static final OutcomeLogger outcomeLogger = new OutcomeLogger(logger);

	public static final String RFC_4122_SYSTEM = "urn:ietf:rfc:4122";

	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param clientFactory
	 *            not <code>null</code>
	 * @param searchBundleOverride
	 *            may be <code>null</code>
	 */
	public FhirBridgeClient(FhirContext fhirContext, HapiFhirClientFactory clientFactory, Path searchBundleOverride)
	{
		super(fhirContext, clientFactory, searchBundleOverride);
	}

	@Override
	public void storeBundle(Bundle bundle)
	{
		// either bundle has a patient, or patient should already exists
		Patient patient = createOrUpdatePatient(bundle).orElseGet(() -> getExistingPatientOrThrow(bundle));

		for (int i = 0; i < bundle.getEntry().size(); i++)
		{
			BundleEntryComponent entry = bundle.getEntry().get(i);

			if (isEntrySupported(entry, e -> !(e.getResource() instanceof Patient)))
				createOrUpdateEntry(i, entry, patient);
			else if (!entry.hasResource() || !(entry.getResource() instanceof Patient))
				// only log for non Patients
				logger.warn("Bundle entry at index {} not supported, ignoring entry", i);
		}
	}

	private Patient getExistingPatientOrThrow(Bundle bundle)
	{
		String pseudonym = findPseudonym(bundle);

		return findPatientInLocalFhirStore(pseudonym).orElseThrow(() -> new RuntimeException(
				"FHIR store has no Patient with pseudonym " + NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM + "|" + pseudonym));
	}

	private boolean isEntrySupported(BundleEntryComponent entry, Predicate<BundleEntryComponent> test)
	{
		return entry.hasFullUrl() && entry.getFullUrl().startsWith("urn:uuid:") && entry.hasRequest()
				&& entry.getRequest().hasMethod() && HTTPVerb.PUT.equals(entry.getRequest().getMethod())
				&& entry.getRequest().hasUrl() && entry.hasResource() && test.test(entry);
	}

	private Optional<Patient> createOrUpdatePatient(Bundle bundle)
	{
		Optional<Patient> bundlePatient = bundle.getEntry().stream()
				.filter(entry -> isEntrySupported(entry, e -> e.getResource() instanceof Patient)).findFirst()
				.map(e -> (Patient) e.getResource());

		if (bundlePatient.isPresent())
		{
			String pseudonym = getPseudonym(bundlePatient.get());
			Optional<Patient> existingPatient = findPatientInLocalFhirStore(pseudonym);

			return existingPatient.map(existing -> update(existing, bundlePatient.get(), pseudonym))
					.orElseGet(() -> create(bundlePatient.get(), pseudonym));
		}
		else
		{
			logger.info("Bundle has no Patient");
			return Optional.empty();
		}
	}

	private Optional<Patient> update(Patient existingPatient, Patient newPatient, String pseudonym)
	{
		logger.debug("Updating Patient with pseudonym {}", pseudonym);

		newPatient.setIdElement(existingPatient.getIdElement().toVersionless());

		try
		{
			MethodOutcome outcome = clientFactory.getFhirStoreClient().update().resource(newPatient)
					.prefer(PreferReturnEnum.REPRESENTATION).preferResponseType(Patient.class).execute();

			if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());

			if (outcome.getResource() != null && outcome.getResource() instanceof Patient)
				return Optional.of((Patient) outcome.getResource());
			else
			{
				logger.warn("Could not update patient {}", newPatient.getIdElement().toString());
				if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
					outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());

				return Optional.empty();
			}
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Could not update patient {}, message: {}, status: {}", newPatient.getIdElement().toString(),
					e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not update patient {}, message: {}, status: {}, body: {}",
					newPatient.getIdElement().toString(), e.getMessage(), e.getStatusCode(), e.getResponseBody());
			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Could not update patient " + newPatient.getIdElement().toString(), e);
			throw e;
		}
	}

	private Optional<Patient> create(Patient newPatient, String pseudonym)
	{
		logger.debug("Creating Patient with pseudonym {}", pseudonym);

		try
		{
			MethodOutcome outcome = clientFactory.getFhirStoreClient().create().resource(newPatient)
					.prefer(PreferReturnEnum.REPRESENTATION).preferResponseType(Patient.class).execute();

			if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());

			if (Boolean.TRUE.equals(outcome.getCreated()) && outcome.getResource() != null
					&& outcome.getResource() instanceof Patient)
				return Optional.of((Patient) outcome.getResource());
			else
			{
				logger.warn("Could not create patient {}", newPatient.getIdElement().toString());
				if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
					outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());

				return Optional.empty();
			}
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Could not create patient {}, message: {}, status: {}", newPatient.getIdElement().toString(),
					e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not create patient {}, message: {}, status: {}, body: {}",
					newPatient.getIdElement().toString(), e.getMessage(), e.getStatusCode(), e.getResponseBody());
			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Could not create patient " + newPatient.getIdElement().toString(), e);
			throw e;
		}
	}

	private void createOrUpdateEntry(int index, BundleEntryComponent entry, Patient patient)
	{
		Resource resource = entry.getResource();
		String url = entry.getRequest().getUrl();

		Optional<Resource> existingResource = findResourceInLocalFhirStore(url, resource.getClass());
		existingResource.ifPresentOrElse(existing -> update(existing, resource), () -> create(resource));
	}

	private Optional<Resource> findResourceInLocalFhirStore(String url, Class<? extends Resource> resourceType)
	{
		try
		{
			Bundle patientBundle = clientFactory.getFhirStoreClient().search().byUrl(url).sort()
					.descending("_lastUpdated").count(1).returnBundle(Bundle.class).execute();

			if (logger.isDebugEnabled())
				logger.debug("{} search-bundle result: {}", resourceType.getAnnotation(ResourceDef.class).name(),
						fhirContext.newJsonParser().encodeResourceToString(patientBundle));

			if (patientBundle.getTotal() > 0)
			{
				if (patientBundle.getTotal() > 1)
					logger.warn("FHIR store has more than one Resource with url {}, using last updated", url);

				if (patientBundle.getEntryFirstRep().hasResource()
						&& resourceType.isInstance(patientBundle.getEntryFirstRep().getResource()))
					return Optional.of((Patient) patientBundle.getEntryFirstRep().getResource());
				else
				{
					logger.warn("Error while search for Resource with url {}, bundle has no {} resource", url,
							resourceType.getAnnotation(ResourceDef.class).name());

					if (patientBundle.getEntryFirstRep().getResponse().hasOutcome()
							&& patientBundle.getEntryFirstRep().getResponse().getOutcome() instanceof OperationOutcome)
						outcomeLogger.logOutcome(
								(OperationOutcome) patientBundle.getEntryFirstRep().getResponse().getOutcome());

					return Optional.empty();
				}
			}
			else
			{
				logger.info("FHIR store has no Resource with url {}", url);
				return Optional.empty();
			}
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Error while search for Resource with url {}, message: {}, status: {}", url, e.getMessage(),
					e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Error while search for Resource with url {}, message: {}, status: {}", url, e.getMessage(),
					e.getStatusCode());
			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Error while search for Resource with url " + url, e);
			throw e;
		}
	}

	private void update(Resource existingResource, Resource newResource)
	{
		logger.debug("Updating {}", newResource.getResourceType().name());

		newResource.setIdElement(existingResource.getIdElement().toVersionless());

		try
		{
			MethodOutcome outcome = clientFactory.getFhirStoreClient().update().resource(newResource)
					.prefer(PreferReturnEnum.MINIMAL).execute();

			if (outcome.getId() == null)
			{
				logger.warn("Could not update {} {}", newResource.getResourceType().name(),
						newResource.getIdElement().toString());
				if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
					outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());

				throw new RuntimeException("Count not update " + newResource.getResourceType().name() + " "
						+ newResource.getIdElement().toString());
			}
			else if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Could not update {} {}, message: {}, status: {}", newResource.getResourceType().name(),
					newResource.getIdElement().toString(), e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not update {} {}, message: {}, status: {}, body: {}",
					newResource.getResourceType().name(), newResource.getIdElement().toString(), e.getMessage(),
					e.getStatusCode(), e.getResponseBody());
			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Count not update " + newResource.getResourceType().name() + " "
					+ newResource.getIdElement().toString(), e);
			throw e;
		}
	}

	private void create(Resource newResource)
	{
		logger.debug("Creating {}", newResource.getResourceType().name());

		try
		{
			MethodOutcome outcome = clientFactory.getFhirStoreClient().create().resource(newResource)
					.prefer(PreferReturnEnum.MINIMAL).execute();

			if (!Boolean.TRUE.equals(outcome.getCreated()) || outcome.getId() == null)
			{
				logger.warn("Could not create {} {}", newResource.getResourceType().name(),
						newResource.getIdElement().toString());
				if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
					outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());

				throw new RuntimeException("Count not create " + newResource.getResourceType().name() + " "
						+ newResource.getIdElement().toString());
			}
			else if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Could not create {} {}, message: {}, status: {}", newResource.getResourceType().name(),
					newResource.getIdElement().toString(), e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not create {} {}, message: {}, status: {}, body: {}",
					newResource.getResourceType().name(), newResource.getIdElement().toString(), e.getMessage(),
					e.getStatusCode(), e.getResponseBody());
			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Could not create " + newResource.getResourceType().name() + " "
					+ newResource.getIdElement().toString(), e);
			throw e;
		}
	}
}
