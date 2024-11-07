package ca.bc.gov.educ.eas.api.repository.v1;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentStudentHistoryRepository extends JpaRepository<AssessmentStudentHistoryEntity, UUID> {
    List<AssessmentStudentHistoryEntity> findAllByAssessmentIDAndAssessmentStudentID(UUID asessmentID, UUID assessmentStudentID);

    List<AssessmentStudentHistoryEntity> findAllByAssessmentStudentID(UUID assessmentStudentID);

}
