package egovframework.example.sample.service.impl;

import static java.time.Duration.ofSeconds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkRequest.Builder;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.output.Response;
import egovframework.example.cmm.util.JsonParser;
import egovframework.example.sample.service.EgovEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("embeddingService")
@Slf4j
@RequiredArgsConstructor
public class EgovEmbeddingServiceImpl extends EgovAbstractServiceImpl implements EgovEmbeddingService {
	
private final OpenSearchClient client;
	
	@Value("${huggingface.access.token}")
	public String accessToken;
	
	@Value("${bulk.insert.txt}")
	public String txtFilePath;
	
	@Value("${bulk.insert.jsontest}")
	public String jsonFilePath;
	
	int index = 1;
	
	@Override
	public void createEmbeddingIndex(String indexName) throws IOException {
		
		CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
			    .index(indexName)
			    .settings(s -> s
			        .knn(true)
			    )
			    .mappings(m -> m
			        .properties("id", p -> p
			            .integer(f -> f
			                .index(true)
			            )
			        )
			        .properties("text", p -> p
			            .text(f -> f
			                .index(true)
			            )
			        )
			        .properties("embedding", p -> p
				            .knnVector(k -> k
				                .dimension(768)
				            )
				        )
			    )
			    .build();
		
		try {
        	CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest);
            log.debug(String.format("Index %s.", createIndexResponse.index().toString().toLowerCase()));
        } catch (OpenSearchException ex) {
            final String errorType = Objects.requireNonNull(ex.response().error().type());
            if (! errorType.equals("resource_already_exists_exception")) {
                throw ex;
            }
        }
	}
	
	@Override
	public SearchResponse<JsonNode> textSearch(String indexName, String query) throws IOException {
		
		//질의 문자열을 벡터로 변환한다
		EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(accessToken)
                .modelId("jhgan/ko-sroberta-multitask")
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .build();

        Response<Embedding> response = embeddingModel.embed(query);

		// embedding 컬럼을 대상으로 검색 (유사한 순으로 5건까지 조회)
		SearchRequest searchRequest = new SearchRequest.Builder()
				.index(indexName)
				.query(q -> q
						.knn(k -> k
							.field("embedding")
							.vector(response.content().vector())
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
	public void insertEmbeddingData(String indexName) throws IOException {
		
		//문자열 파일을 읽어서 json 파일로 변환, 이 과정에서 각 문자열을 임베딩한 결과도 추가한다 
		
		
		
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
		 
		BulkRequest bulkRequest = bulkRequestBuilder.build();

		client.bulk(bulkRequest); 
	}

	@Override
	public void toJsonConverter() throws IOException {
		
		EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(accessToken)
                .modelId("jhgan/ko-sroberta-multitask")
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .build();
		
		try (FileWriter writer = new FileWriter("./example/output.json"); BufferedReader reader = new BufferedReader(new FileReader(txtFilePath))) {
			
			String line;
            JSONArray jsonArray = new JSONArray();
            
            int id = 1; // ID가 1부터 시작하다고 가정
            
            while ((line = reader.readLine())!= null) {
            	
            	JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", id);
                jsonObject.put("text", line);
                
                Response<Embedding> response = embeddingModel.embed(line);
            	
                float[] embeddings =  response.content().vector();
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
