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
import egovframework.example.sample.service.EgovAltEmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@Tag(name="EgovAltEmbeddingController",description = "테스트용 CONTROLLER(별도 모델의 임베딩 테스트)")
public class EgovAltEmbeddingController {
	
	@Resource(name="altEmbeddingService")
	private EgovAltEmbeddingService altEmbeddingService;
	
	@Operation(
			summary = "텍스트를 기초로 한 벡터 검색(Text) 수행 - 별도 모델",
			description = "벡터 데이터(Text)가 있는 인덱스의 데이터를 텍스트를 받아서 벡터 검색 - 별도 모델",
			tags = {"EgovAltEmbeddingController"}
	)
	@GetMapping("/altTextList/{indexName}/{query}")
	public ResultVO altTextVecData(@PathVariable String indexName, @PathVariable String query) throws IOException {
		
		ResultVO resultVO = new ResultVO();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		List<JsonNode> resultList = new ArrayList<>();
		
		try {
			SearchResponse<JsonNode> searchResponse = altEmbeddingService.vectorAltSearch(indexName, query);
			
			for (int i = 0; i< searchResponse.hits().hits().size(); i++) {
				resultList.add(searchResponse.hits().hits().get(i).source().get("text"));
		      }
			resultMap.put("resultList", resultList);
			
			resultVO.setResult(resultMap);
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch getAltTextVecData Complete");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "데이터 추가 - 별도 모델",
			description = "OpenSearch 인덱스(text)에 임베딩된 데이터를 추가(벌크 insert, 별도 모델)",
			tags = {"EgovAltEmbeddingController"}
	)
	@GetMapping("/insertAltEmbeddingData/{indexName}")
	public ResultVO insertAltEmbeddingData(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			altEmbeddingService.insertAltEmbeddingData(indexName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.debug("##### OpenSearch insertAltTextVecData Complete");
		
		return resultVO;
	}
	
	@Operation(
			summary = "json 변환 - 별도 모델",
			description = "문자열 파일을 읽어서 json 파일로 변환, 이 과정에서 각 문자열을 임베딩한 결과도 추가(별도 모델)",
			tags = {"EgovAltEmbeddingController"}
	)
	@GetMapping("/altConvertToJson")
	public ResultVO altConvertToJson() {
		
		ResultVO resultVO = new ResultVO();
		try {
			altEmbeddingService.toJsonAltConverter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
		
	}

}
