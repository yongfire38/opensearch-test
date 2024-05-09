package egovframework.example.cmm.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonParser {
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseJson(String filePath) throws IOException {
        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonContent, Map.class);
    }
	
}
