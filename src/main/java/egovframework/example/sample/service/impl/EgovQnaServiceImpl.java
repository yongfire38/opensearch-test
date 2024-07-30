package egovframework.example.sample.service.impl;

import java.io.IOException;
import java.util.ArrayList;
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
import org.opensearch.client.opensearch._types.analysis.LowercaseTokenFilter;
import org.opensearch.client.opensearch._types.analysis.NoriDecompoundMode;
import org.opensearch.client.opensearch._types.analysis.NoriPartOfSpeechTokenFilter;
import org.opensearch.client.opensearch._types.analysis.NoriTokenizer;
import org.opensearch.client.opensearch._types.analysis.PatternReplaceCharFilter;
import org.opensearch.client.opensearch._types.analysis.SynonymGraphTokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.Tokenizer;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.PoolingMode;
import dev.langchain4j.model.output.Response;
import egovframework.example.cmm.util.StrUtil;
import egovframework.example.sample.repository.QnaRepository;
import egovframework.example.sample.repository.QnaRepository.QnaInfo;
import egovframework.example.sample.service.EgovQnaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("qnaService")
@Slf4j
@RequiredArgsConstructor
public class EgovQnaServiceImpl extends EgovAbstractServiceImpl implements EgovQnaService {
	
	@Value("${synonyms.path}")
	public String synonymsPath;
	
	@Value("${dictionary.path}")
	public String dictionaryRulesPath;
	
	@Value("${stoptags.path}")
	public String stopTagsPath;
	
	@Value("${huggingface.access.token}")
	public String accessToken;
	
	private final OpenSearchClient client;
	
	@Autowired
	private QnaRepository qnaRepository;
	
