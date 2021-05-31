package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.util.Map;

import org.camunda.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

public class PatientReferenceValues
{
	public static interface PatientReferenceValue extends PrimitiveValue<PatientReference>
	{
	}

	private static class PatientReferenceValueImpl extends PrimitiveTypeValueImpl<PatientReference>
			implements PatientReferenceValue
	{
		private static final long serialVersionUID = 1L;

		public PatientReferenceValueImpl(PatientReference value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class PatientReferenceValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private PatientReferenceValueTypeImpl()
		{
			super(PatientReference.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new PatientReferenceValueImpl((PatientReference) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new PatientReferenceValueTypeImpl();

	private PatientReferenceValues()
	{
	}

	public static PatientReferenceValue create(PatientReference value)
	{
		return new PatientReferenceValueImpl(value, VALUE_TYPE);
	}
}
