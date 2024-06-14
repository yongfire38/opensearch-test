package egovframework.example.sample.service.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.analysis.Analyzer;
import org.opensearch.client.opensearch._types.analysis.AsciiFoldingTokenFilter;
import org.opensearch.client.opensearch._types.analysis.CharFilter;
import org.opensearch.client.opensearch._types.analysis.CustomAnalyzer;
import org.opensearch.client.opensearch._types.analysis.HtmlStripCharFilter;
import org.opensearch.client.opensearch._types.analysis.LowercaseTokenFilter;
import org.opensearch.client.opensearch._types.analysis.NoriDecompoundMode;
import org.opensearch.client.opensearch._types.analysis.NoriPartOfSpeechTokenFilter;
import org.opensearch.client.opensearch._types.analysis.NoriTokenizer;
import org.opensearch.client.opensearch._types.analysis.PatternReplaceCharFilter;
import org.opensearch.client.opensearch._types.analysis.SynonymGraphTokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.Tokenizer;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import egovframework.example.sample.index.Movie;
import egovframework.example.sample.service.EgovMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("movieService")
@Slf4j
@RequiredArgsConstructor
public class EgovMovieServiceImpl extends EgovAbstractServiceImpl implements EgovMovieService {
	
	@Value("${synonyms.path}")
	public String synonymsPath;
	
	@Value("${dictionary.path}")
	public String dictionaryRulesPath;
	
	private final OpenSearchClient client;

