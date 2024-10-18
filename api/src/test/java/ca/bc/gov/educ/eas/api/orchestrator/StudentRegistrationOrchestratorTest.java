package ca.bc.gov.educ.eas.api.orchestrator;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.EventOutcome;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.SagaEnum;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.messaging.MessagePublisher;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.*;
import ca.bc.gov.educ.eas.api.service.v1.SagaService;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.eas.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.eas.api.constants.EventType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
class StudentRegistrationOrchestratorTest extends BaseEasAPITest {

    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    AssessmentStudentRepository assessmentStudentRepository;
    @Autowired
    SagaRepository sagaRepository;
    @Autowired
    SagaEventRepository sagaEventRepository;
    @Autowired
    MessagePublisher messagePublisher;
    @Autowired
    StudentRegistrationOrchestrator studentRegistrationOrchestrator;
    @Autowired
    SagaService sagaService;
    AssessmentStudent sagaData;
    String sagaPayload;
    @Captor
    ArgumentCaptor<byte[]> eventCaptor;
    private EasSagaEntity saga;

    @AfterEach
    public void after() {
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
        assessmentStudentRepository.deleteAll();
        assessmentRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(this.messagePublisher);
        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
        sagaData = createMockStudent();
        sagaData.setAssessmentID(assessment.getAssessmentID().toString());
        MockitoAnnotations.openMocks(this);
        sagaPayload = JsonUtil.getJsonString(sagaData).get();
        saga = this.sagaService.createSagaRecordInDB(SagaEnum.CREATE_STUDENT_REGISTRATION.name(), "test", sagaPayload);
    }

    @SneakyThrows
    @Test
    void testHandleEvent_createAssessmentStudent_CreateStudentAndPostMessageToNats() {
        final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
        final var event = Event.builder()
                .eventType(INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(this.saga.getSagaId())
                .eventPayload(sagaPayload)
                .build();
        this.studentRegistrationOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(invocations + 2)).dispatchMessage(eq(this.studentRegistrationOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT_REGISTRATION);
        assertThat(newEvent.getEventOutcome()).isEqualTo(STUDENT_REGISTRATION_CREATED);

        final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(CREATE_STUDENT_REGISTRATION.toString());
        var payload = JsonUtil.getJsonObjectFromString(AssessmentStudent.class, newEvent.getEventPayload());
        assertThat(payload.getStudentID()).isNotBlank();
        assertThat(payload.getAssessmentID()).isNotBlank();
        Optional<AssessmentStudentEntity> assessmentStudent = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(UUID.fromString(payload.getAssessmentID()), UUID.fromString(payload.getStudentID()));
        assertThat(assessmentStudent).isPresent();
        assertThat(assessmentStudent.get().getAssessmentStudentID()).isNotNull();
        assertEquals(assessmentStudent.get().getStudentID().toString(), payload.getStudentID());
        final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
        assertThat(sagaStates).hasSize(1);
        assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
        assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
    }

    @SneakyThrows
    @Test
    void testHandleEvent_createAssessmentStudent_ShouldPublishStudent() {
        final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
        final var event = Event.builder()
                .eventType(INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .sagaId(this.saga.getSagaId())
                .eventPayload(sagaPayload)
                .build();
        this.studentRegistrationOrchestrator.handleEvent(event);
        verify(this.messagePublisher, atMost(invocations + 2)).dispatchMessage(eq(this.studentRegistrationOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT_REGISTRATION);
        assertThat(newEvent.getEventOutcome()).isEqualTo(STUDENT_REGISTRATION_CREATED);

        final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId());
        assertThat(sagaFromDB).isPresent();
        assertThat(sagaFromDB.get().getSagaState()).isEqualTo(CREATE_STUDENT_REGISTRATION.toString());

        final var nextEvent = Event.builder()
                .eventType(CREATE_STUDENT_REGISTRATION)
                .eventOutcome(EventOutcome.STUDENT_REGISTRATION_CREATED)
                .sagaId(this.saga.getSagaId())
                .eventPayload(newEvent.getEventPayload())
                .build();
        this.studentRegistrationOrchestrator.handleEvent(nextEvent);

        verify(this.messagePublisher, atMost(invocations + 3)).dispatchMessage(eq(this.studentRegistrationOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var nextNewEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(nextNewEvent.getEventType()).isEqualTo(PUBLISH_STUDENT_REGISTRATION);
        assertThat(nextNewEvent.getEventOutcome()).isEqualTo(STUDENT_REGISTRATION_PUBLISHED);
    }

    @SneakyThrows
    @Test
    void testMarkSagaCompleteEvent_GivenEventAndSagaData_ShouldMarkSagaCompleted() throws IOException, InterruptedException, TimeoutException {
        final var invocations = mockingDetails(this.messagePublisher).getInvocations().size();
        final var event = Event.builder()
                .eventType(PUBLISH_STUDENT_REGISTRATION)
                .eventOutcome(STUDENT_REGISTRATION_PUBLISHED)
                .sagaId(this.saga.getSagaId())
                .eventPayload(sagaPayload)
                .build();
        this.studentRegistrationOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(this.studentRegistrationOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(MARK_SAGA_COMPLETE);
        assertThat(newEvent.getEventOutcome()).isEqualTo(SAGA_COMPLETED);
    }


}
