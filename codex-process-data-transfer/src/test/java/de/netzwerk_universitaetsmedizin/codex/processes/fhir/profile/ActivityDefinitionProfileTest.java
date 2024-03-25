package de.netzwerk_universitaetsmedizin.codex.processes.fhir.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.DataTransferProcessPluginDefinition;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelperImpl;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class ActivityDefinitionProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProfileTest.class);

	private static final DataTransferProcessPluginDefinition def = new DataTransferProcessPluginDefinition();

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(def.getResourceVersion(),
			def.getReleaseDate(),
			Arrays.asList("dsf-activity-definition-1.0.0.xml", "dsf-extension-process-authorization-1.0.0.xml",
					"dsf-extension-process-authorization-organization-1.0.0.xml",
					"dsf-extension-process-authorization-organization-practitioner-1.0.0.xml",
					"dsf-extension-process-authorization-parent-organization-role-1.0.0.xml",
					"dsf-extension-process-authorization-parent-organization-role-practitioner-1.0.0.xml",
					"dsf-extension-process-authorization-practitioner-1.0.0.xml",
					"dsf-coding-process-authorization-local-all-1.0.0.xml",
					"dsf-coding-process-authorization-local-all-practitioner-1.0.0.xml",
					"dsf-coding-process-authorization-local-organization-1.0.0.xml",
					"dsf-coding-process-authorization-local-organization-practitioner-1.0.0.xml",
					"dsf-coding-process-authorization-local-parent-organization-role-1.0.0.xml",
					"dsf-coding-process-authorization-local-parent-organization-role-practitioner-1.0.0.xml",
					"dsf-coding-process-authorization-remote-all-1.0.0.xml",
					"dsf-coding-process-authorization-remote-parent-organization-role-1.0.0.xml",
					"dsf-coding-process-authorization-remote-organization-1.0.0.xml"),
			Arrays.asList("dsf-organization-role-1.0.0.xml", "dsf-practitioner-role-1.0.0.xml",
					"dsf-process-authorization-1.0.0.xml", "dsf-read-access-tag-1.0.0.xml"),
			Arrays.asList("dsf-organization-role-1.0.0.xml", "dsf-practitioner-role-1.0.0.xml",
					"dsf-process-authorization-recipient-1.0.0.xml", "dsf-process-authorization-requester-1.0.0.xml",
					"dsf-read-access-tag-1.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	private final ProcessAuthorizationHelper processAuthorizationHelper = new ProcessAuthorizationHelperImpl();

	@Test
	public void testDataTriggerValid() throws Exception
	{
		ActivityDefinition ad = validationRule
				.readActivityDefinition(Paths.get("src/main/resources/fhir/ActivityDefinition/data-trigger.xml"));

		ValidationResult result = resourceValidator.validate(ad);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());

		assertTrue(processAuthorizationHelper.isValid(ad, taskProfile -> true, practitionerRole -> true,
				orgIdentifier -> true, organizationRole -> true));
	}

	@Test
	public void testDataSendValid() throws Exception
	{
		ActivityDefinition ad = validationRule
				.readActivityDefinition(Paths.get("src/main/resources/fhir/ActivityDefinition/data-send.xml"));

		ValidationResult result = resourceValidator.validate(ad);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());

		assertTrue(processAuthorizationHelper.isValid(ad, taskProfile -> true, practitionerRole -> true,
				orgIdentifier -> true, organizationRole -> true));
	}

	@Test
	public void testDataTranslateValid() throws Exception
	{
		ActivityDefinition ad = validationRule
				.readActivityDefinition(Paths.get("src/main/resources/fhir/ActivityDefinition/data-translate.xml"));

		ValidationResult result = resourceValidator.validate(ad);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());

		assertTrue(processAuthorizationHelper.isValid(ad, taskProfile -> true, practitionerRole -> true,
				orgIdentifier -> true, organizationRole -> true));
	}

	@Test
	public void testDataReceiveValid() throws Exception
	{
		ActivityDefinition ad = validationRule
				.readActivityDefinition(Paths.get("src/main/resources/fhir/ActivityDefinition/data-receive.xml"));

		ValidationResult result = resourceValidator.validate(ad);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());

		assertTrue(processAuthorizationHelper.isValid(ad, taskProfile -> true, practitionerRole -> true,
				orgIdentifier -> true, organizationRole -> true));
	}
}
