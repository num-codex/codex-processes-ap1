package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.send;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BUNDLE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM;
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
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
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
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReference;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;

public class ReadDataTest
{
	@Test
	public void testExecuteWithBgaData() throws Exception
	{
		FhirContext fhirContext = FhirContext.forR4();

		ProcessPluginApi api = Mockito.mock(ProcessPluginApi.class);
		DataStoreClientFactory dataStoreClientFactory = Mockito.mock(DataStoreClientFactory.class);
		DataStoreClient geccoClient = Mockito.mock(DataStoreClient.class);
		DataStoreFhirClient fhirClient = Mockito.mock(DataStoreFhirClient.class);
		Mockito.when(dataStoreClientFactory.getDataStoreClient()).thenReturn(geccoClient);
		Mockito.when(geccoClient.getFhirClient()).thenReturn(fhirClient);
		Mockito.when(fhirClient.getNewData(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(readBundle(fhirContext));
		DataLogger dataLogger = Mockito.mock(DataLogger.class);

		ReadData readData = new ReadData(api, dataStoreClientFactory, dataLogger);

		Variables variables = Mockito.mock(Variables.class);
		DelegateExecution execution = Mockito.mock(DelegateExecution.class);
		Mockito.when(api.getVariables(execution)).thenReturn(variables);

		Mockito.when(execution.getVariable(BPMN_EXECUTION_VARIABLE_PATIENT_REFERENCE)).thenReturn(PatientReference
				.from(new Identifier().setSystem(NAMING_SYSTEM_NUM_CODEX_DIC_PSEUDONYM).setValue("source/original")));

		Task task = createTask();
		Mockito.when(variables.getStartTask()).thenReturn(task);
		TaskHelper taskHelper = Mockito.mock(TaskHelper.class);
		Mockito.when(api.getTaskHelper()).thenReturn(taskHelper);

		Mockito.when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_EXPORT_TO, InstantType.class))
				.thenReturn(Optional.of(new InstantType(new Date())));

		readData.execute(execution);

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
		verify(variables).setResource(keyCaptor.capture(), bundleCaptor.capture());

		assertEquals(BPMN_EXECUTION_VARIABLE_BUNDLE, keyCaptor.getValue());

		Bundle bundle = (Bundle) bundleCaptor.getValue();
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
