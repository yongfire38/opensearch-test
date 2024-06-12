package egovframework.example.sample.service.impl;

import static java.time.Duration.ofSeconds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.output.Response;
import egovframework.example.cmm.ResponseCode;
import egovframework.example.cmm.ResultVO;
import egovframework.example.sample.entity.Item;
import egovframework.example.sample.repository.ItemRepository;
import egovframework.example.sample.service.EgovItemService;
import lombok.extern.slf4j.Slf4j;

@Service("itemService")
@Slf4j
public class EgovItemServiceImpl extends EgovAbstractServiceImpl implements EgovItemService {
	
	@Value("${huggingface.access.token}")
	public String accessToken;
	
	@Autowired
    private ItemRepository itemRepository;
	
	@Override
	public ResultVO getVector(String text) {
		ResultVO resultVO = new ResultVO();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		//질의 문자열을 벡터로 변환한다
		EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
		        .accessToken(accessToken)
		        .modelId("jhgan/ko-sroberta-multitask")
		        .waitForModel(true)
		        .timeout(ofSeconds(60))
		        .build();

		Response<Embedding> response = embeddingModel.embed(text);
		
		resultMap.put("resultVector", response.content().vector());
		        
		resultVO.setResult(resultMap);
		resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
		resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());   
		
		log.debug("##### getVector Complete");
		
		return resultVO;
	}

	@Override
	public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

	@Override
	public ResultVO getSimilarItems(String text) {
		
		ResultVO resultVO = new ResultVO();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		List<Map<String, Object>> resultList = new ArrayList<>();
		
		//질의 문자열을 벡터로 변환한다
		EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
		        .accessToken(accessToken)
		        .modelId("jhgan/ko-sroberta-multitask")
		        .waitForModel(true)
		        .timeout(ofSeconds(60))
		        .build();
		
		Response<Embedding> response = embeddingModel.embed(text);
		
		//얻은 벡터값을 문자열로 변환
		String vectorString = "[";
		for (float value : response.content().vector()) {
		    vectorString += value + ", ";
		}
		
		vectorString = vectorString.substring(0, vectorString.length() - 2) + "]";
		
		List<Item> resultItemList = itemRepository.findItemsSimilarity(vectorString);
		
		//얻어낸 데이터에서 embedding 컬럼의 값은 제외
		for (Item item : resultItemList) {
			Map<String, Object> itemMap = new HashMap<String, Object>();
			itemMap.put("id", item.getId());
			itemMap.put("title", item.getTitle());
			resultList.add(itemMap);
		}
		
		resultMap.put("resultList", resultList);
		
		resultVO.setResult(resultMap);
		resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
		resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
		
		return resultVO;
		
	}

}
