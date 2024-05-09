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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import egovframework.example.cmm.ResponseCode;
import egovframework.example.cmm.ResultVO;
import egovframework.example.sample.index.Color;
import egovframework.example.sample.service.EgovVecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@Tag(name="EgovVecController",description = "테스트용 CONTROLLER(벡터 테스트)")
public class EgovVecController {
	
	@Resource(name="vecService")
	private EgovVecService vecService;

	
	@Operation(
			summary = "인덱스 생성",
			description = "벡터 검색을 위한 OpenSearch(test) 인덱스를 생성",
			tags = {"EgovVecController"}
	)
	@GetMapping("/createVecIndex/{indexName}")
	public ResultVO createTestIndex(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch createIndex...");
			vecService.createTestIndex(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch create vecIndex Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "인덱스 생성",
			description = "벡터 RGB 검색을 위한 OpenSearch(color) 인덱스를 생성",
			tags = {"EgovVecController"}
	)
	@GetMapping("/createColorIndex/{indexName}")
	public ResultVO createColorIndex(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch createIndex...");
			vecService.createColorIndex(indexName);
			
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
			description = "OpenSearch 인덱스(test)에 테스트용 데이터를 추가(고정, 벌크 insert)",
			tags = {"EgovVecController"}
	)
	@GetMapping("/testInsert/{indexName}")
	public ResultVO insertTestData(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch insertData...");

			vecService.insertTestData(indexName);

			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());

			log.debug("##### OpenSearch insertData Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "데이터 추가",
			description = "OpenSearch 인덱스(color)에 RGB 데이터가 있는 json 파일을 파싱한 데이터를 추가(벌크 insert)",
			tags = {"EgovVecController"}
	)
	@GetMapping("/insert/{indexName}")
	public ResultVO insertColorData(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch insertData...");

			vecService.insertColorData(indexName);

			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());

			log.debug("##### OpenSearch insertData Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "벡터 검색 수행",
			description = "벡터 데이터가 있는 인덱스(test)의 데이터를 벡터 검색",
			tags = {"EgovVecController"}
	)
	@PostMapping("/vectorList/{indexName}")
	public ResultVO listTestData(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		List<JsonNode> resultList = new ArrayList<>();
		
		try {
			log.debug("##### OpenSearch getList...");
			SearchResponse<JsonNode> searchResponse = vecService.search(indexName);
			
			for (int i = 0; i< searchResponse.hits().hits().size(); i++) {
				resultList.add(searchResponse.hits().hits().get(i).source());
				log.debug(":::::"+searchResponse.hits().hits().get(i).source());
		      }
			
			resultMap.put("resultList", resultList);
			
			resultVO.setResult(resultMap);
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch getList Complete");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultVO;
	}
	
	@Operation(
			summary = "벡터 검색(RGB) 수행",
			description = "벡터 데이터(RGB)가 있는 인덱스(color)의 데이터를 벡터 검색",
			tags = {"EgovVecController"}
	)
	@PostMapping("/colorList/{indexName}")
	public ResultVO listColorData(@PathVariable String indexName, Color color) {
		
		ResultVO resultVO = new ResultVO();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		List<JsonNode> resultList = new ArrayList<>();
		
		try {
			log.debug("##### OpenSearch getColorList...");
			SearchResponse<JsonNode> searchResponse = vecService.colorSearch(indexName, color);
			
			for (int i = 0; i< searchResponse.hits().hits().size(); i++) {
				resultList.add(searchResponse.hits().hits().get(i).source());
				log.debug(":::::"+searchResponse.hits().hits().get(i).source());
		      }
			
			resultMap.put("resultList", resultList);
			
			resultVO.setResult(resultMap);
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch getColorList Complete");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultVO;
	}
	
	@Operation(
			summary = "텍스트를 기초로 한 벡터 검색(RGB) 수행",
			description = "벡터 데이터(RGB)가 있는 인덱스(color)의 데이터를 텍스트를 받아서 벡터 검색",
			tags = {"EgovVecController"}
	)
	@GetMapping("/colorList/{indexName}/{query}")
	public ResultVO textColorData(@PathVariable String indexName, @PathVariable String query) throws IOException {
		
		ResultVO resultVO = new ResultVO();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		List<JsonNode> resultList = new ArrayList<>();
		
		try {
			SearchResponse<JsonNode> searchResponse = vecService.colorTextSearch(indexName, query);
			
			for (int i = 0; i< searchResponse.hits().hits().size(); i++) {
				resultList.add(searchResponse.hits().hits().get(i).source());
				log.debug(":::::"+searchResponse.hits().hits().get(i).source());
				
		      }
			resultMap.put("resultList", resultList);
			
			resultVO.setResult(resultMap);
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch getTextColorData Complete");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
		
	}
	
}
