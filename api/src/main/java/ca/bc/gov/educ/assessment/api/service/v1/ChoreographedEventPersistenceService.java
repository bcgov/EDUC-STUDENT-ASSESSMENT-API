package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.exception.BusinessError;
import ca.bc.gov.educ.assessment.api.exception.BusinessException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.ChoreographedEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventStatus.DB_COMMITTED;

@Service
@Slf4j
public class ChoreographedEventPersistenceService {
  private final AssessmentEventRepository assessmentEventRepository;

  @Autowired
  public ChoreographedEventPersistenceService(AssessmentEventRepository assessmentEventRepository) {
      this.assessmentEventRepository = assessmentEventRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AssessmentEventEntity persistEventToDB(final ChoreographedEvent choreographedEvent) throws BusinessException {
    final var eventOptional = this.assessmentEventRepository.findByEventId(UUID.fromString(choreographedEvent.getEventID()));
    if (eventOptional.isPresent()) {
      throw new BusinessException(BusinessError.EVENT_ALREADY_PERSISTED, choreographedEvent.getEventID().toString());
    }
    val event = AssessmentEventEntity.builder()
      .eventType(choreographedEvent.getEventType().toString())
      .eventId(UUID.fromString(choreographedEvent.getEventID()))
      .eventOutcome(choreographedEvent.getEventOutcome().toString())
      .eventPayload(choreographedEvent.getEventPayload())
      .eventStatus(DB_COMMITTED.toString())
      .createUser(StringUtils.isBlank(choreographedEvent.getCreateUser()) ? "EDUC-STUDENT-ASSESSMENT-API" : choreographedEvent.getCreateUser())
      .updateUser(StringUtils.isBlank(choreographedEvent.getUpdateUser()) ? "EDUC-STUDENT-ASSESSMENT-API" : choreographedEvent.getUpdateUser())
      .createDate(LocalDateTime.now())
      .updateDate(LocalDateTime.now())
      .build();
    return this.assessmentEventRepository.save(event);
  }
}
