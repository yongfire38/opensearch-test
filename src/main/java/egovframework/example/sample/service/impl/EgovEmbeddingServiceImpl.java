package egovframework.example.sample.service.impl;

import static java.time.Duration.ofSeconds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;
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
import egovframework.example.cmm.util.ReadWords;
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
	
	@Value("${bulk.insert.jsontext}")
	public String jsonFilePath;
	
	@Value("${synonyms.path}")
	public String synonymsPath;
	
	@Value("${dictionary.path}")
	public String dictionaryRulesPath;
	
	int index = 1;
	
	@Override
	public void createEmbeddingIndex(String indexName) throws IOException {
		
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
        List<String> synonym = ReadWords.readWordsFromFile(synonymsPath);
        
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
		List<String> userDictionaryRules = ReadWords.readWordsFromFile(dictionaryRulesPath);
		
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
			    	//.numberOfShards("2")
			    	//.numberOfReplicas("1")
			        .knn(true)
			        .analysis(a -> a
			        		.charFilter(charFilterMap)
			        		//.normalizer(normalizerMap)
			        		.tokenizer(tokenizerMap)
			        		.filter(tokenFilterMap)
			        		.analyzer(analyzerMap)
                    )       		
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
			                .analyzer("nori-analyzer")
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
	public SearchResponse<JsonNode> vectorSearch(String indexName, String query) throws IOException {
		
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
	public SearchResponse<JsonNode> textSearch(String indexName, String query) throws IOException {

		SearchRequest textSearchRequest = new SearchRequest.Builder()
				.index(indexName)
				.query(q -> q.match(m -> m.field("text").query(FieldValue.of(query)).analyzer("nori-analyzer").fuzziness("AUTO")))
			    .build();
		
		SearchResponse<JsonNode> textSearchResponse = client.search(textSearchRequest, JsonNode.class);
		
		return textSearchResponse;
		
	}

	@Override
	public void insertEmbeddingData(String indexName) throws IOException {
		
		long beforeTime = System.currentTimeMillis();
		
		//문자열 파일을 읽어서 json 파일로 변환, 이 과정에서 각 문자열을 임베딩한 결과도 추가한다 
		//toJsonConverter 호출...
		toJsonConverter();
		
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
		
		long afterTime = System.currentTimeMillis(); 
		long secDiffTime = (afterTime - beforeTime)/1000;
		
		log.debug("시간차이(m) : " + secDiffTime + "초");
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
