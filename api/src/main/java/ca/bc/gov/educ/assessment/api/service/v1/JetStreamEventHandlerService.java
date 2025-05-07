package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.ChoreographedEvent;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventStatus.MESSAGE_PUBLISHED;

/**
 * This class will process events from Jet Stream, which is used in choreography pattern, where messages are published if a student is created or updated.
 */
@Service
@Slf4j
public class JetStreamEventHandlerService {

  private final AssessmentEventRepository assessmentEventRepository;


  @Autowired
  public JetStreamEventHandlerService(AssessmentEventRepository assessmentEventRepository) {
      this.assessmentEventRepository = assessmentEventRepository;
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
