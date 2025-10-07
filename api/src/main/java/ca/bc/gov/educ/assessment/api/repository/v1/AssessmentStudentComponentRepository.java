package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentStudentComponentRepository extends JpaRepository<AssessmentStudentComponentEntity, UUID>, JpaSpecificationExecutor<AssessmentStudentComponentEntity> {
    Optional<AssessmentStudentComponentEntity> findAllByAssessmentStudentComponentIDAndAssessmentComponentID(UUID assessmentStudentID, UUID assessmentComponentID);
}
