package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionCriteriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentSessionCriteriaRepository extends JpaRepository<AssessmentSessionCriteriaEntity, UUID> {

    List<AssessmentSessionCriteriaEntity> findAllByEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(LocalDateTime effectiveDate, LocalDateTime expiryDate);
}
