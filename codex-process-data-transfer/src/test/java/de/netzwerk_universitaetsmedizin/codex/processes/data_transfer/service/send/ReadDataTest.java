package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues.FhirResourceValue;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ca.uhn.fhir.context.FhirContext;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.GeccoClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.GeccoFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;

public class ReadDataTest
{
	@Test
	public void testExecuteWithBgaData() throws Exception
	{
		FhirContext fhirContext = FhirContext.forR4();

		FhirWebserviceClientProvider clientProvider = Mockito.mock(FhirWebserviceClientProvider.class);
		TaskHelper taskHelper = Mockito.mock(TaskHelper.class);
		ReadAccessHelper readAccessHelper = Mockito.mock(ReadAccessHelper.class);
		GeccoClientFactory geccoClientFactory = Mockito.mock(GeccoClientFactory.class);
		GeccoClient geccoClient = Mockito.mock(GeccoClient.class);
		GeccoFhirClient fhirClient = Mockito.mock(GeccoFhirClient.class);
		Mockito.when(geccoClientFactory.getGeccoClient()).thenReturn(geccoClient);
		Mockito.when(geccoClient.getFhirClient()).thenReturn(fhirClient);
		Mockito.when(fhirClient.getNewData(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(readBundle(fhirContext));

		ReadData readData = new ReadData(clientProvider, taskHelper, readAccessHelper, fhirContext, geccoClientFactory);
		DelegateExecution execution = Mockito.mock(DelegateExecution.class);
		Mockito.when(execution.getCurrentActivityId()).thenReturn(UUID.randomUUID().toString());
		Mockito.when(execution.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE)).thenReturn(PatientReference
				.from(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue("source/original")));
		Task task = createTask();
		Mockito.when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK)).thenReturn(task);

		readData.execute(execution);

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FhirResourceValue> bundleCaptor = ArgumentCaptor.forClass(FhirResourceValue.class);
		verify(execution).setVariable(keyCaptor.capture(), bundleCaptor.capture());

		assertEquals(BPMN_EXECUTION_VARIABLE_BUNDLE, keyCaptor.getValue());

		Bundle bundle = (Bundle) bundleCaptor.getValue().getValue();
		assertNotNull(bundle.getEntry());
		assertEquals(7, bundle.getEntry().size());

		assertTrue(bundle.getEntry().get(0).hasResource());
		assertTrue(bundle.getEntry().get(0).getResource().hasMeta());
		assertTrue(bundle.getEntry().get(0).getResource().getMeta().hasProfile());
		assertEquals("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient",
				bundle.getEntry().get(0).getResource().getMeta().getProfile().get(0).getValue());

		List<String> bgaProfiles = new ArrayList<>(Arrays.asList(
				"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/pH",
				"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/carbon-dioxide-partial-pressure",
				"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/oxygen-partial-pressure",
				"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/oxygen-saturation",
				"https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/inhaled-oxygen-concentration"));

		for (int i = 1; i < bundle.getEntry().size() - 1; i++)
		{
			assertTrue(bundle.getEntry().get(i).hasResource());
			assertTrue(bundle.getEntry().get(i).getResource().hasMeta());
			assertTrue(bundle.getEntry().get(i).getResource().getMeta().hasProfile());
			assertTrue(bgaProfiles
					.contains(bundle.getEntry().get(i).getResource().getMeta().getProfile().get(0).getValue()));

			// removing profile from expected profiles to check every expected profile only appears once
			bgaProfiles.remove(bundle.getEntry().get(i).getResource().getMeta().getProfile().get(0).getValue());
		}

		assertTrue(bundle.getEntry().get(bundle.getEntry().size() - 1).hasResource());
		assertTrue(bundle.getEntry().get(bundle.getEntry().size() - 1).getResource().hasMeta());
		assertTrue(bundle.getEntry().get(bundle.getEntry().size() - 1).getResource().getMeta().hasProfile());
		assertEquals("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-gas-panel", bundle
				.getEntry().get(bundle.getEntry().size() - 1).getResource().getMeta().getProfile().get(0).getValue());
	}

	private Stream<DomainResource> readBundle(FhirContext fhirContext) throws FileNotFoundException, IOException
	{
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/fhir/Bundle/dic_fhir_store_demo_psn_bga.json")))
		{
			Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, in);
			return bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
					.map(BundleEntryComponent::getResource).filter(r -> r instanceof DomainResource)
					.map(r -> (DomainResource) r);
		}
	}

	private Task createTask()
	{
		Task task = new Task();
		task.addInput().setValue(new InstantType(new Date())).getType().getCodingFirstRep()
				.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER)
				.setCode(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO);

		return task;
	}
}
