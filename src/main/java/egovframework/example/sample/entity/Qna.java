package egovframework.example.sample.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "LET_TN_QA_INFO")
@Getter
@Setter
public class Qna {
	
	// 묻고답하기ID
	@Id
	@Column(name = "QA_ID", length = 20, nullable = false)
    private String qaId;
	
	// 국문/영문 구분
	@Column(name = "QA_LANG_CD", length = 3, nullable = false)
	private String qaLangCd;
	
	// 질문제목
	@Column(name = "QESTN_SJ", length = 250, nullable = false)
	private String qestnSj;
    
	// 질문내용
	@Column(name = "QESTN_CN", length = 2500)
	private String qestnCn;
	
	// 등록일자
	@Column(name = "WRITNG_DE", length = 8, nullable = false)
	private String writngDe;
	
	// 조회수
	@Column(name = "INQIRE_CO", length = 11, nullable = false)
	private int inqireCo;
	
	// 전화번호앞자리
	@Column(name = "AREA_NO", length = 5)
	private String areaNo;
	
	// 전화번호중간자리
	@Column(name = "MIDDLE_TELNO", length = 5)
	private String middleTelno;
	
	// 전화번호뒷자리
	@Column(name = "END_TELNO", length = 5)
	private String endTelno;
	
	// 이메일주소
	@Column(name = "EMAIL_ADRES", length = 100)
	private String emailAdres;
	
	// 최초등록일시
	@Column(name = "FRST_REGISTER_PNTTM")
	private Date frstRegisterPnttm;
	
	// 최초등록자ID
	@Column(name = "FRST_REGISTER_ID", length = 20)
	private String frstRegisterId;
	
	// 최종수정일시
	@Column(name = "LAST_UPDUSR_PNTTM")
	private Date lastUpdusrPnttm;
	
	// 최종수정자ID
	@Column(name = "LAST_UPDUSR_ID", length = 20)
	private String lastUpdusrId;
	
	// 등록자명
	@Column(name = "WRTER_NM", length = 20)
	private String wrterNm;
	
	// 등록자 비밀번호
	@Column(name = "WRITNG_PASSWORD", length = 100)
	private String writngPassword;
	
	// 답변내용
	@Column(name = "ANSWER_CN", length = 2500)
	private String answerCn;
	
	// 답변에디터사용여부
	@Column(name = "ANSWER_EDITR_USE_YN", length = 1)
	private String answerEditrUseYn;
	
	// 답변첨부파일ID
	@Column(name = "ANSWER_FILE_ID", length = 20)
	private String answerFileId;
	
	// 답변일자
	@Column(name = "ANSWER_DE", length = 8)
	private String answerDe;
	
	// 답변등록일시
	@Column(name = "ANSWER_REG_DTM")
	private Date answerRegDtm;
	
	// 답변등록자ID
	@Column(name = "ANSWER_REG_ID", length = 20)
	private String answerRegId;
	
	// 답변수정일시
	@Column(name = "ANSWER_UPD_DTM")
	private Date answerUpdDtm;
	
	// 답변수정자ID
	@Column(name = "ANSWER_UPD_ID", length = 20)
	private String answerUpdId;
	
	// 이메일수신여부
	@Column(name = "EMAIL_ANSWER_AT", length = 1, nullable = false)
	private String emailAnswerAt;
	
	// 처리상태코드
	@Column(name = "QNA_PROCESS_STTUS_CODE", length = 1, nullable = false)
	private String qnaProcessSttusCode;
	
	// 질문첨부파일ID
	@Column(name = "ATCH_FILE_ID", length = 20)
	private String atchFileId;
	
	// 오픈커뮤니티 artifact_id
	@Column(name = "OPEN_ARTIFACT_ID", length = 20)
	private Long openArtifactId;
	
	// 오픈커뮤니티 메세지 ID
	@Column(name = "OPEN_ARTIFACT_MESSAGE_ID", length = 20)
	private Long openArtifactMessageId;
	
	// 질문구분코드
	@Column(name = "QESTN_ST", length = 8)
	private String qestnSt;
	
	// 질문구분_상세버전
	@Column(name = "QESTN_ST_DT", length = 20)
	private String qestnStDt;
	
	// 환경정보
	@Column(name = "ENV_INFO", length = 2000)
	private String envInfo;
	
	// 국가코드
	@Column(name = "NATIONALITY", length = 2)
	private String nationality;
	
	// 삭제여부
	@Column(name = "DELETE_YN", length = 1, nullable = false)
	private String deleteYn;
	
	// 삭제일시
	@Column(name = "DELETE_DTM")
	private Date deleteDtm;
	
	// 삭제자구분
	@Column(name = "DELETER_TY", length = 1)
	private String deleterTy;
	
	// 삭제자ID
	@Column(name = "DELETER_ID", length = 20)
	private String deleterId;

}
