package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AdaptedAssessmentIndicatorCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdaptedAssessmentIndicatorCodeRepository extends JpaRepository<AdaptedAssessmentIndicatorCodeEntity, String> {

}
