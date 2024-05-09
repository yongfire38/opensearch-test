package egovframework.example.sample.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import egovframework.example.sample.index.Movie;
import egovframework.example.cmm.ResponseCode;
import egovframework.example.cmm.ResultVO;
import egovframework.example.sample.service.EgovMovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@Tag(name="EgovMovieController",description = "테스트용 CONTROLLER(영화)")
public class EgovMovieController {
	
	@Resource(name = "movieService")
	private EgovMovieService movieService;
	
	@Operation(
			summary = "템플릿 화면",
			description = "템플릿 화면을 출력",
			tags = {"EgovMovieController"}
	)
	@GetMapping("/")
    public String getFruit(Model model) {
    	
		Map<String, String> fruitmap = new HashMap<String, String>();
		
    	fruitmap.put("fruit1", "apple");
    	fruitmap.put("fruit2", "banana");
    	fruitmap.put("fruit3", "orange");
        model.addAttribute("fruit", fruitmap);
        
        
        /* MySQL에서 데이터 검색
        try {
        	
        	Class.forName("com.mysql.jdbc.Driver");
            String mysqlUrl = "jdbc:mysql://localhost:3306/solr";
            String username = "solr";
            String password = "solr01";
            Connection connection = DriverManager.getConnection(mysqlUrl, username, password);
            Statement statement = connection.createStatement();

            // MySQL에서 데이터 검색
            ResultSet resultSet = statement.executeQuery("SELECT id, product_name FROM solr_test");
            
            // 자원 해제
            connection.close();
              
        } catch(Exception e) {
        	e.printStackTrace();
        }
        */
        
        
        /* OpenSearch에서 데이터 검색(제네릭으로 처리한 경우)
        ResultVO vo = this.getList("my-index", Sample.class);
        
        List<Sample> resultSample = (List<Sample>) vo.getResult().get("resultList");
        
        for (int i = 0; i < resultSample.size(); i++) {
			log.debug(resultSample.get(i).toString());
		}
		*/
        
		return "fruit/fruit.html";
    }
	
	@Operation(
			summary = "전체 조회",
			description = "OpenSearch 인덱스의 모든 데이터를 조회",
			tags = {"EgovMovieController"}
	)
	@ResponseBody
	@GetMapping("/list/{indexName}")
	public ResultVO getList(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		List<Movie> resultList = new ArrayList<>();
		
		try {
			log.debug("##### OpenSearch getList...");
			SearchResponse<Movie> searchResponse = movieService.search(indexName);
			
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
			summary = "인덱스 생성",
			description = "OpenSearch 인덱스를 생성",
			tags = {"EgovMovieController"}
	)
	@ResponseBody
	@GetMapping("/createIndex/{indexName}")
	public ResultVO createIndex(@PathVariable String indexName) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch createIndex...");
			movieService.createIndex(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch createIndex Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "데이터 추가",
			description = "OpenSearch 인덱스에 데이터를 추가",
			tags = {"EgovMovieController"}
	)
	@ResponseBody
	@PostMapping("/insert/{indexName}/{id}")
	public ResultVO insertData(Movie movie, @PathVariable String indexName, @PathVariable String id) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch insertData...");

			movieService.insertData(indexName, movie, id);

			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());

			log.debug("##### OpenSearch insertData Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "데이터 수정",
			description = "OpenSearch 인덱스의 데이터를 수정",
			tags = {"EgovMovieController"}
	)
	@ResponseBody
	@PutMapping("/update/{indexName}/{id}")
	public ResultVO updateData(Movie movie, @PathVariable String indexName, @PathVariable String id) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch updateData...");
			movieService.updateData(indexName, movie, id);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());

			log.debug("##### OpenSearch updateData Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "데이터 삭제",
			description = "OpenSearch 인덱스의 데이터를 삭제",
			tags = {"EgovMovieController"}
	)
	@ResponseBody
	@DeleteMapping("/delete/{indexName}/{id}")
	public ResultVO deleteData(@PathVariable String indexName, @PathVariable String id) {
		
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch deleteData...");
			movieService.deleteData(indexName, id);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch deleteData Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
	
	@Operation(
			summary = "인덱스 삭제",
			description = "OpenSearch 인덱스를 삭제",
			tags = {"EgovMovieController"}
	)
	@ResponseBody
	@DeleteMapping("/deleteIndex/{indexName}")
	public ResultVO deleteIndex(@PathVariable String indexName) {
		ResultVO resultVO = new ResultVO();
		
		try {
			log.debug("##### OpenSearch deleteIndex...");
			movieService.deleteIndex(indexName);
			
			resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
			resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
			
			log.debug("##### OpenSearch deleteIndex Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resultVO;
	}
}
