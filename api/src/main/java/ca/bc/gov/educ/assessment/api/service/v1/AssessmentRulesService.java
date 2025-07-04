package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.util.AssessmentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentRulesService {

    private final AssessmentStudentRepository assessmentStudentRepository;

    private static final int studentAssessmentWriteMax = 3;

    public boolean hasStudentAssessmentDuplicate(UUID studentID, UUID assessmentID, UUID assessmentStudentID){
        return assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentIDAndAssessmentStudentIDIsNot(assessmentID, studentID, assessmentStudentID)
                .isPresent();
    }

    public boolean studentAssessmentWritesExceeded(UUID studentID, String assessmentTypeCode){
        int numberOfAttemptsForStudentPEN = assessmentStudentRepository.findNumberOfAttemptsForStudent(studentID, AssessmentUtil.getAssessmentTypeCodeList(assessmentTypeCode));
        return numberOfAttemptsForStudentPEN >= studentAssessmentWriteMax;
    }
}
