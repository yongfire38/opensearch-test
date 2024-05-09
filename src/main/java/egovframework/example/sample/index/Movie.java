package egovframework.example.sample.index;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "테스트 데이터 VO(영화)")
@AllArgsConstructor
@NoArgsConstructor
public class Movie {
	
	private String Director;
    private String Title;
    private Integer Year;
    
}
