package egovframework.example.sample.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.opensearch.client.opensearch.OpenSearchClient;
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
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import egovframework.example.cmm.util.ReadWords;
import egovframework.example.sample.repository.FaqRepository;
import egovframework.example.sample.repository.FaqRepository.FaqInfo;
import egovframework.example.sample.service.EgovFaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("faqService")
@Slf4j
@RequiredArgsConstructor
public class EgovFaqServiceImpl extends EgovAbstractServiceImpl implements EgovFaqService {
	
	@Value("${synonyms.path}")
	public String synonymsPath;
	
	@Value("${dictionary.path}")
	public String dictionaryRulesPath;
	
	@Value("${stoptags.path}")
	public String stopTagsPath;
	
	private final OpenSearchClient client;
	
	@Autowired
    private FaqRepository faqRepository;

	@Override
	public void createIndex(String indexName) throws IOException {
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
		List<String> stopTags = ReadWords.readWordsFromFile(stopTagsPath);
		
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
	public List<FaqInfo> getFaqInfo() {
		List<FaqInfo> faqInfoList = faqRepository.findFaqInfo();
		
		return faqInfoList;
	}

}
