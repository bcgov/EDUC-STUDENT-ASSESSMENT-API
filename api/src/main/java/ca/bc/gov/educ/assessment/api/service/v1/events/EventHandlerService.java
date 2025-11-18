package ca.bc.gov.educ.assessment.api.service.v1.events;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentListItemMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.SessionMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.StudentResultProcessingOrchestrator;
import ca.bc.gov.educ.assessment.api.orchestrator.TransferStudentProcessingOrchestrator;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.*;
import ca.bc.gov.educ.assessment.api.util.EventUtil;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.assessment.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.ASSESSMENT_STUDENT_UPDATE;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("java:S3864")
public class EventHandlerService {

    public static final String NO_EXECUTION_MSG = "Execution is not required for this message returning EVENT is :: {}";

    /**
     * The constant PAYLOAD_LOG.
     */
    public static final String PAYLOAD_LOG = "payload is :: {}";
    /**
     * The constant EVENT_PAYLOAD.
     */
    public static final String EVENT_PAYLOAD = "event is :: {}";
    private static final AssessmentStudentMapper assessmentStudentMapper = AssessmentStudentMapper.mapper;
    private static final AssessmentStudentListItemMapper assessmentStudentListItemMapper = AssessmentStudentListItemMapper.mapper;
    private static final SessionMapper sessionMapper = SessionMapper.mapper;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentEventRepository assessmentEventRepository;
    private final AssessmentStudentService assessmentStudentService;
    private final SagaService sagaService;
    private final StudentResultProcessingOrchestrator studentResultProcessingOrchestrator;
    private final TransferStudentProcessingOrchestrator transferStudentProcessingOrchestrator;
    private final AssessmentStudentRepository assessmentStudentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<byte[], AssessmentEventEntity> handleProcessStudentRegistrationEvent(Event event) throws JsonProcessingException {
        final AssessmentStudent assessmentStudent = JsonUtil.getJsonObjectFromString(AssessmentStudent.class, event.getEventPayload());
        Optional<AssessmentStudentEntity> student = assessmentStudentService.getStudentByAssessmentIDAndStudentID(UUID.fromString(assessmentStudent.getAssessmentID()), UUID.fromString(assessmentStudent.getStudentID()));
        log.debug("handleCreateStudentRegistrationEvent found student :: {}", student);

        var isWithdrawal = StringUtils.isNotBlank(assessmentStudent.getCourseStatusCode()) && assessmentStudent.getCourseStatusCode().equalsIgnoreCase("W");

        boolean dataChangedForStudent = false;
        if(isWithdrawal && student.isEmpty()){
            log.error("Student withdrawal record submitted but no registration record is present; ignoring message to remove record");
        } else if (isWithdrawal) {
            log.debug("Removing student registration due to incoming withdrawal record :: student ID: {}", student.get().getStudentID());
            assessmentStudentService.deleteStudent(student.get().getAssessmentStudentID());
            dataChangedForStudent = true;
        } else if (student.isEmpty()) {
            log.debug("handleCreateStudentRegistrationEvent setting audit columns :: {}", student);
            RequestUtil.setAuditColumnsForCreate(assessmentStudent);
            AssessmentStudentEntity createStudentEntity = assessmentStudentMapper.toModel(assessmentStudent);
            var attempts = assessmentStudentService.getNumberOfAttempts(createStudentEntity.getAssessmentEntity().getAssessmentID().toString(), createStudentEntity.getStudentID());
            createStudentEntity.setNumberOfAttempts(Integer.parseInt(attempts));
            log.debug("Writing student entity: " + createStudentEntity);
            assessmentStudentService.saveAssessmentStudentWithHistory(createStudentEntity);
            dataChangedForStudent = true;
        } else {
            log.info("Student already exists in assessment {} updating school or record school id, local id, local assessment id, assessment center school id", assessmentStudent);

            AssessmentStudentEntity existingStudentEntity = student.get();
            final String schoolOfRecordSchoolID = assessmentStudent.getSchoolOfRecordSchoolID();
            if (StringUtils.isNotBlank(schoolOfRecordSchoolID)) {
                try {
                    existingStudentEntity.setSchoolOfRecordSchoolID(UUID.fromString(schoolOfRecordSchoolID));
                } catch (IllegalArgumentException e) {
                    // ignore invalid UUID; keep existing value
                }
            }
            existingStudentEntity.setLocalID(assessmentStudent.getLocalID());
            existingStudentEntity.setLocalAssessmentID(assessmentStudent.getLocalAssessmentID());
            final String assessmentCenterSchoolID = assessmentStudent.getAssessmentCenterSchoolID();
            if (StringUtils.isNotBlank(assessmentCenterSchoolID)) {
                try {
                    existingStudentEntity.setAssessmentCenterSchoolID(UUID.fromString(assessmentCenterSchoolID));
                } catch (IllegalArgumentException e) {
                    // ignore invalid UUID; keep existing value
                }
            }

            assessmentStudentService.saveAssessmentStudentWithHistory(existingStudentEntity);
            dataChangedForStudent = true;
        }

        AssessmentEventEntity assessmentEventEntity = null;
        if(dataChangedForStudent) {
            assessmentEventEntity = EventUtil.createEvent(
                    assessmentStudent.getUpdateUser(), assessmentStudent.getUpdateUser(),
                    JsonUtil.getJsonStringFromObject(assessmentStudent.getStudentID()),
                    ASSESSMENT_STUDENT_UPDATE, EventOutcome.ASSESSMENT_STUDENT_UPDATED);

            log.debug("Assessment event is: {}", assessmentEventEntity);
            log.debug("Assessment student is: {}", assessmentStudent);
            assessmentEventRepository.save(assessmentEventEntity);
        }

        event.setEventOutcome(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);
        val studentEvent = createEventRecord(event);
        return Pair.of(createResponseEvent(studentEvent), assessmentEventEntity);
    }

