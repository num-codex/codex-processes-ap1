package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.receive;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.DataStoreClientFactory;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.fhir.DataStoreFhirClient;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.logging.DataLogger;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class InsertDataIntoCodexTest
{

	@Test
	public void testDoExecuteWithStudyId() throws Exception
	{
		ProcessPluginApi api = mock(ProcessPluginApi.class);
		DataStoreClientFactory dataStoreClientFactory = mock(DataStoreClientFactory.class);
		DataLogger dataLogger = mock(DataLogger.class);
		InsertDataIntoCodex insertDataIntoCodex = new InsertDataIntoCodex(api, dataStoreClientFactory, dataLogger);

		Variables variables = mock(Variables.class);
		DelegateExecution execution = mock(DelegateExecution.class);
		Bundle bundle = new Bundle();
		when(api.getVariables(execution)).thenReturn(variables);
		when(variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE)).thenReturn(bundle);

		Task task = createTask();
		when(variables.getStartTask()).thenReturn(task);
		TaskHelper taskHelper = mock(TaskHelper.class);
		when(api.getTaskHelper()).thenReturn(taskHelper);

		when(taskHelper.getFirstInputParameterStringValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_STUDY_ID)).thenReturn(Optional.of("dataStore"));

		DataStoreClient dataStoreClient = mock(DataStoreClient.class);
		DataStoreFhirClient fhirClient = mock(DataStoreFhirClient.class);
		when(dataStoreClientFactory.getDataStoreClient("dataStore")).thenReturn(dataStoreClient);
		when(dataStoreClient.getFhirClient()).thenReturn(fhirClient);

		insertDataIntoCodex.execute(execution);

		verify(dataStoreClientFactory, times(1)).getDataStoreClient("dataStore");
	}

	@Test
	public void testDoExecuteWithoutStudyId() throws Exception
	{
		ProcessPluginApi api = mock(ProcessPluginApi.class);
		DataStoreClientFactory dataStoreClientFactory = mock(DataStoreClientFactory.class);
		DataLogger dataLogger = mock(DataLogger.class);
		InsertDataIntoCodex insertDataIntoCodex = new InsertDataIntoCodex(api, dataStoreClientFactory, dataLogger);

		Variables variables = mock(Variables.class);
		DelegateExecution execution = mock(DelegateExecution.class);
		Bundle bundle = new Bundle();
		when(variables.getResource(BPMN_EXECUTION_VARIABLE_BUNDLE)).thenReturn(bundle);

		Task task = createTask();
		when(variables.getStartTask()).thenReturn(task);
		TaskHelper taskHelper = mock(TaskHelper.class);
		when(api.getTaskHelper()).thenReturn(taskHelper);

		when(taskHelper.getFirstInputParameterStringValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_STUDY_ID)).thenReturn(Optional.of(""));

		assertThrows(IllegalArgumentException.class, () -> insertDataIntoCodex.doExecute(execution, variables));
		verify(dataStoreClientFactory, never());
	}

	private Task createTask()
	{
		Task task = new Task();
		Coding code = new Coding();
		code.setSystem(CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_STUDY_ID);
		task.addInput(new Task.ParameterComponent(new CodeableConcept(code), new StringType("study_id")));
		return task;
	}
}
