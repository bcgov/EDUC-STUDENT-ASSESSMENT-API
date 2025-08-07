package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentLightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StagedAssessmentStudentLightRepository extends JpaRepository<StagedAssessmentStudentLightEntity, UUID>, JpaSpecificationExecutor<StagedAssessmentStudentLightEntity> {

    List<StagedAssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStagedAssessmentStudentStatusIn(UUID sessionID, List<String> studentStatuses);
}
