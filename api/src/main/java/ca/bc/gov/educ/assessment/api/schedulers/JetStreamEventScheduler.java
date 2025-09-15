package ca.bc.gov.educ.assessment.api.schedulers;

import ca.bc.gov.educ.assessment.api.choreographer.ChoreographEventHandler;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

import static ca.bc.gov.educ.assessment.api.constants.EventStatus.DB_COMMITTED;

/**
 * This class is responsible to check the STUDENT_EVENT table periodically and publish messages to JET STREAM, if some them are not yet published
 * this is a very edge case scenario which will occur.
 */
@Component
@Slf4j
public class JetStreamEventScheduler {

  private final AssessmentEventRepository assessmentEventRepository;
  private final Publisher publisher;
  private final ChoreographEventHandler choreographEventHandler;


  public JetStreamEventScheduler(AssessmentEventRepository assessmentEventRepository, Publisher publisher, ChoreographEventHandler choreographEventHandler) {
      this.assessmentEventRepository = assessmentEventRepository;
      this.publisher = publisher;
      this.choreographEventHandler = choreographEventHandler;
  }

  /**
   * Find and publish student events to stan.
   */
  @Scheduled(cron = "0 0/5 * * * *") // every 5 minutes
  @SchedulerLock(name = "PUBLISH_ASSESSMENT_EVENTS_TO_JET_STREAM", lockAtLeastFor = "PT4M", lockAtMostFor = "PT4M")
  public void findAndPublishStudentEventsToJetStream() {
    LockAssert.assertLocked();
    log.debug("Firing scheduler for jet stream events");
    var incomingEvents = Arrays.asList(EventType.UPDATE_SCHOOL_OF_RECORD.toString(), EventType.CREATE_MERGE.toString(), EventType.DELETE_MERGE.toString());
    var results = assessmentEventRepository.findByEventStatusAndEventTypeIn(DB_COMMITTED.toString(), incomingEvents);

    if (!results.isEmpty()) {
      log.info("Found {} choreographed events which needs to be processed.", results.size());
        results.forEach(result -> {
            try {
                this.choreographEventHandler.handleEvent(result);
            } catch (Exception e) {
              log.error("Exception while processing choreographed event", e);
            }
        });
      }

    final var outgoingEvents = this.assessmentEventRepository.findAllByEventStatusAndCreateDateBeforeAndEventTypeNotInOrderByCreateDate(DB_COMMITTED.toString(), LocalDateTime.now().minusMinutes(1), 500, incomingEvents);
    log.debug("Found {} jet stream events to process", outgoingEvents.size());
    if (!outgoingEvents.isEmpty()) {
      outgoingEvents.forEach(el -> {
        if (el.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(1))) {
          try {
            publisher.dispatchChoreographyEvent(el);
          } catch (final Exception ex) {
            log.error("Exception while trying to publish message", ex);
          }
        }
      });
    }

  }
}
