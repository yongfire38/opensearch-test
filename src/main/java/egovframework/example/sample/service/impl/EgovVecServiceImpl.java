package egovframework.example.sample.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkRequest.Builder;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import egovframework.example.cmm.util.JsonParser;
import egovframework.example.sample.index.Color;
import egovframework.example.sample.service.EgovVecService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Service("vecService")
@Slf4j
@RequiredArgsConstructor
public class EgovVecServiceImpl extends EgovAbstractServiceImpl implements EgovVecService {
	
	private final OpenSearchClient client;
	
	@Value("${bulk.insert.json}")
    public String jsonFilePath ;
	
	int index = 1;

	@Override
	public void createTestIndex(String indexName) throws IOException {
		
        CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
            .index(indexName)
            .settings(s -> s
                .knn(true)
            )
            .mappings(m -> m
                .properties("values", p -> p
                    .knnVector(k -> k
                    .dimension(3)
                )
            )
        ).build();

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
	public void createColorIndex(String indexName) throws IOException {
		
        CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
            .index(indexName)
            .settings(s -> s
                .knn(true)
            )
            .mappings(m -> m
                .properties("rgb", p -> p
                    .knnVector(k -> k
                    .dimension(3)
                )
            )
        ).build();

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
	public void insertTestData(String indexName) throws IOException {
		
			JsonObject doc1 = Json.createObjectBuilder()
                .add("values", Json.createArrayBuilder().add(0.1).add(0.2).add(0.3).build())
                .add("metadata", Json.createObjectBuilder().add("genre", "drama"))
                .build();

            JsonObject doc2 = Json.createObjectBuilder()
                .add("values", Json.createArrayBuilder().add(0.2).add(0.3).add(0.4).build())
                .add("metadata", Json.createObjectBuilder().add("genre", "action"))
                .build();

            ArrayList<BulkOperation> operations = new ArrayList<>();
            operations.add(new BulkOperation.Builder().index(IndexOperation.of(io -> io.index(indexName).id("vec1").document(doc1))).build());
            operations.add(new BulkOperation.Builder().index(IndexOperation.of(io -> io.index(indexName).id("vec2").document(doc2))).build());

            // index data
            BulkRequest bulkRequest = new BulkRequest.Builder()
                .index(indexName)
                .operations(operations)
                .build();

            client.bulk(bulkRequest);
	}
	
	
	@Override
	@SuppressWarnings("unchecked")
	public void insertColorData(String indexName) throws IOException {
		
		String filePath = jsonFilePath;
		
		Map<String, Object> jsonMap = JsonParser.parseJson(filePath);
		
		// BulkRequest 생성
        Builder bulkRequestBuilder = new BulkRequest.Builder();
        
        // JSON 내용을 사용하여 인덱스에 데이터 추가
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
			Map<String, Object> colorData = (Map<String, Object>) entry.getValue();
			bulkRequestBuilder.operations(ops -> ops
	                .index(IndexOperation.of(io -> io.index(indexName).id(String.valueOf(index)).document(colorData)))
	        );
            index++; 	
        }
        
        // BulkRequest 인스턴스 생성
        BulkRequest bulkRequest = bulkRequestBuilder.build();

        // 데이터 인덱싱
        client.bulk(bulkRequest); 
		
	}
	
	@Override
	public SearchResponse<JsonNode> search(String indexName) throws IOException {
		
		// values 컬럼을 대상으로 [0.1f, 0.2f, 0.3f]로 검색 (유사한 순으로 2건까지 조회)
		SearchRequest searchRequest = new SearchRequest.Builder()
                .index(indexName)
                .query(q -> q
                    .knn(k -> k
                        .field("values")
                        .vector(new float[] { 0.1f, 0.2f, 0.3f })
                        .k(2)
                    )
                )
            .build();
		
		SearchResponse<JsonNode> searchResponse = client.search(searchRequest, JsonNode.class);
		
		for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
			log.debug("vector search result:::::"+searchResponse.hits().hits().get(i).source());
		}
		
		return searchResponse;
	}

	@Override
	public SearchResponse<JsonNode> colorSearch(String indexName, Color color) throws IOException {
		// rgb 컬럼을 대상으로 검색 (유사한 순으로 3건까지 조회)
		SearchRequest searchRequest = new SearchRequest.Builder()
				.index(indexName)
				.query(q -> q
						.knn(k -> k
							.field("rgb")
							.vector(new float[] { color.getRed(), color.getGreen(), color.getBlue() })
							.k(3)
						)
				)
			.build();
		
		SearchResponse<JsonNode> searchResponse = client.search(searchRequest, JsonNode.class);
		
		for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
			log.debug("color search result:::::"+searchResponse.hits().hits().get(i).source());
		}
		
		return searchResponse;
	}

	@Override
	public SearchResponse<JsonNode> colorTextSearch(String indexName, String query) throws IOException {
		
		// step 1. query에서 받은 값으로 검색
		SearchRequest textSearchRequest = new SearchRequest.Builder()
				.index(indexName)
				.query(q -> q.queryString(qs -> qs.fields("name").query(query)))
			    .build();
		
		SearchResponse<JsonNode> textSearchResponse = client.search(textSearchRequest, JsonNode.class);
		
		// 검색 결과가 있을 경우에만 다음으로 진행
		if(textSearchResponse.hits().hits().size() != 0) {
			
			// 결과는 searchResponse.hits().hits().get(i).score() 순으로 정렬됨 (유사도). 
			// step 2. 가장 상위의 것으로 rgb 값을 받아서 그걸로 벡터 검색을 실행하도록 한다.
			JsonNode sourceNode = textSearchResponse.hits().hits().get(0).source();
			Color color = getColorFromJsonNode(sourceNode);
			
			 SearchRequest colorSearchRequest = new SearchRequest.Builder()
						.index(indexName)
						.query(q -> q
								.knn(k -> k
									.field("rgb")
									.vector(new float[] { color.getRed(), color.getGreen(), color.getBlue() })
									.k(3)
								)
						)
					.build();
			
			 SearchResponse<JsonNode> colorSearchResponse = client.search(colorSearchRequest, JsonNode.class);
			 
			 for (int i = 0; i < colorSearchResponse.hits().hits().size(); i++) {
					log.debug("color 유사도 search result:::::"+colorSearchResponse.hits().hits().get(i).source());
				}
			
			 return colorSearchResponse;
		} else {
			log.debug("result not found");
			throw new RuntimeException("result not found");
		}
	}
	
	private Color getColorFromJsonNode(JsonNode sourceNode) {
		
	    int red = sourceNode.get("rgb").get(0).asInt();
	    int green = sourceNode.get("rgb").get(1).asInt();
	    int blue = sourceNode.get("rgb").get(2).asInt();
	    
	    log.debug("질의에서 얻어낸 rgb 값은:::::["+red+","+green+","+blue+"]");

	    Color color = new Color();
	    color.setRed(red);
	    color.setGreen(green);
	    color.setBlue(blue);

	    return color;
	}
	
}
