package egovframework.example.sample.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller
@Tag(name="EgovSampleController",description = "메인 페이지")
public class EgovSampleController {
	
	@Operation(
			summary = "템플릿 화면",
			description = "템플릿 화면을 출력",
			tags = {"EgovSampleController"}
	)
	@GetMapping("/fruit")
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
        
		return "fruit/fruit";
    }
	
}
