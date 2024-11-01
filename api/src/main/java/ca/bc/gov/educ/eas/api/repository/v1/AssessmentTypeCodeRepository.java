package ca.bc.gov.educ.eas.api.repository.v1;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentTypeCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentTypeCodeRepository extends JpaRepository<AssessmentTypeCodeEntity, String> {
}
