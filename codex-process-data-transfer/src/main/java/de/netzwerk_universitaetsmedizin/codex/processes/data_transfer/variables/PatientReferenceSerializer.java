package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.io.IOException;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PatientReferenceValues.PatientReferenceValue;

public class PatientReferenceSerializer extends PrimitiveValueSerializer<PatientReferenceValue>
		implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public PatientReferenceSerializer(ObjectMapper objectMapper)
	{
		super(PatientReferenceValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(PatientReferenceValue value, ValueFields valueFields)
	{
		PatientReference results = value.getValue();
		try
		{
			if (results != null)
				valueFields.setByteArrayValue(objectMapper.writeValueAsBytes(results));
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public PatientReferenceValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return PatientReferenceValues.create((PatientReference) untypedValue.getValue());
	}

	@Override
	public PatientReferenceValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			PatientReference referenceList = (bytes == null || bytes.length <= 0) ? null
					: objectMapper.readValue(bytes, PatientReference.class);
			return PatientReferenceValues.create(referenceList);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
