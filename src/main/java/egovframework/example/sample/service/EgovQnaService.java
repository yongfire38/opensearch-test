package egovframework.example.sample.service;

import java.io.IOException;
import java.util.List;

import org.opensearch.client.opensearch.core.SearchResponse;

import com.fasterxml.jackson.databind.JsonNode;

import egovframework.example.sample.repository.QnaRepository.QnaInfo;

public interface EgovQnaService {
	
	public void createTextIndex(String indexName) throws IOException;
	
	public void createEmbeddingIndex(String indexName) throws IOException;
	
	public List<QnaInfo> getQnaInfo();
	
	public void insertData(String indexName);
	
	public void insertEmbeddingData(String indexName);
	
	public SearchResponse<JsonNode> textSearch(String indexName, String query) throws IOException;
	
	public SearchResponse<JsonNode> vectorSearch(String indexName, String query) throws IOException;
	
}
