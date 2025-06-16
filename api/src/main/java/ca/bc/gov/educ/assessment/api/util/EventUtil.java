package ca.bc.gov.educ.assessment.api.util;


import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventStatus;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;

import java.time.LocalDateTime;


public class EventUtil {
  private EventUtil() {
  }

  public static AssessmentEventEntity createEvent(String createUser, String updateUser, String jsonString, EventType eventType, EventOutcome eventOutcome) {
    return AssessmentEventEntity.builder()
      .createDate(LocalDateTime.now())
      .updateDate(LocalDateTime.now())
      .createUser(createUser)
      .updateUser(updateUser)
      .eventPayload(jsonString)
      .eventType(eventType.toString())
      .eventStatus(EventStatus.DB_COMMITTED.toString())
      .eventOutcome(eventOutcome.toString())
      .build();
  }
}
