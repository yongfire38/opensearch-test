package egovframework.example.sample.web;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.example.sample.repository.FaqRepository.FaqInfo;
import egovframework.example.sample.service.EgovFaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name="EgovFaqController",description = "테스트용 CONTROLLER(묻고답하기 테스트)")
public class EgovFaqController {
	
	@Resource(name="faqService")
	private EgovFaqService faqService;
	
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
