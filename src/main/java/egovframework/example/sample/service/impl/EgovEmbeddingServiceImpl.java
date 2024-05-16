package egovframework.example.sample.service.impl;

import static java.time.Duration.ofSeconds;

import java.io.IOException;
import java.util.Objects;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.output.Response;
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

}
