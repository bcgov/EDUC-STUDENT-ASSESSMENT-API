package ca.bc.gov.educ.eas.api.service.v1.events;

import ca.bc.gov.educ.eas.api.constants.TopicsEnum;
import ca.bc.gov.educ.eas.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.eas.api.struct.Event;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static ca.bc.gov.educ.eas.api.service.v1.events.EventHandlerService.PAYLOAD_LOG;
import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class RegistrationEventHandlerService implements EventHandler {

    @Getter
    private final EventHandlerService eventHandlerService;

    @Getter(PRIVATE)
    private final EventPublisherService eventPublisherService;

    @Autowired
    public RegistrationEventHandlerService(final EventHandlerService eventHandlerService, final EventPublisherService eventPublisherService) {
        this.eventHandlerService = eventHandlerService;
        this.eventPublisherService = eventPublisherService;
    }

    @Async("subscriberExecutor")
    @Override
    public void handleEvent(final Event event) {
        try {
            switch (event.getEventType()) {
                case PUBLISH_STUDENT_REGISTRATION_EVENT:
                    log.info("Received PUBLISH_STUDENT_REGISTRATION_EVENT event :: {}", event.getSagaId());
                    log.trace(PAYLOAD_LOG, event.getEventPayload());
                    this.getEventHandlerService().handlePublishStudentRegistrationEvent(event);
                    break;
                default:
                    log.info("silently ignoring other events :: {}", event);
                    break;
            }
        } catch (final Exception e) {
            log.error("Exception", e);
        }
    }

    @Override
    public String getTopicToSubscribe() {
        return TopicsEnum.PUBLISH_STUDENT_REGISTRATION_TOPIC.toString();
    }
}
