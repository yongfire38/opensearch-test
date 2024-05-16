package egovframework.example.sample.service;

import java.io.IOException;

import org.opensearch.client.opensearch.core.SearchResponse;

import com.fasterxml.jackson.databind.JsonNode;

public interface EgovVecService {
	
	public void createTestIndex(String indexName) throws IOException;
	
	public void insertTestData(String indexName) throws IOException;
	
	public SearchResponse<JsonNode> testSearch(String indexName) throws IOException;
	
}