    public byte[] handleGetOpenAssessmentSessionsEvent(Event event, boolean isSynchronous) throws JsonProcessingException {
        var currentDate = LocalDateTime.now();
        val sessions = assessmentSessionRepository.findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqualAndCompletionDateIsNull(currentDate, currentDate);
        var sessionStructs = new ArrayList<>();
        sessions.forEach(sessionStruct -> sessionStructs.add(sessionMapper.toStructure(sessionStruct)));
        if (isSynchronous) {
            return JsonUtil.getJsonBytesFromObject(sessionStructs);
        }

        log.trace(EVENT_PAYLOAD, event);
        event.setEventPayload(JsonUtil.getJsonStringFromObject(sessionStructs));
        event.setEventOutcome(EventOutcome.SESSIONS_FOUND);
        val studentEvent = createEventRecord(event);
        return createResponseEvent(studentEvent);
    }

    public byte[] handleGetAssessmentStudentsEvent(Event event) throws JsonProcessingException {
        val assessmentStudentEntityList = assessmentStudentRepository.findByStudentID(UUID.fromString(event.getEventPayload()));
        log.info("Found :: {} assessment student records for student ID :: {}", assessmentStudentEntityList.size(), UUID.fromString(event.getEventPayload()));
        if (!assessmentStudentEntityList.isEmpty()) {
            var assessmentStudentList = assessmentStudentEntityList.stream().map(assessmentStudentListItemMapper::toStructure).collect(Collectors.toList());
            return JsonUtil.getJsonBytesFromObject(assessmentStudentList);
        } else {
            return new byte[0];
        }
    }

