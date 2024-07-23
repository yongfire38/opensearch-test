package egovframework.example.sample.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "LET_TN_FAQ_INFO")
@Getter
@Setter
public class Faq {
	
	@Id
	@Column(name = "FAQ_ID", length = 20, nullable = false)
    private String faqId;
	
	@Column(name = "QESTN_SJ", length = 1000)
	private String questionSubject;
    
	@Column(name = "QESTN_CN", length = 2500)
	private String questionContent;
    
	@Column(name = "ANSWER_CN", length = 2500)
	private String answerContent;
    
	@Column(name = "INQIRE_CO")
	private Integer inquiryCount;
    
	@Column(name = "ATCH_FILE_ID", length = 20)
	private String attachmentFileId;
    
	@Column(name = "FRST_REGISTER_PNTTM", nullable = false)
	private Date firstRegisterPointInTime;
    
	@Column(name = "FRST_REGISTER_ID", length = 20, nullable = false)
	private String firstRegisterId;
    
	@Column(name = "LAST_UPDUSR_PNTTM", nullable = false)
	private Date lastUpdateUserPointInTime;
    
	@Column(name = "LAST_UPDUSR_ID", length = 20, nullable = false)
	private String lastUpdateUserId;
    
	@Column(name = "QESTN_CT", length = 40)
	private String questionCategory;
    
	@Column(name = "FAQ_NATION", length = 3, nullable = false)
	private String faqNation;

}
