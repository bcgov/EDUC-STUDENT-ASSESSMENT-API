package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentComponentRepository extends JpaRepository<AssessmentComponentEntity, UUID> {

    Optional<AssessmentComponentEntity> findByAssessmentFormEntity_AssessmentFormIDAndComponentTypeCodeAndComponentSubTypeCode(UUID assessmentFormID, String componentTypeCode, String componentSubTypeCode);

    List<AssessmentComponentEntity> findByAssessmentFormEntity_AssessmentFormID(UUID assessmentFormID);

}
