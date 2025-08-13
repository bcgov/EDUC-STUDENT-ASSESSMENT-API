package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TransferStudentProcessingOrchestratorTest extends BaseAssessmentAPITest {

    @Autowired
    private TransferStudentProcessingOrchestrator transferStudentProcessingOrchestrator;

    @Autowired
    private SagaService sagaService;

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private StagedAssessmentStudentRepository stagedAssessmentStudentRepository;

    @Autowired
    private AssessmentSessionRepository assessmentSessionRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssessmentStudentRepository assessmentStudentRepository;

    @Autowired
    private AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

    @Autowired
    private SagaRepository sagaRepository;

    @Autowired
    private SagaEventRepository sagaEventRepository;

    @Captor
    private ArgumentCaptor<byte[]> eventCaptor;

    private String sagaPayload;
    private AssessmentSagaEntity saga;

    @AfterEach
    public void after() {
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        assessmentStudentRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(this.messagePublisher);

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
        StagedAssessmentStudentEntity stagedStudent = createMockStagedStudentEntity(assessment);
        stagedStudent.setStagedAssessmentStudentStatus("TRANSFER");
        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        UUID sagaData = stagedStudent.getAssessmentStudentID();
        MockitoAnnotations.openMocks(this);
        sagaPayload = JsonUtil.getJsonString(sagaData).get();
        saga = this.sagaService.createSagaRecordInDB(
            SagaEnum.PROCESS_STUDENT_TRANSFER.name(),
            "test",
            sagaPayload,
            null,
            stagedStudent.getAssessmentStudentID()
        );
    }

    @SneakyThrows
    @Test
    void testOrchestratorHandlesInitiatedEventAndDelegatesStep0ToService() {
        String payload = sagaPayload;
        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .eventPayload(payload)
                .build();

        transferStudentProcessingOrchestrator.handleEvent(event);

        verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(transferStudentProcessingOrchestrator.getTopicToSubscribe()), eventCaptor.capture());
        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.PROCESS_STUDENT_TRANSFER_EVENT);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_TRANSFER_PROCESSED);
    }

    @SneakyThrows
    @Test
    void testOrchestratorHandlesProcessStudentTransferEventAndCompletesSaga() {
        String payload = sagaPayload;
        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.PROCESS_STUDENT_TRANSFER_EVENT)
                .eventOutcome(EventOutcome.STUDENT_TRANSFER_PROCESSED)
                .eventPayload(payload)
                .build();

        transferStudentProcessingOrchestrator.handleEvent(event);

        verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(transferStudentProcessingOrchestrator.getTopicToSubscribe()), eventCaptor.capture());
        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.MARK_SAGA_COMPLETE);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.SAGA_COMPLETED);
    }

    @Test
    void testOrchestratorGetSagaName() {
        assertEquals(SagaEnum.PROCESS_STUDENT_TRANSFER.toString(), transferStudentProcessingOrchestrator.getSagaName());
    }

    @Test
    void testOrchestratorGetTopicToSubscribe() {
        assertEquals("STUDENT_TRANSFER_PROCESSING_TOPIC", transferStudentProcessingOrchestrator.getTopicToSubscribe());
    }

    @SneakyThrows
    @Test
    void testStartStudentTransferProcessingSaga_initiatesSagaAndDispatchesEvent() {
        SagaService mockSagaService = Mockito.mock(SagaService.class);
        MessagePublisher mockMessagePublisher = Mockito.mock(MessagePublisher.class);
        TransferStudentProcessingOrchestrator orchestratorWithMocks = new TransferStudentProcessingOrchestrator(mockSagaService, mockMessagePublisher, null);

        AssessmentSagaEntity dummySaga = new AssessmentSagaEntity();
        dummySaga.setSagaId(UUID.randomUUID());
        when(mockSagaService.createSagaRecordInDB(anyString(), anyString(), anyString(), isNull(), any(UUID.class))).thenReturn(dummySaga);

        UUID stagedStudentId = UUID.randomUUID();

        orchestratorWithMocks.startStudentTransferProcessingSaga(stagedStudentId);

        verify(mockSagaService, atLeastOnce()).createSagaRecordInDB(anyString(), anyString(), anyString(), isNull(), any(UUID.class));
        verify(mockMessagePublisher, atLeastOnce()).dispatchMessage(eq(orchestratorWithMocks.getTopicToSubscribe()), eventCaptor.capture());

        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);

        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.INITIATED);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS);
    }

    private AssessmentStudentEntity createMainStudentFromStaged(StagedAssessmentStudentEntity staged) {
        return AssessmentStudentEntity.builder()
                .assessmentEntity(staged.getAssessmentEntity())
                .studentID(staged.getStudentID())
                .pen(staged.getPen())
                .surname(staged.getSurname())
                .givenName(staged.getGivenName())
                .schoolOfRecordSchoolID(staged.getSchoolOfRecordSchoolID())
                .proficiencyScore(staged.getProficiencyScore())
                .createUser(staged.getCreateUser())
                .createDate(staged.getCreateDate())
                .updateUser(staged.getUpdateUser())
                .updateDate(staged.getUpdateDate())
                .build();
    }
}