	@Override
	public void createTextIndex(String indexName) throws IOException {
		Map<String, Tokenizer> tokenizerMap = new HashMap<>();
		Map<String, Analyzer> analyzerMap = new HashMap<>();
		Map<String, TokenFilter> tokenFilterMap = new HashMap<>();
		Map<String, CharFilter> charFilterMap = new HashMap<>();
		
		// 줄바꿈 및 \를 공백으로 대체
		PatternReplaceCharFilter patternCharFilter = new PatternReplaceCharFilter.Builder().pattern("[\\r\\n\\\\]").replacement(" ").flags("CASE_INSENSITIVE|MULTILINE").build();
		CharFilter chrFilter =  new CharFilter.Builder().definition(patternCharFilter._toCharFilterDefinition()).build();
		charFilterMap.put("patternfilter", chrFilter);
		
		// remove punctuation chars : 구두점을 제거한다
		PatternReplaceCharFilter punctuationCharFilter = new PatternReplaceCharFilter.Builder().pattern("\\p{Punct}").replacement("").flags("CASE_INSENSITIVE|MULTILINE").build();
		CharFilter chrPatternFilter =  new CharFilter.Builder().definition(punctuationCharFilter._toCharFilterDefinition()).build();
		charFilterMap.put("punctuationCharFilter", chrPatternFilter);
		
		List<String> charFilterList = new ArrayList<>();
		charFilterList.add("patternfilter");
		charFilterList.add("punctuationCharFilter");
		
		// 제거할 품사를 열거한다
		List<String> stopTags = StrUtil.readWordsFromFile(stopTagsPath);
		
		// Token filter : 소문자 변환 / 비ASCII 문자를 ASCII 문자로 변환 / 한국어의 특정 품사를 제거
		LowercaseTokenFilter lowerFilter = new LowercaseTokenFilter.Builder().build();
        AsciiFoldingTokenFilter asciiFilter = new AsciiFoldingTokenFilter.Builder().preserveOriginal(false).build();
        NoriPartOfSpeechTokenFilter noriPartOfSpeechFilter = new NoriPartOfSpeechTokenFilter.Builder().stoptags(stopTags).build();        
        tokenFilterMap.put("lowercase", new TokenFilter.Builder().definition(lowerFilter._toTokenFilterDefinition()).build());
        tokenFilterMap.put("asciifolding", new TokenFilter.Builder().definition(asciiFilter._toTokenFilterDefinition()).build());
        tokenFilterMap.put("nori_part_of_speech", new TokenFilter.Builder().definition(noriPartOfSpeechFilter._toTokenFilterDefinition()).build());
        
        //List<String> synonym = Arrays.asList("amazon, aws", "풋사과, 햇사과, 사과");
        List<String> synonym = StrUtil.readWordsFromFile(synonymsPath);
        
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
		List<String> userDictionaryRules = StrUtil.readWordsFromFile(dictionaryRulesPath);
				
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
		
		CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
			    .index(indexName)
			    .settings(s -> s	
			        .analysis(a -> a
			        		.charFilter(charFilterMap)
			        		.tokenizer(tokenizerMap)
			        		.filter(tokenFilterMap)
			        		.analyzer(analyzerMap)
                    )                
			    )
			    .mappings(m -> m
			        
			        .properties("questionSubject", p -> p
			            .text(f -> f
			                .index(true)
			                .analyzer("nori-analyzer")
			            )
			        )
			        .properties("questionContent", p -> p
				            .text(f -> f
				                .index(true)
				                .analyzer("nori-analyzer")
				            )
				        )
			        .properties("answerContent", p -> p
				            .text(f -> f
				                .index(true)
				                .analyzer("nori-analyzer")
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
	public void createEmbeddingIndex(String indexName) throws IOException {
		Map<String, Tokenizer> tokenizerMap = new HashMap<>();
		Map<String, Analyzer> analyzerMap = new HashMap<>();
		Map<String, TokenFilter> tokenFilterMap = new HashMap<>();
		Map<String, CharFilter> charFilterMap = new HashMap<>();
		
		// 줄바꿈 및 \를 공백으로 대체
		PatternReplaceCharFilter patternCharFilter = new PatternReplaceCharFilter.Builder().pattern("[\\r\\n\\\\]").replacement(" ").flags("CASE_INSENSITIVE|MULTILINE").build();
		CharFilter chrFilter =  new CharFilter.Builder().definition(patternCharFilter._toCharFilterDefinition()).build();
		charFilterMap.put("patternfilter", chrFilter);
		
		// remove punctuation chars : 구두점을 제거한다
		PatternReplaceCharFilter punctuationCharFilter = new PatternReplaceCharFilter.Builder().pattern("\\p{Punct}").replacement("").flags("CASE_INSENSITIVE|MULTILINE").build();
		CharFilter chrPatternFilter =  new CharFilter.Builder().definition(punctuationCharFilter._toCharFilterDefinition()).build();
		charFilterMap.put("punctuationCharFilter", chrPatternFilter);
		
		List<String> charFilterList = new ArrayList<>();
		charFilterList.add("patternfilter");
		charFilterList.add("punctuationCharFilter");
		
		// 제거할 품사를 열거한다
		List<String> stopTags = StrUtil.readWordsFromFile(stopTagsPath);
		
		// Token filter : 소문자 변환 / 비ASCII 문자를 ASCII 문자로 변환 / 한국어의 특정 품사를 제거
		LowercaseTokenFilter lowerFilter = new LowercaseTokenFilter.Builder().build();
        AsciiFoldingTokenFilter asciiFilter = new AsciiFoldingTokenFilter.Builder().preserveOriginal(false).build();
        NoriPartOfSpeechTokenFilter noriPartOfSpeechFilter = new NoriPartOfSpeechTokenFilter.Builder().stoptags(stopTags).build();        
        tokenFilterMap.put("lowercase", new TokenFilter.Builder().definition(lowerFilter._toTokenFilterDefinition()).build());
        tokenFilterMap.put("asciifolding", new TokenFilter.Builder().definition(asciiFilter._toTokenFilterDefinition()).build());
        tokenFilterMap.put("nori_part_of_speech", new TokenFilter.Builder().definition(noriPartOfSpeechFilter._toTokenFilterDefinition()).build());
        
        //List<String> synonym = Arrays.asList("amazon, aws", "풋사과, 햇사과, 사과");
        List<String> synonym = StrUtil.readWordsFromFile(synonymsPath);
        
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
		List<String> userDictionaryRules = StrUtil.readWordsFromFile(dictionaryRulesPath);
				
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
		
		CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
			    .index(indexName)
			    .settings(s -> s
			    	.knn(true)
			        .analysis(a -> a
			        		.charFilter(charFilterMap)
			        		.tokenizer(tokenizerMap)
			        		.filter(tokenFilterMap)
			        		.analyzer(analyzerMap)
                    )                
			    )
			    .mappings(m -> m
			        .properties("questionSubject", p -> p
			            .text(f -> f
			                .index(true)
			                .analyzer("nori-analyzer")
			            )
			        )
			        .properties("questionContent", p -> p
				            .text(f -> f
				                .index(true)
				                .analyzer("nori-analyzer")
				            )
				        )
			        .properties("answerContent", p -> p
				            .text(f -> f
				                .index(true)
				                .analyzer("nori-analyzer")
				            )
				        )
			        .properties("questionEmbedding", p -> p
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
	public void insertData(String indexName) {
		
		long beforeTime = System.currentTimeMillis();
		
		BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
		
		// step 1. 네이티브 쿼리를 던져서 데이터를 얻는다
		List<QnaInfo> qnaInfoList = qnaRepository.findQnaInfo();
		
		// step 2. 얻어낸 데이터 전처리
		qnaInfoList.stream().map(qnaInfo -> {
			Map<String, Object> dataMap = new HashMap<>();
			// QnaInfo 객체의 필드들을 정리하여 Map에 추가
			dataMap.put("id", qnaInfo.getQaId());
			dataMap.put("questionSubject", StrUtil.cleanString(qnaInfo.getQestnSj()));
			dataMap.put("questionContent", StrUtil.cleanString(qnaInfo.getQestnCn()));
			dataMap.put("answerContent", StrUtil.cleanString(qnaInfo.getAnswerCn()));
			
			return dataMap;
		})
		.forEach(dataMap -> bulkRequestBuilder.operations(ops -> 
    	ops.index(IndexOperation.of(io -> 
        	io.index(indexName).id(String.valueOf(dataMap.get("id"))).document(dataMap)))
	    ));
		
		// step 3. 전처리한 데이터를 인덱싱한다 
		try {
		    BulkResponse bulkResponse = client.bulk(bulkRequestBuilder.build());
		    if (bulkResponse.errors()) {
		    	log.debug("Bulk operation had errors");
		    } else {
		    	log.debug("Bulk operation completed successfully");
		    	
		    	long afterTime = System.currentTimeMillis(); 
				long secDiffTime = (afterTime - beforeTime)/1000;
				
				log.debug("시간차이(m) : " + secDiffTime + "초");
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	
	@Override
	public void insertEmbeddingData(String indexName) {
		long beforeTime = System.currentTimeMillis();
		
		/* 실패
		EmbeddingModel embeddingModel = new OnnxEmbeddingModel(
				"./model/KR-SBERT-V40K-klueNLI-augSTS/model.onnx",
				"./model/KR-SBERT-V40K-klueNLI-augSTS/tokenizer.json",
                PoolingMode.MEAN);
		*/
		
		// 성공
		EmbeddingModel embeddingModel = new OnnxEmbeddingModel(
				"./model/ko-sroberta-multitask/model.onnx",
				"./model/ko-sroberta-multitask/tokenizer.json",
                PoolingMode.MEAN);
		
		/* 실패
		EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(accessToken)
                .modelId("snunlp/KR-SBERT-V40K-klueNLI-augSTS")
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .build();
        */
		
		/* 성공
		EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(accessToken)
                .modelId("jhgan/ko-sroberta-multitask")
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .build();
        */
		
		BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
		
		// step 1. 네이티브 쿼리를 던져서 데이터를 얻는다
		List<QnaInfo> qnaInfoList = qnaRepository.findQnaInfo();
		
		// step 2. 얻어낸 데이터 전처리 (+임베딩을 수행, 임베딩은 질문 내용만 임베딩한다)
		qnaInfoList.stream().map(qnaInfo -> {
			Map<String, Object> dataMap = new HashMap<>();
			
			Embedding questionResponse = embeddingModel.embed(StrUtil.cleanString(qnaInfo.getQestnCn())).content();
			//Embedding answerResponse = embeddingModel.embed(StrUtil.cleanString(qnaInfo.getAnswerCn())).content();
			
			float[] questionEmbeddings =  questionResponse.vector();
			//float[] answerEmbeddings = answerResponse.vector();
			
			// QnaInfo 객체의 필드들을 정리하여 Map에 추가
			dataMap.put("id", qnaInfo.getQaId());
			dataMap.put("questionSubject", StrUtil.cleanString(qnaInfo.getQestnSj()));
			dataMap.put("questionContent", StrUtil.cleanString(qnaInfo.getQestnCn()));
			dataMap.put("answerContent", StrUtil.cleanString(qnaInfo.getAnswerCn()));
			dataMap.put("questionEmbedding", questionEmbeddings);
			//dataMap.put("answerEmbedding", answerEmbeddings);
			
			return dataMap;
		})
		.forEach(dataMap -> bulkRequestBuilder.operations(ops -> 
    	ops.index(IndexOperation.of(io -> 
        	io.index(indexName).id(String.valueOf(dataMap.get("id"))).document(dataMap)))
	    ));
		
		// step 3. 임베딩 값 추가 및 전처리한 데이터를 인덱싱한다
		try {
		    BulkResponse bulkResponse = client.bulk(bulkRequestBuilder.build());
		    if (bulkResponse.errors()) {
		    	log.debug("Bulk operation had errors");
		    } else {
		    	log.debug("Bulk operation completed successfully");
		    	
		    	long afterTime = System.currentTimeMillis(); 
				long secDiffTime = (afterTime - beforeTime)/1000;
				
				log.debug("시간차이(m) : " + secDiffTime + "초");
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
	}

	@Override
	public SearchResponse<JsonNode> textSearch(String indexName, String query) throws IOException {
		
		// questionContent 컬럼을 대상으로 검색 (5건까지 조회)
		SearchRequest textSearchRequest = new SearchRequest.Builder()
				.index(indexName)
				.size(5)
				.query(q -> q.match(m -> m.field("questionContent").query(FieldValue.of(query)).analyzer("nori-analyzer").fuzziness("AUTO")))
			    .build();
		
		SearchResponse<JsonNode> textSearchResponse = client.search(textSearchRequest, JsonNode.class);
		
		return textSearchResponse;
	}
	
	@Override
	public SearchResponse<JsonNode> vectorSearch(String indexName, String query) throws IOException {
		
		EmbeddingModel embeddingModel = new OnnxEmbeddingModel(
				"./model/ko-sroberta-multitask/model.onnx",
				"./model/ko-sroberta-multitask/tokenizer.json",
                PoolingMode.MEAN);
		
		Response<Embedding> response = embeddingModel.embed(query);

		// questionEmbedding 컬럼을 대상으로 검색 (유사한 순으로 5건까지 조회)
		SearchRequest searchRequest = new SearchRequest.Builder()
				.index(indexName)
				.query(q -> q
						.knn(k -> k
							.field("questionEmbedding")
							.vector(response.content().vector())
							.k(5)
						)
				)
			.build();
				
		SearchResponse<JsonNode> searchResponse = client.search(searchRequest, JsonNode.class);
				
		for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
			log.debug(":::::"+searchResponse.hits().hits().get(i).source().get("questionContent") + " with score " + searchResponse.hits().hits().get(i).score());
		}
				
		return searchResponse;
		
	}
	
	@Override
	public List<QnaInfo> getQnaInfo() {
		List<QnaInfo> qnaInfoList = qnaRepository.findQnaInfo();
		
		return qnaInfoList;
	}

}
