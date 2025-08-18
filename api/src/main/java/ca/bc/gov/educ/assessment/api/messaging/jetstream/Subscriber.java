package ca.bc.gov.educ.assessment.api.messaging.jetstream;


import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.helpers.LogHelper;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.service.v1.JetStreamEventHandlerService;
import ca.bc.gov.educ.assessment.api.struct.v1.ChoreographedEvent;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.STUDENT_ASSESSMENT_EVENTS_TOPIC;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.PEN_SERVICES_EVENTS_TOPIC;
import static ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher.STREAM_NAME;

/**
 * The type Subscriber.
 */
@Component
@DependsOn("publisher")
@Slf4j
public class Subscriber {
    private final Connection natsConnection;
    private final Executor subscriberExecutor = new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("jet-stream-subscriber-%d").build())
            .setCorePoolSize(2).setMaximumPoolSize(2).setKeepAliveTime(Duration.ofMillis(1000)).build();
    private final Map<String, List<String>> streamTopicsMap = new HashMap<>();
    private final JetStreamEventHandlerService jetStreamEventHandlerService;// one stream can have multiple topics.

    @Autowired
    public Subscriber(final Connection natsConnection, JetStreamEventHandlerService jetStreamEventHandlerService) {
        this.natsConnection = natsConnection;
        this.jetStreamEventHandlerService = jetStreamEventHandlerService;
        this.initializeStreamTopicMap();
    }

    /**
     * this is the source of truth for all the topics this api subscribes to.
     */
    private void initializeStreamTopicMap() {
        final List<String> studentAssessmentEventsTopics = new ArrayList<>();
        studentAssessmentEventsTopics.add(STUDENT_ASSESSMENT_EVENTS_TOPIC.toString());
        studentAssessmentEventsTopics.add(PEN_SERVICES_EVENTS_TOPIC.toString());
        this.streamTopicsMap.put(STREAM_NAME, studentAssessmentEventsTopics);
    }

    @PostConstruct
    public void subscribe() throws IOException, JetStreamApiException {
        val qName = ApplicationProperties.STUDENT_ASSESSMENT_API.concat("-QUEUE");
        val autoAck = false;
        for (val entry : this.streamTopicsMap.entrySet()) {
            for (val topic : entry.getValue()) {
                final PushSubscribeOptions options = PushSubscribeOptions.builder().stream(entry.getKey())
                        .durable(ApplicationProperties.STUDENT_ASSESSMENT_API.concat("-DURABLE"))
                        .configuration(ConsumerConfiguration.builder().deliverPolicy(DeliverPolicy.New).build()).build();
                this.natsConnection.jetStream().subscribe(topic, qName, this.natsConnection.createDispatcher(), this::onMessage,
                        autoAck, options);
            }
        }
    }

    public void onMessage(final Message message) {
        if (message != null) {
            log.info("Received message Subject:: {} , SID :: {} , sequence :: {}, pending :: {} ", message.getSubject(), message.getSID(), message.metaData().consumerSequence(), message.metaData().pendingCount());
            try {
                val eventString = new String(message.getData());
                LogHelper.logMessagingEventDetails(eventString);
                final ChoreographedEvent event = JsonUtil.getJsonObjectFromString(ChoreographedEvent.class, eventString);
                if (event.getEventPayload() == null) {
                    message.ack();
                    log.warn("payload is null, ignoring event :: {}", event);
                    return;
                }
                this.subscriberExecutor.execute(() -> {
                    try {
                        if (event.getEventType().equals(EventType.UPDATE_SCHOOL_OF_RECORD)
                           || event.getEventType().equals(EventType.CREATE_MERGE)
                           || event.getEventType().equals(EventType.DELETE_MERGE)) {
                            this.jetStreamEventHandlerService.handleEvent(event, message);
                        } else {
                            jetStreamEventHandlerService.updateEventStatus(event);
                            log.info("Received event :: {} ", event);
                            message.ack();
                        }
                    } catch (final IOException e) {
                        log.error("IOException ", e);
                    }
                });
                log.info("received event :: {} ", event);
            } catch (final Exception ex) {
                log.error("Exception ", ex);
            }
        }
    }

}