	@Override
	public void createIndex(String indexName) throws IOException {
		
		Map<String, Tokenizer> tokenizerMap = new HashMap<>();
		Map<String, Analyzer> analyzerMap = new HashMap<>();
		Map<String, TokenFilter> tokenFilterMap = new HashMap<>();
        //Map<String, Normalizer> normalizerMap = new HashMap<>(); //normalizer 이용 시에만 추가
        Map<String, CharFilter> charFilterMap = new HashMap<>();
		
        // char filter : html 태그를 제거한다
		HtmlStripCharFilter htmlStripFilter = new HtmlStripCharFilter.Builder().build();
		CharFilter chrFilter =  new CharFilter.Builder().definition(htmlStripFilter._toCharFilterDefinition()).build();
		charFilterMap.put("htmlfilter", chrFilter);
		
		// remove punctuation chars : 구두점을 제거한다
		PatternReplaceCharFilter patternCharFilter = new PatternReplaceCharFilter.Builder().pattern("\\p{Punct}").replacement("").flags("CASE_INSENSITIVE|MULTILINE").build();
		CharFilter chrPatternFilter =  new CharFilter.Builder().definition(patternCharFilter._toCharFilterDefinition()).build();
		charFilterMap.put("patternfilter", chrPatternFilter);
		
		List<String> charFilterList = new ArrayList<>();
        charFilterList.add("htmlfilter");
        charFilterList.add("patternfilter");
        
        // 제거할 품사를 열거한다 : NR - 수사
        List<String> stopTags = Arrays.asList("NR");
        
        // Token filter : 소문자 변환 / 비ASCII 문자를 ASCII 문자로 변환 / 한국어의 특정 품사를 제거
        LowercaseTokenFilter lowerFilter = new LowercaseTokenFilter.Builder().build();
        AsciiFoldingTokenFilter asciiFilter = new AsciiFoldingTokenFilter.Builder().preserveOriginal(false).build();
        NoriPartOfSpeechTokenFilter noriPartOfSpeechFilter = new NoriPartOfSpeechTokenFilter.Builder().stoptags(stopTags).build();        
        tokenFilterMap.put("lowercase", new TokenFilter.Builder().definition(lowerFilter._toTokenFilterDefinition()).build());
        tokenFilterMap.put("asciifolding", new TokenFilter.Builder().definition(asciiFilter._toTokenFilterDefinition()).build());
        tokenFilterMap.put("nori_part_of_speech", new TokenFilter.Builder().definition(noriPartOfSpeechFilter._toTokenFilterDefinition()).build());
        
        //List<String> synonym = Arrays.asList("amazon, aws", "풋사과, 햇사과, 사과");
        List<String> synonym = readWordsFromFile(synonymsPath);
        
        SynonymGraphTokenFilter synonymFilter = new SynonymGraphTokenFilter.Builder().synonyms(synonym).expand(true).build();
        tokenFilterMap.put("synonym_graph", new TokenFilter.Builder().definition(synonymFilter._toTokenFilterDefinition()).build());
        
		List<String> tokenFilterList = new ArrayList<>();
		
		tokenFilterList.add("lowercase");
		tokenFilterList.add("asciifolding");
		tokenFilterList.add("synonym_graph");
		tokenFilterList.add("nori_number"); // 한국어 숫자의 검색을 가능하게 함
		tokenFilterList.add("nori_readingform"); // 한자의 한국어 검색을 가능하게 함
		tokenFilterList.add("nori_part_of_speech");
		
		//List<String> userDictionaryRules = Arrays.asList("낮말", "밤말");
		List<String> userDictionaryRules = readWordsFromFile(dictionaryRulesPath);
		
		// 한글형태소분석기인 Nori 플러그인이 미리 설치되어 있어야 함
		NoriTokenizer noriTokenizer = new NoriTokenizer.Builder()
				.decompoundMode(NoriDecompoundMode.Discard)
				.discardPunctuation(true)
				.userDictionaryRules(userDictionaryRules)
				.build();
		
		Tokenizer tokenizer = new Tokenizer.Builder().definition(noriTokenizer._toTokenizerDefinition()).build();
		tokenizerMap.put("nori-tokenizer", tokenizer);
		
		// 커스텀 Analyzer 구성 : char_filter ==> tokenizer ==> token filter
		CustomAnalyzer noriAnalyzer = new CustomAnalyzer.Builder()
				.charFilter(charFilterList)
				.tokenizer("nori-tokenizer")
				.filter(tokenFilterList).build();
		
		Analyzer analyzer = new Analyzer.Builder().custom(noriAnalyzer).build();
		analyzerMap.put("nori-analyzer", analyzer);
		
		/* normalizer 설정 : term query와 같은 분석기를 사용하지 않는 질의에 적용된다. 
		normalizerMap.put("keyword_normalizer", new Normalizer.Builder()
                .custom(new CustomNormalizer.Builder().charFilter("patternfilter").filter(tokenFilterList).build())
                .build());
		*/

		CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
			    .index(indexName)
			    .settings(s -> s	
			        .analysis(a -> a
			        		.charFilter(charFilterMap)
			        		//.normalizer(normalizerMap)
			        		.tokenizer(tokenizerMap)
			        		.filter(tokenFilterMap)
			        		.analyzer(analyzerMap)
                    )                
			    )
			    .mappings(m -> m
			        
			        .properties("director", p -> p
			            .text(f -> f
			                .index(true)
			                .analyzer("nori-analyzer")
			            )
			        )
			        .properties("title", p -> p
				            .text(f -> f
				                .index(true)
				                .analyzer("nori-analyzer")
				            )
				        )
			        .properties("year", p -> p
				            .long_(f -> f
				                .index(true)
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
	public SearchResponse<Movie> searchAll(String indexName) throws IOException {
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
	public SearchResponse<Movie> search(String indexName, String query) throws IOException {
		SearchRequest textSearchRequest = new SearchRequest.Builder()
				.index(indexName)
				.size(100)
				.query(q -> q.match(m -> m.field("title").query(FieldValue.of(query)).analyzer("nori-analyzer").fuzziness("AUTO")))
			    .build();
		
		SearchResponse<Movie> searchResponse = client.search(textSearchRequest, Movie.class);
		
		for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
			log.debug("search result:::::"+searchResponse.hits().hits().get(i).source());
		}
		return searchResponse;
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
	
	private static List<String> readWordsFromFile(String filePath) {
        List<String> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return words;
    }

}
