package egovframework.example.sample.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import egovframework.example.sample.entity.Qna;

public interface QnaRepository extends JpaRepository<Qna, String> {
	
	@Query(value = "SELECT QA_ID as qaId, QESTN_SJ as qestnSj, QESTN_CN as qestnCn, ANSWER_CN as answerCn from LET_TN_QA_INFO\r\n"
			+ "WHERE DELETE_YN = 'N' AND ANSWER_CN IS NOT NULL ORDER BY QA_ID DESC LIMIT 2000", nativeQuery = true)
	List<QnaInfo> findTotalQnaInfo();
	
	@Query(value = "SELECT QA_ID as qaId, QESTN_SJ as qestnSj, QESTN_CN as qestnCn, ANSWER_CN as answerCn from LET_TN_QA_INFO\r\n"
			+ "WHERE DELETE_YN = 'N' AND ANSWER_CN IS NOT NULL ORDER BY QA_ID DESC LIMIT 2000 OFFSET :offset", nativeQuery = true)
	List<QnaInfo> findSplitedQnaInfo(@Param("offset") int offset);
	
	@Query(value = "SELECT CEIL(COUNT(QA_ID) / 2000) AS queryCnt FROM LET_TN_QA_INFO\r\n"
			+ "WHERE DELETE_YN = 'N' AND ANSWER_CN IS NOT NULL", nativeQuery = true)
	int findQueryCnt();
	
	public static interface QnaInfo {
		
		String getQaId();
		
		String getQestnSj();
		
		String getQestnCn();
		
		String getAnswerCn();
		
	}

}
