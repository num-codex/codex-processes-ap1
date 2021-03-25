package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.io.IOException;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables.PseudonymListValues.PseudonymListValue;

public class PseudonymListSerializer extends PrimitiveValueSerializer<PseudonymListValue> implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public PseudonymListSerializer(ObjectMapper objectMapper)
	{
		super(PseudonymListValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(PseudonymListValue value, ValueFields valueFields)
	{
		PseudonymList results = value.getValue();
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
	public PseudonymListValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return PseudonymListValues.create((PseudonymList) untypedValue.getValue());
	}

	@Override
	public PseudonymListValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			PseudonymList pseudonyms = (bytes == null || bytes.length <= 0) ? null
					: objectMapper.readValue(bytes, PseudonymList.class);
			return PseudonymListValues.create(pseudonyms);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
