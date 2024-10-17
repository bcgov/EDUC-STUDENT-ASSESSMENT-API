package ca.bc.gov.educ.eas.api.repository.v1;


import ca.bc.gov.educ.eas.api.model.v1.EasEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface EasEventRepository extends JpaRepository<EasEventEntity, UUID> {

  Optional<EasEventEntity> findBySagaIdAndEventType(UUID sagaId, String eventType);

}
