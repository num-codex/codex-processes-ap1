package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_NO_CRR_PSEUDONYM_FOR_DIC_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;

public class ReplacePseudonym extends AbstractServiceDelegate
{
	private final FttpClientFactory fttpClientFactory;

	public ReplacePseudonym(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FttpClientFactory fttpClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.fttpClientFactory = fttpClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fttpClientFactory, "fttpClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getCurrentTaskFromExecutionVariables(execution);

		String dicPseudonym = getPseudonym(task).get();

		String crrPseudonym = fttpClientFactory.getFttpClient().getCrrPseudonym(dicPseudonym)
				.orElseThrow(() -> new BpmnError(
						CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_NO_CRR_PSEUDONYM_FOR_DIC_PSEUDONYM,
						"Unable to get CRR pseudonym for given DIC pseudonym"));

		execution.setVariable(BPMN_EXECUTION_VARIABLE_PSEUDONYM, Variables.stringValue(crrPseudonym));
	}

	private Optional<String> getPseudonym(Task task)
	{
		return getInputParameterValues(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM, Identifier.class).findFirst()
				.map(Identifier::getValue);
	}

	private <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> type)
	{
		return task.getInput().stream().filter(c -> type.isInstance(c.getValue()))
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> system.equals(co.getSystem()) && code.equals(co.getCode())))
				.map(c -> type.cast(c.getValue()));
	}
}
