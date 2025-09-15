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
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
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

import java.math.BigDecimal;
import java.util.List;
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

    @Autowired
    private AssessmentFormRepository assessmentFormRepository;
    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;
    @Autowired
    private AssessmentComponentRepository assessmentComponentRepository;
    @Autowired
    AssessmentStudentRepository studentRepository;
    @Autowired
    AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository;

    @Captor
    private ArgumentCaptor<byte[]> eventCaptor;

    private String sagaPayload;
    private AssessmentSagaEntity saga;

    @AfterEach
    void after() {
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        assessmentStudentDOARCalculationRepository.deleteAll();
        assessmentStudentRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        assessmentFormRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(this.messagePublisher);

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
        StagedAssessmentStudentEntity stagedStudent = createMockStagedStudentEntity(assessment);
        stagedStudent.setStagedAssessmentStudentStatus("TRANSFER");
        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        var sagaData = TransferOnApprovalSagaData.builder()
                .assessmentID(String.valueOf(stagedStudent.getAssessmentEntity().getAssessmentID()))
                .studentID(String.valueOf(stagedStudent.getStudentID()))
                .stagedStudentAssessmentID(String.valueOf(stagedStudent.getAssessmentStudentID()))
                .build();
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
    void testOrchestratorHandles_givenEventType_CALCULATE_STUDENT_DOAR_shouldExecuteCreateAndPopulateDOARCalculations() {
        var sagaPayloadObject = JsonUtil.getJsonObjectFromString(TransferOnApprovalSagaData.class, sagaPayload);

        var assessmentEntity = assessmentRepository.findById(UUID.fromString(sagaPayloadObject.getAssessmentID())).orElse(null);
        var assessmentStudent = createMockStudentEntity(assessmentEntity);
        assessmentStudent.setStudentID(UUID.fromString(sagaPayloadObject.getStudentID()));
        assessmentStudentRepository.save(assessmentStudent);

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
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.CALCULATE_STUDENT_DOAR);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_DOAR_CALCULATED);
    }

    @SneakyThrows
    @Test
    void testOrchestratorHandles_givenEventType_CALCULATE_STUDENT_DOAR_shouldExecuteCreateAndPopulateDOARCalculationsForLTF12() {
        var sagaPayloadObject = JsonUtil.getJsonObjectFromString(TransferOnApprovalSagaData.class, sagaPayload);

        var assessmentEntity = assessmentRepository.findById(UUID.fromString(sagaPayloadObject.getAssessmentID())).orElse(null);
        setData(assessmentEntity, sagaPayloadObject.getStudentID());

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
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.CALCULATE_STUDENT_DOAR);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_DOAR_CALCULATED);
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
        TransferStudentProcessingOrchestrator orchestratorWithMocks = new TransferStudentProcessingOrchestrator(mockSagaService, mockMessagePublisher, null, null, null, null);

        AssessmentSagaEntity dummySaga = new AssessmentSagaEntity();
        dummySaga.setSagaId(UUID.randomUUID());
        when(mockSagaService.createSagaRecordInDB(anyString(), anyString(), anyString(), isNull(), any(UUID.class))).thenReturn(dummySaga);

        var sagaData = TransferOnApprovalSagaData.builder()
                        .assessmentID(String.valueOf(UUID.randomUUID()))
                        .studentID(String.valueOf(UUID.randomUUID()))
                        .stagedStudentAssessmentID(String.valueOf(UUID.randomUUID()))
                        .build();

        orchestratorWithMocks.startStudentTransferProcessingSaga(sagaData);

        verify(mockSagaService, atLeastOnce()).createSagaRecordInDB(anyString(), anyString(), anyString(), isNull(), any(UUID.class));
        verify(mockMessagePublisher, atLeastOnce()).dispatchMessage(eq(orchestratorWithMocks.getTopicToSubscribe()), eventCaptor.capture());

        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);

        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.INITIATED);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS);
    }

    private AssessmentFormEntity setData(AssessmentEntity savedAssessmentEntity, String studentID) {
        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(savedAssessmentEntity, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var q1 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2);
        q1.setMasterQuestionNumber(2);
        var oe1 = assessmentQuestionRepository.save(q1);

        var q2 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3);
        q2.setMasterQuestionNumber(2);
        assessmentQuestionRepository.save(q2);

        var q3 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5);
        q3.setMasterQuestionNumber(4);
        assessmentQuestionRepository.save(q3);

        var q4 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6);
        q4.setMasterQuestionNumber(4);
        var oe4 = assessmentQuestionRepository.save(q4);

        var studentEntity1 = createMockStudentEntity(savedAssessmentEntity);
        var componentEntity1 = createMockAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        studentEntity1.setStudentID(UUID.fromString(studentID));
        studentRepository.save(studentEntity1);

        return savedForm;
    }
}
