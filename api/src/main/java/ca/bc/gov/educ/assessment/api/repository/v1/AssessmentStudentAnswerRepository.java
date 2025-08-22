package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentStudentAnswerRepository extends JpaRepository<AssessmentStudentAnswerEntity, UUID>, JpaSpecificationExecutor<AssessmentStudentAnswerEntity> {
    List<AssessmentStudentAnswerEntity> findAllByAssessmentStudentComponentEntity_AssessmentStudentEntity_AssessmentStudentID(UUID assessmentStudentID);
}
