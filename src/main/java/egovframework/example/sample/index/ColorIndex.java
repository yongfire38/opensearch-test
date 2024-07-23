package egovframework.example.sample.index;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "테스트 데이터 VO(RGB)")
@AllArgsConstructor
@NoArgsConstructor
public class ColorIndex {

	private Integer red;
	private Integer green;
	private Integer blue;
}