    public byte[] handleGetStudentAssessmentDetailEvent(Event event) throws JsonProcessingException {
        AssessmentStudentGet student = JsonUtil.getJsonObjectFromString(AssessmentStudentGet.class, event.getEventPayload());
        log.debug("handleGetStudentAssessmentDetailEvent - incoming payload student :: {}", student);
        var response = new AssessmentStudentDetailResponse();

        val optionalStudentEntity = assessmentStudentService.getStudentByAssessmentIDAndStudentID(UUID.fromString(student.getAssessmentID()), UUID.fromString(student.getStudentID()));

        if (optionalStudentEntity.isPresent()) {
            response.setHasPriorRegistration(true);

            var stud = optionalStudentEntity.get();
            if (stud.getProficiencyScore() != null || StringUtils.isNotBlank(stud.getProvincialSpecialCaseCode())) {
                response.setAlreadyWrittenAssessment(true);
                log.debug("Student has prior written assessment or special case :: {}", stud.getAssessmentStudentID());
            }
        }

        val numberOfAttempts = assessmentStudentService.getNumberOfAttempts(student.getAssessmentID(), UUID.fromString(student.getStudentID()));
        response.setNumberOfAttempts(numberOfAttempts);

        log.debug("Returning response for assessmentID: {} studentID: {} - {}", student.getAssessmentID(), student.getStudentID(), response);

        return JsonUtil.getJsonBytesFromObject(response);
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void handleProcessStudentResultEvent(final Event event) throws JsonProcessingException {
        if (event.getEventOutcome() == EventOutcome.READ_STUDENT_RESULT_FOR_PROCESSING_SUCCESS) {
            final StudentResultSagaData sagaData = JsonUtil.getJsonObjectFromString(StudentResultSagaData.class, event.getEventPayload());
            final var sagaList = sagaService.findByAssessmentIDAndPenAndSagaNameAndStatusNot(UUID.fromString(sagaData.getAssessmentID()), sagaData.getPen(), SagaEnum.PROCESS_STUDENT_RESULT.toString(), SagaStatusEnum.COMPLETED.toString());
            if (!sagaList.isEmpty()) { // possible duplicate message.
                log.trace(NO_EXECUTION_MSG, event);
                return;
            }
            val saga = this.studentResultProcessingOrchestrator
                    .createSaga(event.getEventPayload(),
                            ApplicationProperties.STUDENT_ASSESSMENT_API,
                            null,
                            UUID.fromString(sagaData.getStagedStudentResultID()),
                            UUID.fromString(sagaData.getAssessmentID()),
                            sagaData.getPen());
            log.debug("Starting course student processing orchestrator :: {}", saga);
            this.studentResultProcessingOrchestrator.startSaga(saga);
        }
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void handleProcessTransferStudentResultEvent(final Event event) throws JsonProcessingException {
        if (event.getEventOutcome() == EventOutcome.READ_TRANSFER_STUDENT_RESULT_SUCCESS) {
            final TransferOnApprovalSagaData sagaData = JsonUtil.getJsonObjectFromString(TransferOnApprovalSagaData.class, event.getEventPayload());
            final var sagaList = sagaService.findByAssessmentStudentIDAndSagaNameAndStatusNot(UUID.fromString(sagaData.getStagedStudentAssessmentID()), SagaEnum.PROCESS_STUDENT_TRANSFER.toString(), SagaStatusEnum.COMPLETED.toString());
            if (sagaList.isPresent()) { // possible duplicate message.
                log.trace(NO_EXECUTION_MSG, event);
                return;
            }
            val saga = this.transferStudentProcessingOrchestrator
                    .createSaga(event.getEventPayload(),
                            ApplicationProperties.STUDENT_ASSESSMENT_API,
                            null,
                            UUID.fromString(sagaData.getStagedStudentAssessmentID()),
                            null,
                            null);
            log.debug("Starting transfer student processing orchestrator :: {}", saga);
            this.transferStudentProcessingOrchestrator.startSaga(saga);
        }
    }

    private AssessmentEventEntity createEventRecord(Event event) {
        return AssessmentEventEntity.builder()
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .eventPayload(event.getEventPayload())
                .eventType(event.getEventType().toString())
                .sagaId(event.getSagaId())
                .eventStatus(MESSAGE_PUBLISHED.toString())
                .eventOutcome(event.getEventOutcome().toString())
                .replyChannel(event.getReplyTo())
                .build();
    }

    private byte[] createResponseEvent(AssessmentEventEntity event) throws JsonProcessingException {
        val responseEvent = Event.builder()
                .sagaId(event.getSagaId())
                .eventType(EventType.valueOf(event.getEventType()))
                .eventOutcome(EventOutcome.valueOf(event.getEventOutcome()))
                .eventPayload(event.getEventPayload()).build();
        return JsonUtil.getJsonBytesFromObject(responseEvent);
    }

}
