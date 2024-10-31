package ca.bc.gov.educ.eas.api.service.v1.events;


import ca.bc.gov.educ.eas.api.messaging.MessagePublisher;
import ca.bc.gov.educ.eas.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.eas.api.model.v1.EasEventEntity;
import ca.bc.gov.educ.eas.api.struct.Event;
import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static ca.bc.gov.educ.eas.api.service.v1.events.EventHandlerService.PAYLOAD_LOG;


/**
 * The type Event handler service.
 */
@Service
@Slf4j
@SuppressWarnings({"java:S3864", "java:S3776"})
public class EventHandlerDelegatorService {

  /**
   * The constant RESPONDING_BACK_TO_NATS_ON_CHANNEL.
   */
  public static final String RESPONDING_BACK_TO_NATS_ON_CHANNEL = "responding back to NATS on {} channel ";
  private final MessagePublisher messagePublisher;
  private final EventHandlerService eventHandlerService;
  private final Publisher publisher;

  /**
   * Instantiates a new Event handler delegator service.
   *
   * @param messagePublisher    the message publisher
   * @param eventHandlerService the event handler service
   * @param publisher           the publisher
   */
  @Autowired
  public EventHandlerDelegatorService(MessagePublisher messagePublisher, EventHandlerService eventHandlerService, Publisher publisher) {
    this.messagePublisher = messagePublisher;
    this.eventHandlerService = eventHandlerService;
    this.publisher = publisher;
  }

  /**
   * Handle event.
   *
   * @param event   the event
   * @param message the message
   */
  public void handleEvent(final Event event, final Message message) {
    byte[] response;
    boolean isSynchronous = message.getReplyTo() != null;
    try {
      switch (event.getEventType()) {
        case GET_OPEN_ASSESSMENT_SESSIONS:
          log.info("Received GET_OPEN_ASSESSMENT_SESSIONS event :: {}", event.getSagaId());
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          response = eventHandlerService.handleGetOpenAssessmentSessionsEvent(event, isSynchronous);
          log.info(RESPONDING_BACK_TO_NATS_ON_CHANNEL, message.getReplyTo() != null ? message.getReplyTo() : event.getReplyTo());
          publishToNATS(event, message, isSynchronous, response);
          break;
        case GET_STUDENT_ASSESSMENT_DETAILS:
          log.info("Received GET_STUDENT_ASSESSMENT_DETAILS event :: {}", event.getSagaId());
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          response = eventHandlerService.handleGetStudentAssessmentDetailEvent(event);
          log.info(RESPONDING_BACK_TO_NATS_ON_CHANNEL, message.getReplyTo() != null ? message.getReplyTo() : event.getReplyTo());
          publishToNATS(event, message, isSynchronous, response);
          break;
        case CREATE_STUDENT_REGISTRATION:
          log.info("Received CREATE_STUDENT_REGISTRATION event :: {}", event.getSagaId());
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          response = eventHandlerService.handleCreateStudentRegistrationEvent(event);
          log.info(RESPONDING_BACK_TO_NATS_ON_CHANNEL, message.getReplyTo() != null ? message.getReplyTo() : event.getReplyTo());
          publishToNATS(event, message, isSynchronous, response);
          break;
        default:
          log.info("silently ignoring other events :: {}", event);
          break;
      }
    } catch (final Exception e) {
      log.error("Exception", e);
    }
  }

  private void publishToNATS(Event event, Message message, boolean isSynchronous, byte[] left) {
    if (isSynchronous) { // sync, req/reply pattern of nats
      messagePublisher.dispatchMessage(message.getReplyTo(), left);
    } else { // async, pub/sub
      messagePublisher.dispatchMessage(event.getReplyTo(), left);
    }
  }

  private void publishToJetStream(final EasEventEntity event) {
    publisher.dispatchChoreographyEvent(event);
  }
}
