package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.ProvincialSpecialCaseCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvincialSpecialCaseCodeRepository extends JpaRepository<ProvincialSpecialCaseCodeEntity, String> {
}
