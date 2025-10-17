package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    List<AssessmentEntity> findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCodeIn(UUID assessmentSessionEntitySessionID, Collection<String> assessmentTypeCodes);

    Optional<AssessmentEntity> findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(UUID sessionID, String assessmentTypeCode);
}
