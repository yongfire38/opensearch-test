package egovframework.example.sample.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import egovframework.example.sample.entity.Faq;


public interface FaqRepository extends JpaRepository<Faq, String>{
	
	@Query(value = "SELECT FAQ_ID as faqId, QESTN_SJ as questionSubject, QESTN_CN as questionContent, ANSWER_CN as AnswerContent from LET_TN_FAQ_INFO\r\n"
			+ "ORDER BY FAQ_ID DESC LIMIT 100", nativeQuery = true)
	List<FaqInfo> findFaqInfo();

	public static interface FaqInfo {
			
			String getFaqId();
			
			String getQuestionSubject();
			
			String getQuestionContent();
			
			String getAnswerContent();
			
		}
}


