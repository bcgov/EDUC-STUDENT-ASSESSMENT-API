package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistoryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AssessmentStudentHistoryService {

    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentStudentHistoryEntity createAssessmentStudentHistoryEntity(AssessmentStudentEntity assessmentStudentEntity, String updateUser) {
        final AssessmentStudentHistoryEntity assessmentStudentHistoryEntity = new AssessmentStudentHistoryEntity();
        BeanUtils.copyProperties(assessmentStudentEntity, assessmentStudentHistoryEntity);
        assessmentStudentHistoryEntity.setAssessmentStudentID(assessmentStudentEntity.getAssessmentStudentID());
        assessmentStudentHistoryEntity.setAssessmentID(assessmentStudentEntity.getAssessmentEntity().getAssessmentID());
        assessmentStudentHistoryEntity.setCreateUser(updateUser);
        assessmentStudentHistoryEntity.setCreateDate(LocalDateTime.now());
        assessmentStudentHistoryEntity.setUpdateUser(updateUser);
        assessmentStudentHistoryEntity.setUpdateDate(LocalDateTime.now());
        return assessmentStudentHistoryEntity;
    }
}
