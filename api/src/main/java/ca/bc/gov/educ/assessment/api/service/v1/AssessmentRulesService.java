package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.util.AssessmentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentRulesService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;

    private static final int studentAssessmentWriteMax = 3;

    public boolean hasStudentAssessmentDuplicate(UUID studentID, AssessmentEntity assessment, UUID assessmentStudentID){
        var assessmentList = List.of(assessment);
        var assessmentTypeCodeList = AssessmentUtil.getAssessmentTypeCodeList(assessment.getAssessmentTypeCode());
        
        if(assessmentTypeCodeList.size() > 1){
            assessmentList = assessmentRepository.findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCodeIn(assessment.getAssessmentSessionEntity().getSessionID(), assessmentTypeCodeList);
        }
        return !assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentIDAndAssessmentStudentIDIsNot(assessmentList.stream().map(AssessmentEntity::getAssessmentID).toList(), studentID, assessmentStudentID)
                .isEmpty();
    }

    public boolean studentAssessmentWritesExceeded(UUID studentID, String assessmentTypeCode){
        int numberOfAttemptsForStudentPEN = assessmentStudentRepository.findNumberOfAttemptsForStudent(studentID, AssessmentUtil.getAssessmentTypeCodeList(assessmentTypeCode));
        log.debug("Number of attempts is {} for student with ID: {}", numberOfAttemptsForStudentPEN, studentID);
        return numberOfAttemptsForStudentPEN >= studentAssessmentWriteMax;
    }
}
