package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentStudentHistoryRepository extends JpaRepository<AssessmentStudentHistoryEntity, UUID> {
    List<AssessmentStudentHistoryEntity> findAllByAssessmentIDAndAssessmentStudentID(UUID asessmentID, UUID assessmentStudentID);

}
