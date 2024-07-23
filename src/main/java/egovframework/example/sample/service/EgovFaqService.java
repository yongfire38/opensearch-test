 package egovframework.example.sample.service;

import java.io.IOException;
import java.util.List;

import egovframework.example.sample.repository.FaqRepository.FaqInfo;

public interface EgovFaqService {

	public void createIndex(String indexName) throws IOException;
	
	public List<FaqInfo> getFaqInfo();
}
