package ca.bc.gov.educ.assessment.api.support;

import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.assessment.api.messaging.NatsConnection;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import io.nats.client.Connection;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * The type Mock configuration.
 */
@Profile("test")
@Configuration
public class MockConfiguration {

  @Bean
  @Primary
  public MessagePublisher messagePublisher() {
    return Mockito.mock(MessagePublisher.class);
  }

  @Bean
  @Primary
  public MessageSubscriber messageSubscriber() {
    return Mockito.mock(MessageSubscriber.class);
  }

  @Bean
  @Primary
  public RestTemplate restTemplate() {
    return Mockito.mock(RestTemplate.class);
  }

  @Bean
  @Primary
  public RestUtils restUtils() {
    return Mockito.mock(RestUtils.class);
  }

  @Bean
  @Primary
  public Connection connection() {
    return Mockito.mock(Connection.class);
  }

  @Bean
  @Primary
  public NatsConnection natsConnection() {
    return Mockito.mock(NatsConnection.class);
  }

  @Bean
  @Primary
  public Publisher publisher() {
    return Mockito.mock(Publisher.class);
  }

  @Bean
  @Primary
  public Subscriber subscriber() {
    return Mockito.mock(Subscriber.class);
  }
}
