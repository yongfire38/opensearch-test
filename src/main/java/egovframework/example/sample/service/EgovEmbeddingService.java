package egovframework.example.sample.service;

import java.io.IOException;
import org.opensearch.client.opensearch.core.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface EgovEmbeddingService {
	
	public void createEmbeddingIndex(String indexName) throws IOException;
	
	public SearchResponse<JsonNode> vectorSearch(String indexName, String query) throws IOException;
	
	public SearchResponse<JsonNode> textSearch(String indexName, String query) throws IOException;
	
	public void insertEmbeddingData(String indexName) throws IOException;
	
	public void toJsonConverter() throws IOException;

}
