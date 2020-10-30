package ca.bc.gov.educ.api.studentassessment.model.entity;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

import lombok.Data;

@Data
@Immutable
@Entity
@Table(name = "TAB_GRAD_ASSMT")
public class AssessmentEntity {
   
	@Id
	@Column(name = "ASSMT_CODE", nullable = true)
    private String assessmentCode;   

    @Column(name = "ASSMT_NAME", nullable = true)
    private String assessmentName;   

    @Column(name = "LANGUAGE", nullable = true)
    private String language;
    
    @Column(name = "ASSMT_START_DATE", nullable = true)
    private Date startDate;

    @Column(name = "ASSMT_END_DATE", nullable = true)
    private Date endDate;
}
