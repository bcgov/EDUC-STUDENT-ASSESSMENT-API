package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentDOARCalculationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StagedAssessmentStudentDOARCalculationRepository extends JpaRepository<StagedAssessmentStudentDOARCalculationEntity, UUID> {

    Optional<StagedAssessmentStudentDOARCalculationEntity> findByAssessmentStudentID(UUID assessmentStudentID);

    List<StagedAssessmentStudentDOARCalculationEntity> findAllByAssessmentIDAndAssessmentStudentIDIn(UUID assessmentStudentID, List<UUID> assessmentStudentIDs);

    Optional<StagedAssessmentStudentDOARCalculationEntity> findByAssessmentStudentIDAndAssessmentID(UUID assessmentStudentID, UUID assessmentID);

}
