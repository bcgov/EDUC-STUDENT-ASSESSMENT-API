package ca.bc.gov.educ.assessment.api.repository.v1;


import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface AssessmentEventRepository extends JpaRepository<AssessmentEventEntity, UUID> {

  Optional<AssessmentEventEntity> findBySagaIdAndEventType(UUID sagaId, String eventType);

  List<AssessmentEventEntity> findByEventStatus(String eventStatus);
}
