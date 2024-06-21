package egovframework.example.sample.index;

import egovframework.example.sample.service.SampleDefaultVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "테스트 데이터 VO(영화)")
@AllArgsConstructor
@NoArgsConstructor
public class Movie extends SampleDefaultVO {
	
	private static final long serialVersionUID = -5131252926879440177L;
	
	private String Director;
    private String Title;
    private Integer Year;
    
}
