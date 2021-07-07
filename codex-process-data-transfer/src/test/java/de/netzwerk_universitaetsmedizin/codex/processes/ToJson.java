package de.netzwerk_universitaetsmedizin.codex.processes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.context.FhirContext;

public class ToJson
{
	public static void main(String[] args) throws IOException
	{
		FhirContext fhirContext = FhirContext.forR4();
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/fhir/Task/TaskStartDataSendWithIdentifierReference.xml")))
		{
			Task task = fhirContext.newXmlParser().parseResource(Task.class, in);
			String taskString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(task);
			System.out.println(taskString);
		}
	}
}
