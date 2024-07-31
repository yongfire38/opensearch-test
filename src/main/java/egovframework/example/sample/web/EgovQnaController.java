package egovframework.example.sample.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import egovframework.example.cmm.ResponseCode;
import egovframework.example.cmm.ResultVO;
import egovframework.example.sample.repository.QnaRepository.QnaInfo;
import egovframework.example.sample.service.EgovQnaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@Tag(name="EgovQnaController",description = "테스트용 CONTROLLER(묻고 답하기 테스트)")
public class EgovQnaController {
	
	@Resource(name="qnaService")
	private EgovQnaService qnaService;
	
	@Operation(
			summary = "인덱스 생성",
			description = "mysql 테이블과 연동되는 OpenSearch(text) 인덱스를 생성",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/createMysqlQnaIndex/{indexName}")
	public ResultVO createTextIndex(@PathVariable String indexName) {
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch createIndex...");
			qnaService.createTextIndex(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch create vecIndex Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	
	@Operation(
			summary = "임베딩 값이 포함된 인덱스 생성",
			description = "mysql 테이블과 연동되는 OpenSearch(embedding) 인덱스를 생성",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/createMysqlQnaEmbeddingIndex/{indexName}")
	public ResultVO createEmbeddingIndex(@PathVariable String indexName) {
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch createIndex...");
			qnaService.createEmbeddingIndex(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch create vecIndex Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	
	@Operation(
			summary = "데이터 추가",
			description = "OpenSearch 인덱스(text)에 mySql 테이블의 데이터를 추가(벌크 insert)",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/insertMysqlTextData/{indexName}")
	public ResultVO insertMysqlTextData(@PathVariable String indexName) {
		ResultVO resultVO = new ResultVO();
		
		try {
			qnaService.insertData(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.debug("##### OpenSearch insertMysqlData Complete");
		
		return resultVO;
	}
	
	@Operation(
			summary = "임베딩 값이 포함된 데이터를 쿼리에서 얻은 레코드 수만큼 추가",
			description = "OpenSearch 인덱스(embedding)에 mySql 테이블의 데이터를 추가(벌크 insert)",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/insertMysqlEmbeddingData/{indexName}")
	public ResultVO insertMysqlEmbeddingData(@PathVariable String indexName) {
		ResultVO resultVO = new ResultVO();
		
		try {
			qnaService.insertEmbeddingData(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.debug("##### OpenSearch insertMysqlEmbeddingData Complete");
		
		return resultVO;
	}
	
	@Operation(
			summary = "임베딩 값이 포함된 데이터를 전부 추가",
			description = "OpenSearch 인덱스(embedding)에 mySql 테이블의 데이터를 추가(분할 insert)",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/insertSplitedEmbeddingData/{indexName}")
	public ResultVO insertSplitedEmbeddingData(@PathVariable String indexName) {
		ResultVO resultVO = new ResultVO();
		
		try {
			qnaService.insertSplitedEmbeddingData(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.debug("##### OpenSearch insertSplitedEmbeddingData Complete");
		
		return resultVO;
	}
	
	@Operation(
			summary = "단어 검색(Term) 수행",
			description = "질문 내용을 대상으로 단어 검색 수행. 대소문자는 구분하지 않는다",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/termSearch/{indexName}/{query}")
	public ResultVO termSearch(@PathVariable String indexName, @PathVariable String query) throws IOException {
		ResultVO resultVO = new ResultVO();
		Map<String, Object> totalResultMap = new HashMap<String, Object>();

		List<Object> resultList = new ArrayList<>();

		try {
			SearchResponse<JsonNode> searchResponse = qnaService.termSearch(indexName, query);

			for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
				Map<String, Object> resultMap = createResultMap(searchResponse, i);
				resultList.add(resultMap);
			}
			totalResultMap.put("resultList", resultList);

			resultVO.setResult(totalResultMap);
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());

			log.debug("##### OpenSearch getTermData Complete");

		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultVO;
	}
	
	@Operation(
			summary = "한국어 텍스트 검색(Text) 수행",
			description = "질문 내용을 대상으로 한국어 텍스트 검색 수행. 약간의 오타는 무시하는 설정 추가",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/koreanTextSearch/{indexName}/{query}")
	public ResultVO textSearch(@PathVariable String indexName, @PathVariable String query) throws IOException {
		ResultVO resultVO = new ResultVO();
		Map<String, Object> totalResultMap = new HashMap<String, Object>();

		List<Object> resultList = new ArrayList<>();

		try {
			SearchResponse<JsonNode> searchResponse = qnaService.textSearch(indexName, query);

			for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
				Map<String, Object> resultMap = createResultMap(searchResponse, i);
				resultList.add(resultMap);
			}
			totalResultMap.put("resultList", resultList);

			resultVO.setResult(totalResultMap);
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());

			log.debug("##### OpenSearch getKoreanTextData Complete");

		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultVO;
	}
	
	@Operation(
			summary = "텍스트를 기초로 한 벡터 검색 수행",
			description = "질문 내용을 대상으로 벡터 데이터가 있는 인덱스의 데이터를 벡터 검색",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/vecSearch/{indexName}/{query}")
	public ResultVO vectorSearch(@PathVariable String indexName, @PathVariable String query) throws IOException {
		ResultVO resultVO = new ResultVO();
		Map<String, Object> totalResultMap = new HashMap<String, Object>();

		List<Object> resultList = new ArrayList<>();

		try {
			SearchResponse<JsonNode> searchResponse = qnaService.vectorSearch(indexName, query);

			for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
				Map<String, Object> resultMap = createResultMap(searchResponse, i);
				resultList.add(resultMap);
			}

			totalResultMap.put("resultList", resultList);

			resultVO.setResult(totalResultMap);
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());

			log.debug("##### OpenSearch getTextVecData Complete");

		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultVO;
	}
	
	@Operation(
			summary = "일괄 조회",
			description = "쿼리를 실행하여 mySql 테이블의 데이터를 얻는다",
			tags = {"EgovQnaController"}
	)
	@GetMapping("/getQnas")
	public List<QnaInfo> getItems() {
		return qnaService.getQnaInfo();
	}
	
	private Map<String, Object> createResultMap(SearchResponse<JsonNode> searchResponse, int index) {
		Map<String, Object> resultMap = new HashMap<>();
	    resultMap.put("score", searchResponse.hits().hits().get(index).score());
	    resultMap.put("id", searchResponse.hits().hits().get(index).source().get("id"));
	    resultMap.put("questionContent", searchResponse.hits().hits().get(index).source().get("questionContent"));
	    return resultMap;
	}

}
