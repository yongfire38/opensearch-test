package egovframework.example.sample.service;

import java.io.IOException;
import org.opensearch.client.opensearch.core.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import egovframework.example.sample.index.Color;

public interface EgovColorService {
	
	public void createColorIndex(String indexName) throws IOException;

	public void insertColorData(String indexName) throws IOException;
	
	public SearchResponse<JsonNode> colorSearch(String indexName, Color color) throws IOException;
	
	public SearchResponse<JsonNode> colorTextSearch(String indexName, String query) throws IOException;
	
}
