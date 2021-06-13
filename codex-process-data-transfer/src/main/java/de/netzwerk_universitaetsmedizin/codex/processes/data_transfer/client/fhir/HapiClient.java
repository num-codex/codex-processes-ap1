package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir;

import java.nio.file.Path;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.HapiFhirClientFactory;

public class HapiClient extends AbstractComplexFhirClient
{
	static final Logger logger = LoggerFactory.getLogger(HapiClient.class);

	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param clientFactory
	 *            not <code>null</code>
	 * @param searchBundleOverride
	 *            may be <code>null</code>
	 */
	public HapiClient(FhirContext fhirContext, HapiFhirClientFactory clientFactory, Path searchBundleOverride)
	{
		super(fhirContext, clientFactory, searchBundleOverride);
	}

	@Override
	public void storeBundle(Bundle bundle)
	{
		modifyBundle(bundle);

		clientFactory.getFhirStoreClient().transaction().withBundle(bundle)
				.withAdditionalHeader(Constants.HEADER_PREFER, "handling=strict").execute();
	}

	private void modifyBundle(Bundle bundle)
	{
		// bundle has patient
		// - db has patient by pseudonym -> update references, modify conditions
		// - db does not have patient -> remove patient condition
		// bundle has no patient, select from first resource by ref
		// - db has patient by pseudonym -> update references, modify conditions
		// - error

		Optional<Patient> bundlePatient = bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof Patient).map(r -> (Patient) r)
				.findFirst();

		if (bundlePatient.isPresent())
		{
			String pseudonym = getPseudonym(bundlePatient.get());

			findPatientInLocalFhirStore(pseudonym).ifPresentOrElse(patient ->
			{
				String localPatientId = patient.getIdElement().getIdPart();
				modifyBundleWithPatientId(bundle, pseudonym, localPatientId);
			}, () ->
			{
				String tempId = bundlePatient.get().getIdElement().getIdPart();
				modifyBundleWithTempPatientId(bundle, pseudonym, tempId);
			});
		}
		else
		{
			String pseudonym = findPseudonym(bundle);

			findPatientInLocalFhirStore(pseudonym).ifPresentOrElse(patient ->
			{
				String localPatientid = patient.getIdElement().getIdPart();
				modifyBundleWithPatientId(bundle, pseudonym, localPatientid);
			}, () ->
			{
				logger.warn(
						"Bundle does not contain Patient, and Patient with pseudonym {} not found in local fhir store",
						pseudonym);
				throw new RuntimeException(
						"Bundle has no patient and local fhir store has no patient with pseudonym " + pseudonym);
			});
		}

		if (logger.isDebugEnabled())
			logger.debug("Modified bundle: {}", fhirContext.newJsonParser().encodeResourceToString(bundle));
	}

	private void modifyBundleWithPatientId(Bundle bundle, String pseudonym, String patientId)
	{
		bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.filter(e -> !(e.getResource() instanceof Patient)).forEach(e ->
				{
					setSubject(e.getResource(), new Reference(new IdType("Patient", patientId)));
					modifyConditionalUpdateUrl(e, pseudonym, "&patient=Patient/" + patientId);
				});
	}

	private void modifyBundleWithTempPatientId(Bundle bundle, String pseudonym, String tempId)
	{
		bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.filter(e -> !(e.getResource() instanceof Patient)).forEach(e ->
				{
					setSubject(e.getResource(), new Reference(tempId));
					modifyConditionalUpdateUrl(e, pseudonym, "");
				});
	}

	private void modifyConditionalUpdateUrl(BundleEntryComponent entry, String pseudonym, String replacement)
	{
		String url = entry.getRequest().getUrl();
		String newUrl = url.replace(
				"&patient:identifier=" + ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_CRR_PSEUDONYM + "|" + pseudonym,
				replacement);
		entry.getRequest().setUrl(newUrl);
	}
}
