package ca.bc.gov.educ.api.studentassessment.model.dto;

import lombok.Data;

@Data
public class StudentAssessmentId {

    private String pen;    
    private String assessmentCode;    
    private String sessionDate;

    public StudentAssessmentId() {
    }

    /**
     * Constructor method used by JPA to create a composite primary key.
     *
     * @param studNo
     * @param crseCode
     * @param crseLevel
     * @param crseSession
     */
    public StudentAssessmentId(String studNo, String assessmentCode, String assmSession) {
        this.pen = studNo;
        this.assessmentCode = assessmentCode;
        this.sessionDate = assmSession;
    }
}
