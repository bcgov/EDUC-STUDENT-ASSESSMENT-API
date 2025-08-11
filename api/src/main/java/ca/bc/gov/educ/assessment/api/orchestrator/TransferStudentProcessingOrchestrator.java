package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.assessment.api.constants.EventType.MARK_STAGED_STUDENTS_READY_FOR_TRANSFER;
import static ca.bc.gov.educ.assessment.api.constants.EventType.PROCESS_STUDENT_TRANSFER_EVENT;
import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.PROCESS_STUDENT_TRANSFER;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.STUDENT_TRANSFER_PROCESSING_TOPIC;

@Slf4j
@Component
public class TransferStudentProcessingOrchestrator extends BaseOrchestrator<String> {

    private final AssessmentStudentService assessmentStudentService;
    private final SagaService sagaService;

    protected TransferStudentProcessingOrchestrator(final SagaService sagaService,
                                                    final MessagePublisher messagePublisher,
                                                    final AssessmentStudentService assessmentStudentService) {
        super(sagaService, messagePublisher, String.class, PROCESS_STUDENT_TRANSFER.toString(), STUDENT_TRANSFER_PROCESSING_TOPIC.toString());
        this.assessmentStudentService = assessmentStudentService;
        this.sagaService = sagaService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
            .begin(PROCESS_STUDENT_TRANSFER_EVENT, this::processStudentTransfer)
            .step(PROCESS_STUDENT_TRANSFER_EVENT, STUDENT_TRANSFER_PROCESSED, MARK_STAGED_STUDENTS_READY_FOR_TRANSFER, this::markStudentAsTransferred)
            .end(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER, STAGED_STUDENTS_MARKED_READY_FOR_TRANSFER);
    }

    private void processStudentTransfer(Event event, AssessmentSagaEntity saga, String payload) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(PROCESS_STUDENT_TRANSFER_EVENT.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID studentId = UUID.fromString(payload);
        log.debug("Processing transfer for student: {}", studentId);

        // Transfer student data from staging to main tables
        transferStagedStudentToMainTables(studentId);

        log.debug("Successfully processed transfer for student: {}", studentId);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(PROCESS_STUDENT_TRANSFER_EVENT)
                .eventOutcome(STUDENT_TRANSFER_PROCESSED)
                .eventPayload(JsonUtil.getJsonStringFromObject("Student " + studentId + " transfer completed successfully"))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step processStudentTransfer: {}", saga.getSagaId());
    }

    private void markStudentAsTransferred(Event event, AssessmentSagaEntity saga, String payload) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID studentId = UUID.fromString(payload);
        log.debug("Marking student {} as TRANSFERRED", studentId);

