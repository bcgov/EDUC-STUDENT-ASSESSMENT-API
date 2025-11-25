package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentResultMapper;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResult;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventType.CREATE_STUDENT_RESULT;
import static ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class StudentResultProcessingOrchestratorTest extends BaseAssessmentAPITest {

    @Autowired
    private MessagePublisher messagePublisher;
    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    SagaRepository sagaRepository;
    @Autowired
    SagaEventRepository sagaEventRepository;
    @Autowired
    private AssessmentFormRepository assessmentFormRepository;
    private AssessmentEntity savedAssessmentEntity;
    private AssessmentFormEntity savedAssessmentFormEntity;
    @MockBean
    private RestUtils restUtils;
    @Autowired
    private AssessmentComponentRepository assessmentComponentRepository;
    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;
    @Autowired
    StudentResultProcessingOrchestrator studentResultProcessingOrchestrator;
    @Captor
    ArgumentCaptor<byte[]> eventCaptor;
    @Autowired
    StagedStudentResultRepository stagedStudentResultRepository;
    @Autowired
    AssessmentStudentRepository assessmentStudentRepository;
    @Autowired
    private AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    @Autowired
    AssessmentChoiceRepository  assessmentChoiceRepository;

    private AssessmentComponentEntity savedMultiComp;
    private AssessmentComponentEntity savedOpenEndedComp;

    @BeforeEach
    void setUp() {
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        assessmentStudentRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        stagedStudentResultRepository.deleteAll();
        assessmentQuestionRepository.deleteAll();
        assessmentComponentRepository.deleteAll();
        assessmentFormRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();

        Mockito.reset(this.messagePublisher);
        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        savedAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTP10.getCode()));
        savedAssessmentFormEntity = assessmentFormRepository.save(createMockAssessmentFormEntity(savedAssessmentEntity, "A"));

        // Setup multiple choice questions (28 questions)
        savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedAssessmentFormEntity, "MUL_CHOICE", "NONE"));
        for(int i = 1; i < 29; i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        // Setup open-ended questions (4 questions)
        savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedAssessmentFormEntity, "OPEN_ENDED", "NONE"));
        assessmentChoiceRepository.save(createMockAssessmentChoiceEntity(savedOpenEndedComp, 2, 1));
        assessmentChoiceRepository.save(createMockAssessmentChoiceEntity(savedOpenEndedComp, 4, 4));

        GradStudentRecord gradStudentRecord = new GradStudentRecord();
        gradStudentRecord.setSchoolOfRecordId(UUID.randomUUID().toString());
        gradStudentRecord.setStudentStatusCode("CUR");
        gradStudentRecord.setGraduated("false");
        gradStudentRecord.setProgramCompletionDate("2023-06-30");
        when(restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeTypeInitiated_createStudentResultRecord_WhereStudentStatusIsActive_WithEventOutCome_STUDENT_RESULT_CREATED() {
        var school = this.createMockSchoolTombstone();
        school.setMincode("07965039");
        when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));

        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        Student stud1 = new Student();
        stud1.setStudentID(UUID.randomUUID().toString());
        stud1.setDob("1990-01-01");
        stud1.setLegalLastName("LAST NAME");
        stud1.setLegalFirstName("FIRSTNAME");
        stud1.setPen("123456789");
        stud1.setStatusCode("A");
        when(this.restUtils.getStudentByPEN(any(), any())).thenReturn(Optional.of(stud1));

        var studentResult = StagedStudentResultEntity
                .builder()
                .assessmentEntity(savedAssessmentEntity)
                .assessmentFormID(savedAssessmentFormEntity.getAssessmentFormID())
                .pen("123456789")
                .mincode("07965039")
                .stagedStudentResultStatus("LOADED")
                .componentType("3")
                .oeMarks("02.003.004.004.003.003.0")
                .mcMarks("00.000.000.001.001.000.001.001.001.001.000.001.001.000.000.000.001.001.000.001.001.000.001.000.001.001.000.001")
                .choicePath(null)
                .provincialSpecialCaseCode(null)
                .proficiencyScore(3)
                .adaptedAssessmentCode(null)
                .irtScore("0.4733")
                .markingSession(null)
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .updateUser("TEST")
                .build();

        var savedStudentResult = stagedStudentResultRepository.save(studentResult);
        var sagaData = StudentResultSagaData
                .builder()
                .assessmentID(String.valueOf(savedStudentResult.getAssessmentEntity().getAssessmentID()))
                .pen(savedStudentResult.getPen())
                .build();

        val saga = this.createStudentResultMockSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.studentResultProcessingOrchestrator.handleEvent(event);


        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.studentResultProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT_RESULT);
        assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_RESULT_CREATED);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(CREATE_STUDENT_RESULT.toString());

        val stagedResult = stagedStudentResultRepository.findById(savedStudentResult.getStagedStudentResultID());
        assertThat(stagedResult).isPresent();
        assertThat(stagedResult.get().getStagedStudentResultStatus()).isEqualTo("COMPLETED");
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeTypeInitiated_createStudentResultRecord_WhereStudentStatusIsMerged_WithEventOutCome_STUDENT_RESULT_CREATED() {
        var school = this.createMockSchoolTombstone();
        school.setMincode("07965039");
        when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));

        Student stud1 = new Student();
        stud1.setStudentID(UUID.randomUUID().toString());
        stud1.setDob("1990-01-01");
        stud1.setLegalLastName("LAST NAME");
        stud1.setLegalFirstName("FIRSTNAME");
        stud1.setPen("123456789");
        stud1.setStatusCode("M");
        stud1.setTrueStudentID(UUID.randomUUID().toString());
        when(this.restUtils.getStudentByPEN(any(), any())).thenReturn(Optional.of(stud1));

        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));
        when(restUtils.getStudents(any(), any())).thenReturn(List.of(stud1));

        var studentResult = StagedStudentResultEntity
                .builder()
                .assessmentEntity(savedAssessmentEntity)
                .assessmentFormID(savedAssessmentFormEntity.getAssessmentFormID())
                .pen("123456789")
                .mincode("07965039")
                .stagedStudentResultStatus("LOADED")
                .componentType("3")
                .oeMarks("03.003.004.003.003.003.0")
                .mcMarks("00.000.000.001.001.000.001.001.001.001.000.001.001.000.000.000.001.001.000.001.001.000.001.000.001.001.000.001")
                .choicePath(null)
                .provincialSpecialCaseCode(null)
                .proficiencyScore(3)
                .adaptedAssessmentCode(null)
                .irtScore("0.4733")
                .markingSession(null)
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .updateUser("TEST")
                .build();

        var savedStudentResult = stagedStudentResultRepository.save(studentResult);

        var sagaData = StudentResultSagaData
                .builder()
                .assessmentID(String.valueOf(savedStudentResult.getAssessmentEntity().getAssessmentID()))
                .pen(savedStudentResult.getPen())
                .build();

        val saga = this.createStudentResultMockSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.studentResultProcessingOrchestrator.handleEvent(event);


        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.studentResultProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT_RESULT);
        assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_RESULT_CREATED);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(CREATE_STUDENT_RESULT.toString());

        val stagedResult = stagedStudentResultRepository.findById(savedStudentResult.getStagedStudentResultID());
        assertThat(stagedResult).isPresent();
        assertThat(stagedResult.get().getStagedStudentResultStatus()).isEqualTo("COMPLETED");
    }


    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeTypeInitiatedAndPENNotInStudentAPI_createStudentResultRecordWithEventOutCome_STUDENT_RESULT_CREATED() {
        var school = this.createMockSchoolTombstone();
        school.setMincode("07965039");
        when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));

        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));
        when(this.restUtils.getStudentByPEN(any(), any())).thenReturn(Optional.empty());

        var studentResult = StagedStudentResultEntity
                .builder()
                .assessmentEntity(savedAssessmentEntity)
                .assessmentFormID(savedAssessmentFormEntity.getAssessmentFormID())
                .pen("123456789")
                .mincode("07965039")
                .stagedStudentResultStatus("LOADED")
                .componentType("1")
                .oeMarks("03.003.004.003.003.003.0")
                .mcMarks("00.000.000.001.001.000.001.001.001.001.000.001.001.000.000.000.001.001.000.001.001.000.001.000.001.001.000.001")
                .choicePath(null)
                .provincialSpecialCaseCode(null)
                .proficiencyScore(3)
                .adaptedAssessmentCode(null)
                .irtScore("0.4733")
                .markingSession(null)
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .updateUser("TEST")
                .build();

        var savedStudentResult = stagedStudentResultRepository.save(studentResult);

        var sagaData = StudentResultSagaData
                .builder()
                .assessmentID(String.valueOf(savedStudentResult.getAssessmentEntity().getAssessmentID()))
                .pen(savedStudentResult.getPen())
                .build();

        val saga = this.createStudentResultMockSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.studentResultProcessingOrchestrator.handleEvent(event);


        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.studentResultProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT_RESULT);
        assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_RESULT_CREATED);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(CREATE_STUDENT_RESULT.toString());

        val stagedResult = stagedStudentResultRepository.findById(savedStudentResult.getStagedStudentResultID());
        assertThat(stagedResult).isPresent();
        assertThat(stagedResult.get().getStagedStudentResultStatus()).isEqualTo("COMPLETED");
    }

    @SneakyThrows
    @Test
    void testOrchestratorHandles_givenEventType_CALCULATE_STUDENT_DOAR_shouldExecuteCreateAndPopulateDOARCalculations() {
        var studentEntity1 = createMockStagedStudentEntity(savedAssessmentEntity);
        var componentEntity1 = createMockStagedAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockStagedAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }
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

        componentEntity2.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getStagedAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedAssessmentFormEntity.getAssessmentFormID());
        studentEntity1.setStudentID(UUID.randomUUID());
        stagedAssessmentStudentRepository.save(studentEntity1);

        var sagaData = StudentResultSagaData
                .builder()
                .assessmentID(String.valueOf(studentEntity1.getAssessmentEntity().getAssessmentID()))
                .pen(studentEntity1.getPen())
                .build();

        val saga = this.createStudentResultMockSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        Event event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.CREATE_STUDENT_RESULT)
                .eventOutcome(EventOutcome.STUDENT_RESULT_CREATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData))
                .build();

        studentResultProcessingOrchestrator.handleEvent(event);

        verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(studentResultProcessingOrchestrator.getTopicToSubscribe()), eventCaptor.capture());
        String dispatchedPayload = new String(eventCaptor.getValue());
        Event dispatchedEvent = JsonUtil.getJsonObjectFromString(Event.class, dispatchedPayload);
        assertThat(dispatchedEvent.getEventType()).isEqualTo(EventType.CALCULATE_STAGED_STUDENT_DOAR);
        assertThat(dispatchedEvent.getEventOutcome()).isEqualTo(EventOutcome.STAGED_STUDENT_DOAR_CALCULATED);
    }

}
