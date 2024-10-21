package ca.bc.gov.educ.eas.api.service.v1;


import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.EventOutcome;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.TopicsEnum;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.service.v1.events.EventHandlerService;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class EventHandlerServiceTest extends BaseEasAPITest {

  public static final String EAS_API_TOPIC = TopicsEnum.EAS_API_TOPIC.toString();
  @Autowired
  private SessionRepository sessionRepository;

  @Autowired
  private AssessmentRepository assessmentRepository;

  @Autowired
  private EventHandlerService eventHandlerServiceUnderTest;
  private final boolean isSynchronous = false;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    sessionRepository.save(createMockSessionEntity());
  }

  @AfterEach
  public void tearDown() {
    assessmentRepository.deleteAll();
    sessionRepository.deleteAll();
  }

  @Test
  void testHandleEvent_givenEventTypeGET_STUDENT__whenNoStudentExist_shouldHaveEventOutcomeSTUDENT_NOT_FOUND() throws IOException {
    var sagaId = UUID.randomUUID();
    final Event event = Event.builder().eventType(EventType.GET_OPEN_ASSESSMENT_SESSIONS).sagaId(sagaId).replyTo(EAS_API_TOPIC).eventPayload(UUID.randomUUID().toString()).build();
    byte[] response = eventHandlerServiceUnderTest.handleGetOpenAssessmentSessionsEvent(event, isSynchronous);
    assertThat(response).isNotEmpty();
    Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response);
    assertThat(responseEvent).isNotNull();
    assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.SESSIONS_FOUND);
  }

}
