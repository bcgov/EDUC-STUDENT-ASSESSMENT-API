package ca.bc.gov.educ.assessment.api.repository.v1;


import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface AssessmentEventRepository extends JpaRepository<AssessmentEventEntity, UUID> {

  List<AssessmentEventEntity> findByEventStatusAndEventTypeIn(String eventStatus, List<String> eventTypes);

  List<AssessmentEventEntity> findByEventStatus(String eventStatus);

  Optional<AssessmentEventEntity> findByEventId(UUID eventId);

  @Query(value = "select event.* from ASSESSMENT_EVENT event where event.EVENT_STATUS = :eventStatus " +
          "AND event.CREATE_DATE < :createDate " +
          "AND event.EVENT_TYPE not in :eventTypes " +
          "ORDER BY event.CREATE_DATE asc " +
          "FETCH FIRST :limit ROWS ONLY", nativeQuery=true)
  List<AssessmentEventEntity> findAllByEventStatusAndCreateDateBeforeAndEventTypeNotInOrderByCreateDate(String eventStatus, LocalDateTime createDate, int limit, List<String> eventTypes);

}
