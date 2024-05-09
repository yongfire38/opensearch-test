package egovframework.example.sample.service.impl;

import java.io.IOException;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.UpdateResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.springframework.stereotype.Service;

import egovframework.example.sample.index.Movie;
import egovframework.example.sample.service.EgovMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("movieService")
@Slf4j
@RequiredArgsConstructor
public class EgovMovieServiceImpl extends EgovAbstractServiceImpl implements EgovMovieService {
	
	private final OpenSearchClient client;

	@Override
	public void createIndex(String indexName) throws IOException {
		CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder().index(indexName).build();
		CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest);
		
		log.debug(String.format("Index %s.", createIndexResponse.index().toString().toLowerCase()));
	}

	@Override
	public void insertData(String indexName, Movie movie, String id) throws IOException {
		IndexRequest<Movie> indexRequest = new IndexRequest.Builder<Movie>().index(indexName).id(id).document(movie).build();
		IndexResponse indexResponse = client.index(indexRequest);
		
		log.debug(String.format("Document %s.", indexResponse.result().toString().toLowerCase()));
	}
	
	@Override
	public void updateData(String indexName, Movie movie, String id) throws IOException {
		UpdateRequest<Movie, Movie> updateRequest = new UpdateRequest.Builder<Movie, Movie>()
				.id(id)
				.index(indexName)
				.doc(movie)
				.build();
		UpdateResponse<Movie> updateResponse = client.update(updateRequest, Movie.class);
		log.debug(String.format("Document %s.", updateResponse.result().toString().toLowerCase()));
	}

	@Override
	public SearchResponse<Movie> search(String indexName) throws IOException {
		try {
			// 도큐먼트 설정이 없이 조회하면 기본적으로 10개만 조회함
			//SearchResponse<Movie> searchResponse = client.search(s -> s.index(indexName), Movie.class);

			SearchRequest.Builder builder = new SearchRequest.Builder().index(indexName);
			builder.size(1000); // 가져올 도큐먼트의 갯수 설정

			SearchResponse<Movie> searchResponse = client.search(builder.build(), Movie.class);

			for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
				log.debug("search result:::::"+searchResponse.hits().hits().get(i).source());
			}
			return searchResponse;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}

	@Override
	public void deleteData(String indexName, String id) throws IOException {
		DeleteResponse deleteResponse = client.delete(b -> b.index(indexName).id(id));
		log.debug(String.format("Document %s.", deleteResponse.result().toString().toLowerCase()));
	}

	@Override
	public void deleteIndex(String indexName) throws IOException {
		DeleteIndexRequest deleteRequest = new DeleteIndexRequest.Builder().index(indexName).build();
        client.indices().delete(deleteRequest);
        log.debug(String.format("Index %s.", deleteRequest.index().toString().toLowerCase()));
	}

}
