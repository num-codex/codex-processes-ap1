package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.variables;

import java.util.Map;

import org.camunda.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

public class PatientReferenceListValues
{
	public static interface PatientReferenceListValue extends PrimitiveValue<PatientReferenceList>
	{
	}

	private static class PatientReferenceListValueImpl extends PrimitiveTypeValueImpl<PatientReferenceList>
			implements PatientReferenceListValue
	{
		private static final long serialVersionUID = 1L;

		public PatientReferenceListValueImpl(PatientReferenceList value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class PatientReferenceListValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private PatientReferenceListValueTypeImpl()
		{
			super(PatientReferenceList.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new PatientReferenceListValueImpl((PatientReferenceList) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new PatientReferenceListValueTypeImpl();

	private PatientReferenceListValues()
	{
	}

	public static PatientReferenceListValue create(PatientReferenceList value)
	{
		return new PatientReferenceListValueImpl(value, VALUE_TYPE);
	}
}
