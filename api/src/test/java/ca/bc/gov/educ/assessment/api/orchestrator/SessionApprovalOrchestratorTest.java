package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.XAMFileService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.ApprovalSagaData;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
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
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class SessionApprovalOrchestratorTest extends BaseAssessmentAPITest {

    @Autowired
    private SessionApprovalOrchestrator sessionApprovalOrchestrator;

    @Autowired
    private SagaService sagaService;

    @Autowired
    private MessagePublisher messagePublisher;
    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    AssessmentStudentRepository assessmentStudentRepository;
    @Autowired
    AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    @Autowired
    SagaRepository sagaRepository;
    @Autowired
    SagaEventRepository sagaEventRepository;

    @Captor 
    private ArgumentCaptor<byte[]> eventCaptor;
    ApprovalSagaData sagaData;
    String sagaPayload;
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
        AssessmentStudent student = createMockStudent();
        student.setAssessmentID(assessment.getAssessmentID().toString());
        sagaData = ApprovalSagaData.builder().sessionID(String.valueOf(session.getSessionID())).build();
        MockitoAnnotations.openMocks(this);
        sagaPayload = JsonUtil.getJsonString(sagaData).get();
        saga = this.sagaService.createSagaRecordInDB(SagaEnum.GENERATE_XAM_FILES.name(), "test", sagaPayload, UUID.fromString(sagaData.getSessionID()), null, null, null);
    }

    @SneakyThrows
    @Test
    void testOrchestratorHandlesEventAndDelegatesStep0ToService() {
        String payload = sagaPayload;
        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .eventPayload(payload)
                .build();

        sessionApprovalOrchestrator.handleEvent(event);

        verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(sessionApprovalOrchestrator.getTopicToSubscribe()), eventCaptor.capture());
        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.GENERATE_XAM_FILES_AND_UPLOAD);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.XAM_FILES_GENERATED_AND_UPLOADED);
    }
    
    @SneakyThrows
    @Test
    void testOrchestratorHandlesEventAndDelegatesStep1ToService() {
        String payload = sagaPayload;
        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.GENERATE_XAM_FILES_AND_UPLOAD)
                .eventOutcome(EventOutcome.XAM_FILES_GENERATED_AND_UPLOADED)
                .eventPayload(payload)
                .build();
        
        sessionApprovalOrchestrator.handleEvent(event);

        verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(sessionApprovalOrchestrator.getTopicToSubscribe()), eventCaptor.capture());
        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.NOTIFY_MYED_OF_UPDATED_STUDENTS);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.MYED_NOTIFIED);
    }

    @SneakyThrows
    @Test
    void testOrchestratorHandlesEventAndDelegatesStep2ToService() {
        String payload = sagaPayload;
        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.NOTIFY_MYED_OF_UPDATED_STUDENTS)
                .eventOutcome(EventOutcome.MYED_NOTIFIED)
                .eventPayload(payload)
                .build();

        sessionApprovalOrchestrator.handleEvent(event);

        verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(sessionApprovalOrchestrator.getTopicToSubscribe()), eventCaptor.capture());
        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.MARK_SESSION_COMPLETION_DATE);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.COMPLETION_DATE_SET);
    }

    @SneakyThrows
    @Test
    void testOrchestratorHandlesEventAndDelegatesStep3ToService() {
        String payload = sagaPayload;
        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.MARK_SESSION_COMPLETION_DATE)
                .eventOutcome(EventOutcome.COMPLETION_DATE_SET)
                .eventPayload(payload)
                .build();

        sessionApprovalOrchestrator.handleEvent(event);

        verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(sessionApprovalOrchestrator.getTopicToSubscribe()), eventCaptor.capture());
        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.MARK_STAGED_STUDENTS_READY_FOR_TRANSFER);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.STAGED_STUDENTS_MARKED_READY_FOR_TRANSFER);
    }

    @SneakyThrows
    @Test
    void testStartXamFileGenerationSagaCreatesSagaRecord() {
        UUID newSessionID = UUID.fromString(sagaData.getSessionID());
        sessionApprovalOrchestrator.startXamFileGenerationSaga(newSessionID);
        AssessmentSagaEntity newSaga = sagaService.findByAssessmentStudentIDAndSagaNameAndStatusNot(newSessionID, SagaEnum.GENERATE_XAM_FILES.toString(), SagaStatusEnum.IN_PROGRESS.toString()).orElse(null);
        assertThat(newSaga).isNotNull();
        assertEquals(newSessionID, newSaga.getAssessmentStudentID());
    }

    @SneakyThrows
    @Test
    void testGenerateXamFilesAndUpload_invalidPayload_directCall() {
        var approvalSagaData = ApprovalSagaData.builder().sessionID("not-a-valid-uuid").build();
        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.GENERATE_XAM_FILES_AND_UPLOAD)
                .eventOutcome(EventOutcome.XAM_FILES_GENERATED_AND_UPLOADED)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();

        AssessmentStudentRepository dummyStudentRepo = Mockito.mock(AssessmentStudentRepository.class);
        AssessmentSessionRepository dummySessionRepo = Mockito.mock(AssessmentSessionRepository.class);
        RestUtils dummyRestUtils = Mockito.mock(RestUtils.class);
        S3Client dummyS3Client = Mockito.mock(S3Client.class);
        ApplicationProperties dummyApplicationProperties = Mockito.mock(ApplicationProperties.class);
        StagedAssessmentStudentRepository dummyStagedAssessmentStudentRepository = Mockito.mock(StagedAssessmentStudentRepository.class);
        XAMFileService dummyService = new XAMFileService(dummyStudentRepo, dummySessionRepo, dummyRestUtils, dummyS3Client, dummyApplicationProperties, dummyStagedAssessmentStudentRepository);
        XAMFileService spyService = Mockito.spy(dummyService);
        ReflectionTestUtils.setField(sessionApprovalOrchestrator, "xamFileService", spyService);

        Method method = SessionApprovalOrchestrator.class
                .getDeclaredMethod("generateXAMFilesAndUpload", Event.class, AssessmentSagaEntity.class, ApprovalSagaData.class);
        method.setAccessible(true);
        Exception thrown = assertThrows(Exception.class, () -> {
            method.invoke(sessionApprovalOrchestrator, event, saga, approvalSagaData);
        });
        assertThat(thrown.getCause()).isInstanceOf(IllegalArgumentException.class);
    }
}
