package egovframework.example.sample.service;

import java.io.IOException;

import org.opensearch.client.opensearch.core.SearchResponse;

import com.fasterxml.jackson.databind.JsonNode;

public interface EgovAltEmbeddingService {
	
	public SearchResponse<JsonNode> vectorAltSearch(String indexName, String query) throws IOException;
	
	public void insertAltEmbeddingData(String indexName) throws IOException;
	
	public void toJsonAltConverter() throws IOException;

}
