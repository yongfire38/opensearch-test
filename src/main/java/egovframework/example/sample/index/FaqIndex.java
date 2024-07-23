package egovframework.example.sample.index;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "테스트 데이터 VO(묻고 답하기)")
@AllArgsConstructor
@NoArgsConstructor
public class FaqIndex {
	
	private String questionSubject;
    private String questionContent;
    private String answerContent;

}
