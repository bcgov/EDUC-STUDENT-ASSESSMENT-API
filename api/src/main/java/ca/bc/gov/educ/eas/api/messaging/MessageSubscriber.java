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
  public MessageSubscriber(final Connection con, EventHandlerDelegatorService eventHandlerDelegatorServiceV1) {
    this.connection = con;
    this.eventHandlerDelegatorServiceV1 = eventHandlerDelegatorServiceV1;
    messageProcessingThreads = new EnhancedQueueExecutor.Builder().setThreadFactory(new ThreadFactoryBuilder().setNameFormat("nats-message-subscriber-%d").build()).setCorePoolSize(10).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @PostConstruct
  public void subscribe() {
    String queue = EAS_API_TOPIC.toString().replace("_", "-");
    var dispatcher = connection.createDispatcher(onMessage());
    dispatcher.subscribe(EAS_API_TOPIC.toString(), queue);
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
}
