package egovframework.example.sample.web;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import egovframework.example.cmm.ResponseCode;
import egovframework.example.cmm.ResultVO;
import egovframework.example.sample.repository.FaqRepository.FaqInfo;
import egovframework.example.sample.service.EgovFaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@Tag(name="EgovFaqController",description = "테스트용 CONTROLLER(묻고답하기 테스트)")
public class EgovFaqController {
	
	@Resource(name="faqService")
	private EgovFaqService faqService;
	
	@Operation(
			summary = "인덱스 생성",
			description = "mysql 테이블과 연동되는 OpenSearch(text) 인덱스를 생성",
			tags = {"EgovFaqController"}
	)
	@GetMapping("/createMysqlIndex/{indexName}")
	public ResultVO createColorIndex(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch createIndex...");
			faqService.createIndex(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch create vecIndex Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "일괄 조회",
			description = "쿼리를 실행하여 테이블의 데이터를 얻는다",
			tags = {"EgovFaqController"}
	)
	@GetMapping("/getFaqs")
	public List<FaqInfo> getItems() {
		return faqService.getFaqInfo();
	}

}
