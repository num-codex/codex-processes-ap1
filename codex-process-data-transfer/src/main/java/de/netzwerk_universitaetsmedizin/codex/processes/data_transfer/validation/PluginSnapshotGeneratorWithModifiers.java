package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.StructureDefinition;
import org.springframework.beans.factory.InitializingBean;

import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.ClosedTypeSlicingRemover;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.IdentifierRemover;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.SliceMinFixer;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation.structure_definition.StructureDefinitionModifier;
import dev.dsf.fhir.validation.SnapshotGenerator;

public class PluginSnapshotGeneratorWithModifiers implements SnapshotGenerator, InitializingBean
{
	public static final StructureDefinitionModifier CLOSED_TYPE_SLICING_REMOVER = new ClosedTypeSlicingRemover();
	public static final StructureDefinitionModifier IDENTIFIER_REMOVER = new IdentifierRemover();
	public static final StructureDefinitionModifier SLICE_MIN_FIXER = new SliceMinFixer();

	private final SnapshotGenerator delegate;
	private final List<StructureDefinitionModifier> structureDefinitionModifiers = new ArrayList<>();

	public PluginSnapshotGeneratorWithModifiers(SnapshotGenerator delegate)
	{
		this(delegate, Arrays.asList(CLOSED_TYPE_SLICING_REMOVER, IDENTIFIER_REMOVER, SLICE_MIN_FIXER));
	}

	public PluginSnapshotGeneratorWithModifiers(SnapshotGenerator delegate,
			Collection<? extends StructureDefinitionModifier> structureDefinitionModifiers)
	{
		this.delegate = delegate;

		if (structureDefinitionModifiers != null)
			this.structureDefinitionModifiers.addAll(structureDefinitionModifiers);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public SnapshotWithValidationMessages generateSnapshot(StructureDefinition differential)
	{
		return delegate.generateSnapshot(modify(differential));
	}

	@Override
	public SnapshotWithValidationMessages generateSnapshot(StructureDefinition differential,
			String baseAbsoluteUrlPrefix)
	{
		return delegate.generateSnapshot(modify(differential), baseAbsoluteUrlPrefix);
	}

	private StructureDefinition modify(StructureDefinition differential)
	{
		if (differential == null)
			return null;

		for (StructureDefinitionModifier mod : structureDefinitionModifiers)
			differential = mod.modify(differential);

		return differential;
	}
}
