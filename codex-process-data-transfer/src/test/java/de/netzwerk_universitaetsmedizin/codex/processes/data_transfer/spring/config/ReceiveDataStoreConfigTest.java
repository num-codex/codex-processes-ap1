package de.netzwerk_universitaetsmedizin.codex.processes.data_transfer.spring.config;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import junit.framework.TestCase;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class ReceiveDataStoreConfigTest extends TestCase
{

	public static final String VALID_PROPERTIES = "applicationProperties/valid-application.properties";
	public static final String INVALID_PROPERTIES = "applicationProperties/invalid-application.properties";

	@InjectMocks
	private ReceiveDataStoreConfig receiveDataStoreConfig;

	@Spy
	private ObjectMapper objectMapper = JsonMapper.builder().serializationInclusion(JsonInclude.Include.NON_NULL)
			.serializationInclusion(JsonInclude.Include.NON_EMPTY).disable(MapperFeature.AUTO_DETECT_CREATORS)
			.disable(MapperFeature.AUTO_DETECT_FIELDS).disable(MapperFeature.AUTO_DETECT_SETTERS).build();

	@Test
	public void testConvertClientMapValidMap() throws IOException
	{
		String fileContent = readPropertyFile(VALID_PROPERTIES);

		receiveDataStoreConfig.convertClientMap();
		verify(objectMapper, times(1)).readValue(eq(fileContent), any(TypeReference.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertClientMapInvalidMap() throws IOException
	{
		String fileContent = readPropertyFile(INVALID_PROPERTIES);

		receiveDataStoreConfig.convertClientMap();
		verify(objectMapper, times(1)).readValue(eq(fileContent), any(TypeReference.class));
	}

	@Test
	public void testConvertClientMapWithoutMap() throws IOException
	{
		receiveDataStoreConfig.convertClientMap();
		verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
	}

	private String readPropertyFile(String resourcePath) throws IOException
	{
		Properties props = new Properties();

		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream propsStream = Objects.requireNonNull(classLoader.getResourceAsStream(resourcePath));
		props.load(propsStream);
		propsStream.close();
		String dataStores = (String) props.get("dataStores");
		ReflectionTestUtils.setField(receiveDataStoreConfig, "dataStoresProperty", dataStores);
		return dataStores;
	}
}
