package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

public interface BundleValidatorFactory
{
	void init();

	BundleValidator create();
}
