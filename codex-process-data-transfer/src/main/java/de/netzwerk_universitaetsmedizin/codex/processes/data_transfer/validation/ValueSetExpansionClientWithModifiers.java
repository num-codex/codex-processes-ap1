package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.beans.factory.InitializingBean;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set.MissingEntriesIncluder;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.value_set.ValueSetModifier;

public class ValueSetExpansionClientWithModifiers implements ValueSetExpansionClient, InitializingBean
{
	public static final ValueSetModifier MISSING_ENTRIES_INCLUDER = new MissingEntriesIncluder();

	private final ValueSetExpansionClient delegate;
	private final List<ValueSetModifier> valueSetModifiers = new ArrayList<>();

	public ValueSetExpansionClientWithModifiers(ValueSetExpansionClient delegate)
	{
		this(delegate, Arrays.asList(MISSING_ENTRIES_INCLUDER));
	}

	public ValueSetExpansionClientWithModifiers(ValueSetExpansionClient delegate,
			Collection<? extends ValueSetModifier> valueSetModifiers)
	{
		this.delegate = delegate;

		if (valueSetModifiers != null)
			this.valueSetModifiers.addAll(valueSetModifiers);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ValueSet expand(ValueSet valueSet) throws IOException, WebApplicationException
	{
		if (valueSet == null)
			return null;

		for (ValueSetModifier modifier : valueSetModifiers)
			valueSet = modifier.modifyPreExpansion(valueSet);

		ValueSet expandedValueSet = delegate.expand(valueSet);

		for (ValueSetModifier modifier : valueSetModifiers)
			expandedValueSet = modifier.modifyPostExpansion(valueSet, expandedValueSet);

		return expandedValueSet;
	}

	@Override
	public CapabilityStatement getMetadata() throws WebApplicationException
	{
		return delegate.getMetadata();
	}
}
