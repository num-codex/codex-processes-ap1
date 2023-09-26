package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import dev.dsf.fhir.validation.SnapshotGeneratorImpl;

public class PluginSnapshotGeneratorImpl extends SnapshotGeneratorImpl
{
	public PluginSnapshotGeneratorImpl(FhirContext fhirContext, IValidationSupport validationSupport)
	{
		super(fhirContext, validationSupport);
	}

	protected IWorkerContext createWorker(FhirContext context, IValidationSupport validationSupport)
	{
		HapiWorkerContext workerContext = new HapiWorkerContext(context, validationSupport);
		workerContext.setLocale(context.getLocalizer().getLocale());
		return new PluginWorkerContext(workerContext);
	}
}
