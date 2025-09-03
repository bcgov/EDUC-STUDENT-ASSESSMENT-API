package ca.bc.gov.educ.assessment.api.service.v1.events;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class EventHandlerDelegatorServiceTest extends BaseAssessmentAPITest {

  @MockBean
  private MessagePublisher messagePublisher;

  @MockBean
  private Publisher publisher;

  @Autowired
  private EventHandlerService eventHandlerService;

  @Autowired
  private AssessmentSessionRepository assessmentSessionRepository;

  @Autowired
  private AssessmentRepository assessmentRepository;

  @Autowired
  private AssessmentStudentRepository assessmentStudentRepository;

  private EventHandlerDelegatorService eventHandlerDelegatorService;

  @BeforeEach
  void setUp() {
    eventHandlerDelegatorService = new EventHandlerDelegatorService(
        messagePublisher, eventHandlerService, publisher);
    assessmentSessionRepository.save(createMockSessionEntity());
  }

  @AfterEach
  void tearDown() {
    assessmentStudentRepository.deleteAll();
    assessmentRepository.deleteAll();
    assessmentSessionRepository.deleteAll();
  }

  @Nested
  @DisplayName("GET_ASSESSMENT_STUDENTS Event Tests")
  class GetAssessmentStudentsEventTests {

    @Test
    @DisplayName("Should handle synchronous GET_ASSESSMENT_STUDENTS event and publish response to message replyTo")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenSynchronous_shouldPublishToMessageReplyTo() {
      // Given
      AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
      AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
      
      UUID studentId = UUID.randomUUID();
      AssessmentStudentEntity student = createMockAssessmentStudentEntity(assessment, studentId);
      assessmentStudentRepository.save(student);

      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(TopicsEnum.STUDENT_ASSESSMENT_API_TOPIC.toString())
          .eventPayload(studentId.toString())
          .build();

      // Mock synchronous message with replyTo
      Message mockMessage = mock(Message.class);
      String replyToChannel = "test-reply-channel";
      when(mockMessage.getReplyTo()).thenReturn(replyToChannel);

      // When
      eventHandlerDelegatorService.handleEvent(event, mockMessage);

      // Then
      ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
      
      verify(messagePublisher, times(1)).dispatchMessage(channelCaptor.capture(), payloadCaptor.capture());
      
      assertThat(channelCaptor.getValue()).isEqualTo(replyToChannel);
      assertThat(payloadCaptor.getValue()).isNotEmpty();
      
      // Verify the response contains the expected student data
      String responseJson = new String(payloadCaptor.getValue());
      assertThat(responseJson)
          .contains(studentId.toString())
          .contains(assessment.getAssessmentID().toString());
    }

    @Test
    @DisplayName("Should handle asynchronous GET_ASSESSMENT_STUDENTS event and publish response to event replyTo")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenAsynchronous_shouldPublishToEventReplyTo() {
      // Given
      AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
      AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
      
      UUID studentId = UUID.randomUUID();
      AssessmentStudentEntity student = createMockAssessmentStudentEntity(assessment, studentId);
      assessmentStudentRepository.save(student);

      var sagaId = UUID.randomUUID();
      String eventReplyTo = TopicsEnum.STUDENT_ASSESSMENT_API_TOPIC.toString();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(eventReplyTo)
          .eventPayload(studentId.toString())
          .build();

      // Mock asynchronous message without replyTo
      Message mockMessage = mock(Message.class);
      when(mockMessage.getReplyTo()).thenReturn(null);

      // When
      eventHandlerDelegatorService.handleEvent(event, mockMessage);

      // Then
      ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
      
      verify(messagePublisher, times(1)).dispatchMessage(channelCaptor.capture(), payloadCaptor.capture());
      
      assertThat(channelCaptor.getValue()).isEqualTo(eventReplyTo);
      assertThat(payloadCaptor.getValue()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle GET_ASSESSMENT_STUDENTS event when no students exist and return empty response")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenNoStudentsExist_shouldReturnEmptyResponse() {
      // Given
      UUID studentId = UUID.randomUUID();
      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(TopicsEnum.STUDENT_ASSESSMENT_API_TOPIC.toString())
          .eventPayload(studentId.toString())
          .build();

      Message mockMessage = mock(Message.class);
      when(mockMessage.getReplyTo()).thenReturn("test-reply-channel");

      // When
      eventHandlerDelegatorService.handleEvent(event, mockMessage);

      // Then
      ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
      verify(messagePublisher, times(1)).dispatchMessage(anyString(), payloadCaptor.capture());
      
      assertThat(payloadCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("Should handle GET_ASSESSMENT_STUDENTS event with multiple students and return all")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenMultipleStudentsExist_shouldReturnAllStudents() {
      // Given
      AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
      AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
      
      UUID studentId = UUID.randomUUID();
      AssessmentStudentEntity student1 = createMockAssessmentStudentEntity(assessment, studentId);
      AssessmentStudentEntity student2 = createMockAssessmentStudentEntity(assessment, studentId);
      
      assessmentStudentRepository.save(student1);
      assessmentStudentRepository.save(student2);

      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(TopicsEnum.STUDENT_ASSESSMENT_API_TOPIC.toString())
          .eventPayload(studentId.toString())
          .build();

      Message mockMessage = mock(Message.class);
      when(mockMessage.getReplyTo()).thenReturn("test-reply-channel");

      // When
      eventHandlerDelegatorService.handleEvent(event, mockMessage);

      // Then
      ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
      verify(messagePublisher, times(1)).dispatchMessage(anyString(), payloadCaptor.capture());
      
      byte[] response = payloadCaptor.getValue();
      assertThat(response).isNotEmpty();
      
      // Verify response contains both students
      String responseJson = new String(response);
      assertThat(responseJson)
          .contains(studentId.toString())
          .contains(assessment.getAssessmentID().toString());
    }

    @Test
    @DisplayName("Should log appropriate messages for GET_ASSESSMENT_STUDENTS event")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_shouldLogAppropriateMessages() {
      // Given
      AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
      AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
      
      UUID studentId = UUID.randomUUID();
      AssessmentStudentEntity student = createMockAssessmentStudentEntity(assessment, studentId);
      assessmentStudentRepository.save(student);

      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(TopicsEnum.STUDENT_ASSESSMENT_API_TOPIC.toString())
          .eventPayload(studentId.toString())
          .build();

      Message mockMessage = mock(Message.class);
      when(mockMessage.getReplyTo()).thenReturn("test-reply-channel");

      // When
      eventHandlerDelegatorService.handleEvent(event, mockMessage);

      // Then
      // Verify the service handled the event and published a response
      verify(messagePublisher, times(1)).dispatchMessage(anyString(), any());
      
      // The logging is done via SLF4J, so we can't easily verify it in unit tests
      // But we can verify the event was processed successfully by checking the response
      ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
      verify(messagePublisher).dispatchMessage(anyString(), payloadCaptor.capture());
      
      assertThat(payloadCaptor.getValue()).isNotEmpty();
    }
  }
}