        // Mark the student as TRANSFERRED in the staging table
        int updatedCount = assessmentStudentService.markStudentAsTransferred(studentId);
        log.debug("Marked student {} as TRANSFERRED (updated: {} records)", studentId, updatedCount);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER)
                .eventOutcome(STAGED_STUDENTS_MARKED_READY_FOR_TRANSFER)
                .eventPayload(JsonUtil.getJsonStringFromObject("Student " + studentId + " marked as TRANSFERRED successfully"))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step markStudentAsTransferred: {}", saga.getSagaId());
    }

    private void transferStagedStudentToMainTables(UUID stagedStudentId) {
        StagedAssessmentStudentEntity stagedStudent = assessmentStudentService.getStagedStudentById(stagedStudentId);

        Optional<AssessmentStudentEntity> existingStudent = assessmentStudentService
            .getStudentByAssessmentIDAndStudentID(
                stagedStudent.getAssessmentEntity().getAssessmentID(),
                stagedStudent.getStudentID()
            );

        if (existingStudent.isPresent()) {
            log.debug("Updating existing student {} for assessment {}", stagedStudent.getStudentID(), stagedStudent.getAssessmentEntity().getAssessmentID());
            updateExistingStudentFromStaged(existingStudent.get(), stagedStudent);
        } else {
            log.debug("Creating new student {} for assessment {}", stagedStudent.getStudentID(), stagedStudent.getAssessmentEntity().getAssessmentID());
            createNewStudentFromStaged(stagedStudent);
        }
    }

    private void updateExistingStudentFromStaged(AssessmentStudentEntity existingStudent, StagedAssessmentStudentEntity stagedStudent) {
        updateMainStudentFromStaged(existingStudent, stagedStudent);

        // Clear existing components and answers to replace with new data
        existingStudent.getAssessmentStudentComponentEntities().clear();

        // Transfer new components and answers from staging
        Set<AssessmentStudentComponentEntity> newComponents = new HashSet<>();
        for (StagedAssessmentStudentComponentEntity stagedComponent : stagedStudent.getStagedAssessmentStudentComponentEntities()) {
            AssessmentStudentComponentEntity mainComponent = createMainComponentFromStaged(stagedComponent, existingStudent);
            newComponents.add(mainComponent);
        }
        existingStudent.setAssessmentStudentComponentEntities(newComponents);

        AssessmentStudentEntity savedStudent = assessmentStudentService.saveAssessmentStudentWithHistory(existingStudent);
        log.debug("Updated existing student {} to main student {}", stagedStudent.getAssessmentStudentID(), savedStudent.getAssessmentStudentID());
    }

    private void createNewStudentFromStaged(StagedAssessmentStudentEntity stagedStudent) {
        AssessmentStudentEntity mainStudent = createMainStudentFromStaged(stagedStudent);

        AssessmentStudentEntity savedMainStudent = assessmentStudentService.createStudentWithoutValidation(mainStudent);

        log.debug("Created new student {} from staged student {}", savedMainStudent.getAssessmentStudentID(), stagedStudent.getAssessmentStudentID());
    }

    private void updateMainStudentFromStaged(AssessmentStudentEntity existing, StagedAssessmentStudentEntity staged) {
        existing.setAssessmentFormID(staged.getAssessmentFormID());
        existing.setSchoolAtWriteSchoolID(staged.getSchoolAtWriteSchoolID());
        existing.setAssessmentCenterSchoolID(staged.getAssessmentCenterSchoolID());
        existing.setSchoolOfRecordSchoolID(staged.getSchoolOfRecordSchoolID());
        existing.setGivenName(staged.getGivenName());
        existing.setSurname(staged.getSurname());
        existing.setPen(staged.getPen());
        existing.setLocalID(staged.getLocalID());
        existing.setGradeAtRegistration(staged.getGradeAtRegistration());
        existing.setLocalAssessmentID(staged.getLocalAssessmentID());
        existing.setProficiencyScore(staged.getProficiencyScore());
        existing.setProvincialSpecialCaseCode(staged.getProvincialSpecialCaseCode());
        existing.setNumberOfAttempts(staged.getNumberOfAttempts());
        existing.setRawScore(staged.getRawScore());
        existing.setMcTotal(staged.getMcTotal());
        existing.setOeTotal(staged.getOeTotal());
        existing.setAdaptedAssessmentCode(staged.getAdaptedAssessmentCode());
        existing.setIrtScore(staged.getIrtScore());
        existing.setMarkingSession(staged.getMarkingSession());
        existing.setUpdateUser(staged.getUpdateUser());
        existing.setUpdateDate(LocalDateTime.now());
    }

    private AssessmentStudentEntity createMainStudentFromStaged(StagedAssessmentStudentEntity staged) {
        AssessmentStudentEntity main = AssessmentStudentEntity.builder()
                .assessmentEntity(staged.getAssessmentEntity())
                .assessmentFormID(staged.getAssessmentFormID())
                .schoolAtWriteSchoolID(staged.getSchoolAtWriteSchoolID())
                .assessmentCenterSchoolID(staged.getAssessmentCenterSchoolID())
                .schoolOfRecordSchoolID(staged.getSchoolOfRecordSchoolID())
                .studentID(staged.getStudentID())
                .studentStatus(staged.getStagedAssessmentStudentStatus()) // do we want to do this?
                .givenName(staged.getGivenName())
                .surname(staged.getSurname())
                .pen(staged.getPen())
                .localID(staged.getLocalID())
                .gradeAtRegistration(staged.getGradeAtRegistration())
                .localAssessmentID(staged.getLocalAssessmentID())
                .proficiencyScore(staged.getProficiencyScore())
                .provincialSpecialCaseCode(staged.getProvincialSpecialCaseCode())
                .numberOfAttempts(staged.getNumberOfAttempts())
                .rawScore(staged.getRawScore())
                .mcTotal(staged.getMcTotal())
                .oeTotal(staged.getOeTotal())
                .adaptedAssessmentCode(staged.getAdaptedAssessmentCode())
                .irtScore(staged.getIrtScore())
                .markingSession(staged.getMarkingSession())
                .createUser(staged.getCreateUser())
                .createDate(staged.getCreateDate())
                .updateUser(staged.getUpdateUser())
                .updateDate(staged.getUpdateDate())
                .build();

        // Transfer components and answers
        Set<AssessmentStudentComponentEntity> mainComponents = new HashSet<>();
        for (StagedAssessmentStudentComponentEntity stagedComponent : staged.getStagedAssessmentStudentComponentEntities()) {
            AssessmentStudentComponentEntity mainComponent = createMainComponentFromStaged(stagedComponent, main);
            mainComponents.add(mainComponent);
        }
        main.setAssessmentStudentComponentEntities(mainComponents);

        return main;
    }

    private AssessmentStudentComponentEntity createMainComponentFromStaged(StagedAssessmentStudentComponentEntity staged, AssessmentStudentEntity mainStudent) {
        AssessmentStudentComponentEntity main = AssessmentStudentComponentEntity.builder()
                .assessmentStudentEntity(mainStudent)
                .assessmentFormID(null) // this is missing on staged
                .assessmentComponentID(staged.getAssessmentComponentID())
                .choicePath(staged.getChoicePath())
                .createUser(staged.getCreateUser())
                .createDate(staged.getCreateDate())
                .updateUser(staged.getUpdateUser())
                .updateDate(staged.getUpdateDate())
                .build();

        // Transfer answers
        Set<AssessmentStudentAnswerEntity> mainAnswers = new HashSet<>();
        for (StagedAssessmentStudentAnswerEntity stagedAnswer : staged.getStagedAssessmentStudentAnswerEntities()) {
            AssessmentStudentAnswerEntity mainAnswer = createMainAnswerFromStaged(stagedAnswer, main);
            mainAnswers.add(mainAnswer);
        }
        main.setAssessmentStudentAnswerEntities(mainAnswers);

        return main;
    }

    private AssessmentStudentAnswerEntity createMainAnswerFromStaged(StagedAssessmentStudentAnswerEntity staged, AssessmentStudentComponentEntity mainComponent) {
        return AssessmentStudentAnswerEntity.builder()
                .assessmentStudentComponentEntity(mainComponent)
                .assessmentStudentChoiceID(staged.getAssessmentStudentChoiceID())
                .assessmentQuestionID(staged.getAssessmentQuestionID())
                .score(staged.getScore())
                .createUser(staged.getCreateUser())
                .createDate(staged.getCreateDate())
                .updateUser(staged.getUpdateUser())
                .updateDate(staged.getUpdateDate())
                .build();
    }
    public void startStudentTransferProcessingSaga(UUID stagedStudentResultID) {
        String payload = stagedStudentResultID.toString();
        AssessmentSagaEntity saga = sagaService.createSagaRecordInDB(
                this.getSagaName(),
                "ASSESSMENT-API",
                payload,
                null,
                stagedStudentResultID
        );
        this.startSaga(saga);
        log.debug("Started student transfer processing saga for student: {}", stagedStudentResultID);
    }
}
