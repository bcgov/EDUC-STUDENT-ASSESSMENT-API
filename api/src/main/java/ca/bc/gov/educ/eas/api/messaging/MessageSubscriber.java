package ca.bc.gov.educ.eas.api.messaging;

import ca.bc.gov.educ.eas.api.helpers.LogHelper;
import ca.bc.gov.educ.eas.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.eas.api.service.v1.events.EventHandlerDelegatorService;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static ca.bc.gov.educ.eas.api.constants.TopicsEnum.EAS_API_TOPIC;
import static lombok.AccessLevel.PRIVATE;

@Component
@Slf4j
public class MessageSubscriber {

    /**
     * The Handlers.
     */
    @Getter(PRIVATE)
    private final Map<String, EventHandler> handlerMap = new HashMap<>();
    private final Connection connection;
    private final Executor messageProcessingThreads;
    private final EventHandlerDelegatorService eventHandlerDelegatorServiceV1;

    @Autowired
    public MessageSubscriber(final Connection con, EventHandlerDelegatorService eventHandlerDelegatorServiceV1, final List<EventHandler> eventHandlers) {
        this.connection = con;
        this.eventHandlerDelegatorServiceV1 = eventHandlerDelegatorServiceV1;
        messageProcessingThreads = new EnhancedQueueExecutor.Builder().setThreadFactory(new ThreadFactoryBuilder().setNameFormat("nats-message-subscriber-%d").build()).setCorePoolSize(10).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
        eventHandlers.forEach(handler -> {
            this.handlerMap.put(handler.getTopicToSubscribe(), handler);
            this.subscribeForSAGA(handler.getTopicToSubscribe(), handler);
        });
    }

    @PostConstruct
    public void subscribe() {
        String queue = EAS_API_TOPIC.toString().replace("_", "-");
        var dispatcher = connection.createDispatcher(onMessage());
        dispatcher.subscribe(EAS_API_TOPIC.toString(), queue);
    }

    /**
     * Subscribe the topic on messages for SAGA
     *
     * @param topic        the topic name
     * @param eventHandler the orchestrator
     */
    private void subscribeForSAGA(final String topic, final EventHandler eventHandler) {
        this.handlerMap.computeIfAbsent(topic, k -> eventHandler);
        final String queue = topic.replace("_", "-");
        final var dispatcher = this.connection.createDispatcher(MessageSubscriber.onMessageForSAGA(eventHandler));
        dispatcher.subscribe(topic, queue);
    }

    private MessageHandler onMessage() {
        return (Message message) -> {
            if (message != null) {
                try {
                    var eventString = new String(message.getData());
                    LogHelper.logMessagingEventDetails(eventString);
                    var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
                    messageProcessingThreads.execute(() -> eventHandlerDelegatorServiceV1.handleEvent(event, message));
                } catch (final Exception e) {
                    log.error("Exception ", e);
                }
            }
        };
    }

    /**
     * On message, event handler for SAGA
     *
     * @param eventHandler the orchestrator
     * @return the message handler
     */
    private static MessageHandler onMessageForSAGA(final EventHandler eventHandler) {
        return (Message message) -> {
            if (message != null) {
                log.info("Message received subject :: {},  replyTo :: {}, subscriptionID :: {}", message.getSubject(), message.getReplyTo(), message.getSID());
                try {
                    final var eventString = new String(message.getData());
                    final var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
                    eventHandler.handleEvent(event);
                } catch (final InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.error("InterruptedException while findAndProcessPendingSagaEvents :: {}", ex);
                } catch (final Exception e) {
                    log.error("Exception ", e);
                }
            }
        };
    }
}
