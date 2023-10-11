package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.util.ClasspathUtil;

public class QuietCommonCodeSystemsTerminologyService extends CommonCodeSystemsTerminologyService
{
	private static final Logger logger = LoggerFactory.getLogger(QuietCommonCodeSystemsTerminologyService.class);

	private final UcumEssenceService ucumEssenceService;

	public QuietCommonCodeSystemsTerminologyService(FhirContext theFhirContext)
	{
		super(theFhirContext);

		ucumEssenceService = createUcumEssenceService();
	}

	private UcumEssenceService createUcumEssenceService()
	{
		try (InputStream in = ClasspathUtil.loadResourceAsStream("/ucum-essence.xml"))
		{
			return new UcumEssenceService(in);
		}
		catch (UcumException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public LookupCodeResult lookupCode(ValidationSupportContext validationSupportContext, String system, String code)
	{
		if (UCUM_CODESYSTEM_URL.equals(system))
			return lookupUcumCode(code);

		return super.lookupCode(validationSupportContext, system, code);
	}

	private LookupCodeResult lookupUcumCode(String code)
	{
		LookupCodeResult retVal = new LookupCodeResult();
		retVal.setSearchedForCode(code);
		retVal.setSearchedForSystem(UCUM_CODESYSTEM_URL);

		analyse(code).ifPresent(outcome ->
		{
			retVal.setFound(true);
			retVal.setCodeDisplay(outcome);
		});

		return retVal;
	}

	private Optional<String> analyse(String code)
	{
		try
		{
			return Optional.of(ucumEssenceService.analyse(code));
		}
		catch (UcumException e)
		{
			logger.warn("Failed parse UCUM code '{}': {}", code, e.getMessage());
			return Optional.empty();
		}
	}
}
