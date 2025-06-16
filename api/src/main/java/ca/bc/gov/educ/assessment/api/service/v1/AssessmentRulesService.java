package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
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

    public boolean hasStudentAssessmentDuplicate(String studentPEN, UUID assessmentID, UUID assessmentStudentID){
        return assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndPen(assessmentID, studentPEN)
                .filter(existing -> assessmentStudentID == null || !existing.getAssessmentStudentID().equals(assessmentStudentID))
                .isPresent();
    }

    public boolean studentAssessmentWritesExceeded(String pen, List<String> assessmentTypeCodes){
        int numberOfAttemptsForStudentPEN = assessmentStudentRepository.findNumberOfAttemptsForStudentPEN(pen, assessmentTypeCodes);

        return numberOfAttemptsForStudentPEN >= studentAssessmentWriteMax;
    }

    public boolean studentHasWrittenAssessment(UUID assessmentStudentID){
        Optional<AssessmentStudentEntity> existingStudent = assessmentStudentRepository.findById(assessmentStudentID);

        return existingStudent.isPresent() && (existingStudent.get().getProvincialSpecialCaseCode() != null || existingStudent.get().getProficiencyScore() != null);
    }
}
