package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.OutcomeLogger;

public class FhirBridgeClient extends AbstractComplexFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(FhirBridgeClient.class);
	private static final OutcomeLogger outcomeLogger = new OutcomeLogger(logger);

	private static final String NUM_CODEX_BLOOD_GAS_PANEL = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-gas-panel";

	/**
	 * @param geccoClient
	 *            not <code>null</code>
	 */
	public FhirBridgeClient(GeccoClient geccoClient)
	{
		super(geccoClient);
	}

	@Override
	public void storeBundle(Bundle bundle)
	{
		// either bundle has a patient, or patient should already exists
		Patient patient = createOrUpdatePatient(bundle).orElseGet(() -> getExistingPatientOrThrow(bundle));

		Map<String, IdType> resourceIdsByUuid = new HashMap<>();
		for (int i = 0; i < bundle.getEntry().size(); i++)
		{
			BundleEntryComponent entry = bundle.getEntry().get(i);

			if (isEntrySupported(entry, e -> !(e.getResource() instanceof Patient)))
				createOrUpdateEntry(i, entry, patient, resourceIdsByUuid);

			// only log for non Patients
			else if (!entry.hasResource() || !(entry.getResource() instanceof Patient))
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
		Optional<BundleEntryComponent> bundlePatientEntry = bundle.getEntry().stream()
				.filter(entry -> isEntrySupported(entry, e -> e.getResource() instanceof Patient)).findFirst();

		if (bundlePatientEntry.isPresent())
		{
			Patient newPatient = (Patient) bundlePatientEntry.get().getResource();
			String pseudonym = getPseudonym(newPatient);
			Optional<Patient> existingPatient = findPatientInLocalFhirStore(pseudonym);

			return existingPatient
					.map(existing -> update(existing, newPatient, pseudonym, bundlePatientEntry.get().getFullUrl()))
					.orElseGet(() -> create(newPatient, pseudonym, bundlePatientEntry.get().getFullUrl()));
		}
		else
		{
			logger.debug("Bundle has no Patient");
			return Optional.empty();
		}
	}

	private Optional<Patient> update(Patient existingPatient, Patient newPatient, String pseudonym,
			String bundleFullUrl)
	{
		logger.debug("Updating Patient with pseudonym {}", pseudonym);

		newPatient.setIdElement(existingPatient.getIdElement().toVersionless());

		try
		{
			MethodOutcome outcome = geccoClient.getGenericFhirClient().update().resource(newPatient)
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
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newPatient.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not update patient {}, message: {}, status: {}, body: {}",
					newPatient.getIdElement().toString(), e.getMessage(), e.getStatusCode(), e.getResponseBody());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newPatient.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Could not update patient " + newPatient.getIdElement().toString(), e);
			throw e;
		}
	}

	private Optional<Patient> create(Patient newPatient, String pseudonym, String bundleFullUrl)
	{
		logger.debug("Creating Patient with pseudonym {}", pseudonym);

		try
		{
			MethodOutcome outcome = geccoClient.getGenericFhirClient().create().resource(newPatient)
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
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newPatient.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not create patient {}, message: {}, status: {}, body: {}",
					newPatient.getIdElement().toString(), e.getMessage(), e.getStatusCode(), e.getResponseBody());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newPatient.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Could not create patient " + newPatient.getIdElement().toString(), e);
			throw e;
		}
	}

	private void createOrUpdateEntry(int index, BundleEntryComponent entry, Patient patient,
			Map<String, IdType> resourceIdsByUuid)
	{
		Resource resource = entry.getResource();
		String url = entry.getRequest().getUrl();

		Optional<Resource> existingResource = findResourceInLocalFhirStore(url, resource.getClass());
		IdType resourceId = existingResource.map(
				existing -> update(existing, fixTemporaryReferences(resource, resourceIdsByUuid), entry.getFullUrl()))
				.orElseGet(() -> create(fixTemporaryReferences(resource, resourceIdsByUuid), entry.getFullUrl()));

		resourceIdsByUuid.put(entry.getFullUrl(), resourceId);
	}

	private Optional<Resource> findResourceInLocalFhirStore(String url, Class<? extends Resource> resourceType)
	{
		if (geccoClient.shouldUseChainedParameterNotLogicalReference())
			url = url.replace("patient:identifier", "patient.identifier");

		try
		{
			Bundle resultBundle = geccoClient.getGenericFhirClient().search().byUrl(url).sort()
					.descending("_lastUpdated").count(1).returnBundle(Bundle.class).execute();

			if (logger.isDebugEnabled())
				logger.debug("{} search-bundle result: {}", resourceType.getAnnotation(ResourceDef.class).name(),
						geccoClient.getFhirContext().newJsonParser().encodeResourceToString(resultBundle));

			if (resultBundle.getTotal() > 0)
			{
				if (resultBundle.getTotal() > 1)
					logger.warn("FHIR store has more than one Resource with url {}, using last updated", url);

				if (resultBundle.getEntryFirstRep().hasResource()
						&& resourceType.isInstance(resultBundle.getEntryFirstRep().getResource()))
					return Optional.of(resultBundle.getEntryFirstRep().getResource());
				else
				{
					logger.warn("Error while searching for Resource with url {}, bundle has no {} resource", url,
							resourceType.getAnnotation(ResourceDef.class).name());

					if (resultBundle.getEntryFirstRep().getResponse().hasOutcome()
							&& resultBundle.getEntryFirstRep().getResponse().getOutcome() instanceof OperationOutcome)
						outcomeLogger.logOutcome(
								(OperationOutcome) resultBundle.getEntryFirstRep().getResponse().getOutcome());

					return Optional.empty();
				}
			}
			else
			{
				logger.debug("FHIR store has no Resource with url {}", url);
				return Optional.empty();
			}
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Error while searching for Resource with url {}, message: {}, status: {}", url, e.getMessage(),
					e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Error while searching for Resource with url {}, message: {}, status: {}", url, e.getMessage(),
					e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
				outcomeLogger.logOutcome((OperationOutcome) outcome);

			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Error while searching for Resource with url " + url, e);
			throw e;
		}
	}

	private Resource fixTemporaryReferences(Resource resource, Map<String, IdType> resourceIdsByUuid)
	{
		if (resource == null)
			return null;

		else if (resource instanceof Observation)
		{
			if (resource.getMeta().getProfile().stream().map(CanonicalType::getValue)
					.anyMatch(url -> NUM_CODEX_BLOOD_GAS_PANEL.equals(url)
							|| (url != null && url.startsWith(NUM_CODEX_BLOOD_GAS_PANEL + "|"))))
			{
				Observation observation = (Observation) resource;
				List<Reference> members = observation.getHasMember();
				for (int i = 0; i < members.size(); i++)
				{
					Reference member = members.get(i);
					if (member.hasReference())
					{
						String uuid = member.getReference();
						IdType resourceId = resourceIdsByUuid.get(uuid);

						if (resourceId != null)
						{
							logger.debug(
									"Replacing reference at Observation.hasMember[{}] from bundle resource {} with existing resource id",
									i, resource.getIdElement().getValue());
							member.setReferenceElement(resourceId);
						}
					}
				}
			}
		}

		return resource;
	}

	private IdType update(Resource existingResource, Resource newResource, String bundleFullUrl)
	{
		logger.debug("Updating {}", newResource.getResourceType().name());

		newResource.setIdElement(existingResource.getIdElement().toVersionless());

		try
		{
			MethodOutcome outcome = geccoClient.getGenericFhirClient().update().resource(newResource)
					.prefer(PreferReturnEnum.MINIMAL).execute();

			if (outcome.getId() == null)
			{
				logger.warn("Could not update {} {}: unknown reason", newResource.getResourceType().name(),
						newResource.getIdElement().toString());
				if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
					outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());

				throw new RuntimeException("Count not update " + newResource.getResourceType().name() + " "
						+ newResource.getIdElement().toString());
			}
			else if (outcome.getOperationOutcome() != null && outcome.getOperationOutcome() instanceof OperationOutcome)
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());
				logger.warn("Could not update {} {}: unknown reason", newResource.getResourceType().name(),
						newResource.getIdElement().toString());
				throw new RuntimeException("Could not create " + newResource.getResourceType().name() + " "
						+ newResource.getIdElement().toString() + ": unknown reason");
			}
			else
				return (IdType) outcome.getId();
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Could not update {} {}, message: {}, status: {}", newResource.getResourceType().name(),
					newResource.getIdElement().toString(), e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newResource.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not update {} {}, message: {}, status: {}, body: {}",
					newResource.getResourceType().name(), newResource.getIdElement().toString(), e.getMessage(),
					e.getStatusCode(), e.getResponseBody());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newResource.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Count not update " + newResource.getResourceType().name() + " "
					+ newResource.getIdElement().toString(), e);
			throw e;
		}
	}

	private IdType create(Resource newResource, String bundleFullUrl)
	{
		logger.debug("Creating {}", newResource.getResourceType().name());

		try
		{
			MethodOutcome outcome = geccoClient.getGenericFhirClient().create().resource(newResource)
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
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome.getOperationOutcome());
				logger.warn("Could not create {} {}: unknown reason", newResource.getResourceType().name(),
						newResource.getIdElement().toString());
				throw new RuntimeException("Could not create " + newResource.getResourceType().name() + " "
						+ newResource.getIdElement().toString() + ": unknown reason");
			}
			else
				return (IdType) outcome.getId();
		}
		catch (UnprocessableEntityException e)
		{
			logger.warn("Could not create {} {}, message: {}, status: {}", newResource.getResourceType().name(),
					newResource.getIdElement().toString(), e.getMessage(), e.getStatusCode());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newResource.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

			throw e;
		}
		catch (BaseServerResponseException e)
		{
			logger.warn("Could not create {} {}, message: {}, status: {}, body: {}",
					newResource.getResourceType().name(), newResource.getIdElement().toString(), e.getMessage(),
					e.getStatusCode(), e.getResponseBody());

			IBaseOperationOutcome outcome = e.getOperationOutcome();

			if (outcome != null && outcome instanceof OperationOutcome)
			{
				outcomeLogger.logOutcome((OperationOutcome) outcome);
				throw new ValidationException(newResource.getResourceType().name(), bundleFullUrl,
						(OperationOutcome) outcome);
			}

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
