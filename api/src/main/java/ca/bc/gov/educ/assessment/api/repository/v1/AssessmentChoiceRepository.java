package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentChoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentChoiceRepository extends JpaRepository<AssessmentChoiceEntity, UUID> {

    Optional<AssessmentChoiceEntity> findByItemNumberAndAssessmentComponentEntity_AssessmentComponentID(int itemNumber, UUID assessmentComponentID);

}
