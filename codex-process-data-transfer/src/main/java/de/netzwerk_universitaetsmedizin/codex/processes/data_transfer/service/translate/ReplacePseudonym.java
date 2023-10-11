package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.service.translate;

import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_NO_CRR_PSEUDONYM_FOR_DIC_PSEUDONYM;
import static de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.ConstantsDataTransfer.CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM;

import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.client.FttpClientFactory;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class ReplacePseudonym extends AbstractServiceDelegate
{
	private final FttpClientFactory fttpClientFactory;

	public ReplacePseudonym(ProcessPluginApi api, FttpClientFactory fttpClientFactory)
	{
		super(api);

		this.fttpClientFactory = fttpClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fttpClientFactory, "fttpClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		Task task = variables.getStartTask();
		String dicPseudonym = getPseudonym(task).get();

		String crrPseudonym = fttpClientFactory.getFttpClient().getCrrPseudonym(dicPseudonym)
				.orElseThrow(() -> new BpmnError(
						CODESYSTEM_NUM_CODEX_DATA_TRANSFER_ERROR_VALUE_NO_CRR_PSEUDONYM_FOR_DIC_PSEUDONYM,
						"Unable to get CRR pseudonym for given DIC pseudonym"));
		variables.setString(BPMN_EXECUTION_VARIABLE_PSEUDONYM, crrPseudonym);
	}

	private Optional<String> getPseudonym(Task task)
	{
		return api.getTaskHelper().getFirstInputParameterValue(task, CODESYSTEM_NUM_CODEX_DATA_TRANSFER,
				CODESYSTEM_NUM_CODEX_DATA_TRANSFER_VALUE_PSEUDONYM, Identifier.class).map(Identifier::getValue);
	}
}
