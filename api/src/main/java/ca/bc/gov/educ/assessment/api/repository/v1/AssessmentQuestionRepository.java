package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestionEntity, UUID> {

    Optional<AssessmentQuestionEntity> findByAssessmentComponentEntity_AssessmentComponentIDAndQuestionNumberAndItemNumber(UUID assessmentComponentID, Integer questionNumber, Integer itemNumber);

    int countAllByAssessmentComponentEntity_AssessmentComponentIDAndQuestionNumber(UUID assessmentComponentID, Integer questionNumber);

}
