package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.choreographer.ChoreographEventHandler;
import ca.bc.gov.educ.assessment.api.exception.BusinessException;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.ChoreographedEvent;
import io.nats.client.Message;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventStatus.MESSAGE_PUBLISHED;

/**
 * This class will process events from Jet Stream, which is used in choreography pattern, where messages are published if a student is created or updated.
 */
@Service
@Slf4j
public class JetStreamEventHandlerService {

  private final AssessmentEventRepository assessmentEventRepository;
  private final ChoreographedEventPersistenceService choreographedEventPersistenceService;
  private final ChoreographEventHandler choreographEventHandler;


  @Autowired
  public JetStreamEventHandlerService(AssessmentEventRepository assessmentEventRepository, ChoreographedEventPersistenceService choreographedEventPersistenceService, ChoreographEventHandler choreographEventHandler) {
      this.assessmentEventRepository = assessmentEventRepository;
      this.choreographedEventPersistenceService = choreographedEventPersistenceService;
      this.choreographEventHandler = choreographEventHandler;
  }

  public void handleEvent(@NonNull final ChoreographedEvent choreographedEvent, final Message message) throws IOException {
    try {
      final var persistedEvent = this.choreographedEventPersistenceService.persistEventToDB(choreographedEvent);
      message.ack(); // acknowledge to Jet Stream that api got the message and it is now in DB.
      log.info("acknowledged to Jet Stream...");
      this.choreographEventHandler.handleEvent(persistedEvent);
    } catch (final BusinessException businessException) {
      message.ack(); // acknowledge to Jet Stream that api got the message already...
      log.info("acknowledged to Jet Stream...");
    }
  }

  /**
   * Update event status.
   *
   * @param choreographedEvent the choreographed event
   */
  @Transactional
  public void updateEventStatus(ChoreographedEvent choreographedEvent) {
    if (choreographedEvent != null && choreographedEvent.getEventID() != null) {
      var eventID = choreographedEvent.getEventID();
      var eventOptional = assessmentEventRepository.findById(UUID.fromString(eventID));
      if (eventOptional.isPresent()) {
        var studentEvent = eventOptional.get();
        studentEvent.setEventStatus(MESSAGE_PUBLISHED.toString());
        assessmentEventRepository.save(studentEvent);
      }
    }
  }
}
