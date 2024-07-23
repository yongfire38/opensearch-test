package egovframework.example.sample.service.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkRequest.Builder;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.PoolingMode;
import egovframework.example.cmm.util.JsonParser;
import egovframework.example.sample.service.EgovAltEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("altEmbeddingService")
@Slf4j
@RequiredArgsConstructor
public class EgovAltEmbeddingServiceImpl extends EgovAbstractServiceImpl implements EgovAltEmbeddingService {
	
	private final OpenSearchClient client;
	
	@Value("${bulk.insert.txt}")
	public String txtFilePath;
	
	@Value("${bulk.insert.jsontext}")
	public String jsonFilePath;
	
	@Value("${embedding.model}")
	public String embeddingModel;
	
	@Value("${embedding.tokenizer}")
	public String embeddingTokenizer;
	
	int index = 1;
	
	@Override
	public SearchResponse<JsonNode> vectorAltSearch(String indexName, String query) throws IOException {
		
		//질의 문자열을 벡터로 변환한다
		EmbeddingModel embeddingModel = new OnnxEmbeddingModel(
				"./model/model.onnx",
				"./model/tokenizer.json",
                PoolingMode.MEAN);
		
		Embedding response = embeddingModel.embed(query).content();

		// embedding 컬럼을 대상으로 검색 (유사한 순으로 5건까지 조회)
		SearchRequest searchRequest = new SearchRequest.Builder()
				.index(indexName)
				.query(q -> q
						.knn(k -> k
							.field("embedding")
							.vector(response.vector())
							.k(5)
						)
				)
			.build();
				
		SearchResponse<JsonNode> searchResponse = client.search(searchRequest, JsonNode.class);
				
		for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
			log.debug(":::::"+searchResponse.hits().hits().get(i).source().get("text") + " with score " + searchResponse.hits().hits().get(i).score());
		}
				
		return searchResponse;
	}

	@Override
	public void insertAltEmbeddingData(String indexName) throws IOException {
		
		long beforeTime = System.currentTimeMillis();
		
		//문자열 파일을 읽어서 json 파일로 변환, 이 과정에서 각 문자열을 임베딩한 결과도 추가한다 
		//toJsonAltConverter 호출...
		toJsonAltConverter();
		
		//만들어진 json 파일을 읽어서 벌크 인덱싱 처리
		List<Map<String, Object>> parsedJsonList = JsonParser.parseJsonList(jsonFilePath);

		Builder bulkRequestBuilder = new BulkRequest.Builder();
		 
		for (Map<String, Object> jsonMap : parsedJsonList) {
		    Map<String, Object> data = jsonMap;
		    bulkRequestBuilder.operations(ops -> ops
		        .index(IndexOperation.of(io -> io.index(indexName).id(String.valueOf(index)).document(data)))
		    );
		    index++;     
		}
		
		//Thread.sleep(1000);
		
		BulkRequest bulkRequest = bulkRequestBuilder.build();
		
		client.bulk(bulkRequest);	
		
		long afterTime = System.currentTimeMillis(); 
		long secDiffTime = (afterTime - beforeTime)/1000;
		
		log.debug("시간차이(m) : " + secDiffTime + "초");
		
	}

	@Override
	public void toJsonAltConverter() throws IOException {
		
		EmbeddingModel embeddingModel = new OnnxEmbeddingModel(
				"./model/model.onnx",
				"./model/tokenizer.json",
                PoolingMode.MEAN);
		
		
		try (FileWriter writer = new FileWriter("./example/output.json"); BufferedReader reader = new BufferedReader(new FileReader(txtFilePath))) {
			
			String line;
            JSONArray jsonArray = new JSONArray();
            
            int id = 1; // ID가 1부터 시작하다고 가정
            
            while ((line = reader.readLine())!= null) {
            	
            	JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", id);
                jsonObject.put("text", line);
                
                Embedding response = embeddingModel.embed(line).content();
            	
                float[] embeddings =  response.vector();
                JSONArray embeddingArray = new JSONArray(embeddings);
                jsonObject.put("embedding", embeddingArray);

                jsonArray.put(jsonObject);
                
            	id++; 
            }
            
            writer.write(jsonArray.toString());
            writer.flush();
            
            log.debug(":::::변환 완료되었습니다.");
            
			
		} catch (IOException e) {
            e.printStackTrace();
        }
		
	}

}
