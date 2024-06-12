package egovframework.example.sample.service;

import java.util.List;

import egovframework.example.cmm.ResultVO;
import egovframework.example.sample.entity.Item;

/**
 * @author platformtech
 * postgre 테스트용
 *
 */
public interface EgovItemService {
	
	public ResultVO getVector(String text);
	
	public List<Item> getAllItems();
	
	public ResultVO getSimilarItems(String text);
	
}
