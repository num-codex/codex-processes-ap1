package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.io.IOException;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceListValues.PatientReferenceListValue;

public class PatientReferenceListSerializer extends PrimitiveValueSerializer<PatientReferenceListValue>
		implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public PatientReferenceListSerializer(ObjectMapper objectMapper)
	{
		super(PatientReferenceListValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(PatientReferenceListValue value, ValueFields valueFields)
	{
		PatientReferenceList patientReferenceList = value.getValue();
		try
		{
			if (patientReferenceList != null)
				valueFields.setByteArrayValue(objectMapper.writeValueAsBytes(patientReferenceList));
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public PatientReferenceListValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return PatientReferenceListValues.create((PatientReferenceList) untypedValue.getValue());
	}

	@Override
	public PatientReferenceListValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			PatientReferenceList referenceList = (bytes == null || bytes.length <= 0) ? null
					: objectMapper.readValue(bytes, PatientReferenceList.class);
			return PatientReferenceListValues.create(referenceList);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
