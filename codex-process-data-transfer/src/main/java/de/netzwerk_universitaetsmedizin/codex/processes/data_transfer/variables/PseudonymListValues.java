package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.util.Map;

import org.camunda.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

public class PseudonymListValues
{
	public static interface PseudonymListValue extends PrimitiveValue<PseudonymList>
	{
	}

	private static class PseudonymListValueImpl extends PrimitiveTypeValueImpl<PseudonymList>
			implements PseudonymListValues.PseudonymListValue
	{
		private static final long serialVersionUID = 1L;

		public PseudonymListValueImpl(PseudonymList value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class PseudonymListValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private PseudonymListValueTypeImpl()
		{
			super(PseudonymList.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new PseudonymListValues.PseudonymListValueImpl((PseudonymList) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new PseudonymListValues.PseudonymListValueTypeImpl();

	private PseudonymListValues()
	{
	}

	public static PseudonymListValues.PseudonymListValue create(PseudonymList value)
	{
		return new PseudonymListValues.PseudonymListValueImpl(value, VALUE_TYPE);
	}
}
