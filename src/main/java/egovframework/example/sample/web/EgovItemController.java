package egovframework.example.sample.web;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.example.cmm.ResultVO;
import egovframework.example.sample.entity.Item;
import egovframework.example.sample.service.EgovItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name="EgovItemController",description = "테스트용 CONTROLLER(postgreSql 테스트)")
public class EgovItemController {
	
	@Value("${huggingface.access.token}")
	public String accessToken;
	
	@Resource(name="itemService")
	private EgovItemService itemService;
	
	@Operation(
			summary = "벡터값 변환",
			description = "텍스트를 벡터로 받는다",
			tags = {"EgovItemController"}
	)
	@GetMapping("/getVector")
	public ResultVO getVector(String text) {
		return itemService.getVector(text);
		
	}
	
	@Operation(
			summary = "전체 조회",
			description = "벡터 데이터가 들어가 있는 테이블의 전체 검색",
			tags = {"EgovItemController"}
	)
	@GetMapping("/getItems")
	public List<Item> getItems() {
		return itemService.getAllItems();
	}
	
	@Operation(
			summary = "유사 데이터 조회",
			description = "벡터 데이터가 들어가 있는 테이블에서 요청 문자열과 유사결과 우선 검색",
			tags = {"EgovItemController"}
	)
	@GetMapping("/getSimilarItems")
	public ResultVO getSimilarItems(String text) {
		return itemService.getSimilarItems(text);
	}

}
