package egovframework.example.sample.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import egovframework.example.sample.entity.Qna;

public interface QnaRepository extends JpaRepository<Qna, String> {
	
	@Query(value = "SELECT QA_ID as qaId, QESTN_SJ as qestnSj, QESTN_CN as qestnCn, ANSWER_CN as answerCn from LET_TN_QA_INFO\r\n"
			+ "WHERE DELETE_YN = 'N' AND ANSWER_CN IS NOT NULL ORDER BY QA_ID DESC LIMIT 100", nativeQuery = true)
	List<QnaInfo> findQnaInfo();
	
	public static interface QnaInfo {
		
		String getQaId();
		
		String getQestnSj();
		
		String getQestnCn();
		
		String getAnswerCn();
		
	}

}
