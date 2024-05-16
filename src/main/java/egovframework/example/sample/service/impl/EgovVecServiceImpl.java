package egovframework.example.sample.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

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
	public SearchResponse<JsonNode> testSearch(String indexName) throws IOException {
		
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
	
}
