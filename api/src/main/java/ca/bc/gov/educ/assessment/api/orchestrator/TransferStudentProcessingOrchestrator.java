package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.service.v1.DOARReportService;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.assessment.api.constants.EventType.*;
import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.PROCESS_STUDENT_TRANSFER;
import static ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.STUDENT_TRANSFER_PROCESSING_TOPIC;

@Slf4j
@Component
public class TransferStudentProcessingOrchestrator extends BaseOrchestrator<TransferOnApprovalSagaData> {

    private final AssessmentStudentService assessmentStudentService;
    private final SagaService sagaService;
    private final DOARReportService doarReportService;

    protected TransferStudentProcessingOrchestrator(final SagaService sagaService,
                                                    final MessagePublisher messagePublisher,
                                                    final AssessmentStudentService assessmentStudentService, DOARReportService doarReportService) {
        super(sagaService, messagePublisher, TransferOnApprovalSagaData.class, PROCESS_STUDENT_TRANSFER.toString(), STUDENT_TRANSFER_PROCESSING_TOPIC.toString());
        this.assessmentStudentService = assessmentStudentService;
        this.sagaService = sagaService;
        this.doarReportService = doarReportService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
            .begin(PROCESS_STUDENT_TRANSFER_EVENT, this::processStudentTransfer)
            .step(PROCESS_STUDENT_TRANSFER_EVENT, STUDENT_TRANSFER_PROCESSED, CALCULATE_STUDENT_DOAR, this::createAndPopulateDOARCalculations)
            .end(CALCULATE_STUDENT_DOAR, STUDENT_DOAR_CALCULATED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processStudentTransfer(Event event, AssessmentSagaEntity saga, TransferOnApprovalSagaData transferOnApprovalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(PROCESS_STUDENT_TRANSFER_EVENT.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID studentId = UUID.fromString(transferOnApprovalSagaData.getStagedStudentAssessmentID());
        log.debug("Processing transfer for student: {}", studentId);
        log.info("Processing transfer for student: {}", studentId);

        // Transfer student data from staging to main tables
        transferStagedStudentToMainTables(studentId);

        // Mark the student as TRANSFERRED in the staging table (same transaction)
        int updatedCount = markStudentAsTransferredInTransaction(studentId);
        log.debug("Marked student {} as TRANSFERRED (updated: {} records)", studentId, updatedCount);
        log.info("Marked student {} as TRANSFERRED (updated: {} records)", studentId, updatedCount);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(PROCESS_STUDENT_TRANSFER_EVENT)
                .eventOutcome(STUDENT_TRANSFER_PROCESSED)
                .eventPayload(JsonUtil.getJsonStringFromObject(transferOnApprovalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step processStudentTransfer: {}", saga.getSagaId());
        log.info("Posted completion message for saga step processStudentTransfer: {}", saga.getSagaId());
    }

    protected void createAndPopulateDOARCalculations(Event event, AssessmentSagaEntity saga, TransferOnApprovalSagaData transferOnApprovalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(CALCULATE_STUDENT_DOAR.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        final Event.EventBuilder eventBuilder = Event.builder();
        eventBuilder.sagaId(saga.getSagaId()).eventType(CALCULATE_STUDENT_DOAR);

        doarReportService.createAndPopulateDOARSummaryCalculations(transferOnApprovalSagaData);

        eventBuilder.eventOutcome(STUDENT_DOAR_CALCULATED);
        val nextEvent = eventBuilder.build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
    }

    private int markStudentAsTransferredInTransaction(UUID studentId) {
        log.debug("Marking student {} as TRANSFERRED in current transaction", studentId);

        return assessmentStudentService.markStudentAsTransferredInCurrentTransaction(studentId);
    }

    private void transferStagedStudentToMainTables(UUID stagedStudentId) {
        StagedAssessmentStudentEntity stagedStudent = assessmentStudentService.getStagedStudentById(stagedStudentId);

        Optional<AssessmentStudentEntity> existingStudent = assessmentStudentService
            .getStudentByAssessmentIDAndStudentID(
                stagedStudent.getAssessmentEntity().getAssessmentID(),
                stagedStudent.getStudentID()
            );

        log.info("Transferring student {} for assessment {}", stagedStudent.getStudentID(), stagedStudent.getAssessmentEntity().getAssessmentID());
        log.info("existing student present: {}", existingStudent.isPresent());
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

        AssessmentStudentEntity savedStudent = assessmentStudentService.saveAssessmentStudentWithHistoryInCurrentTransaction(existingStudent);
        log.debug("Updated existing student {} to main student {}", stagedStudent.getAssessmentStudentID(), savedStudent.getAssessmentStudentID());
        log.info("Updated existing student {} to main student {}", stagedStudent.getAssessmentStudentID(), savedStudent.getAssessmentStudentID());
    }

    private void createNewStudentFromStaged(StagedAssessmentStudentEntity stagedStudent) {
        AssessmentStudentEntity mainStudent = createMainStudentFromStaged(stagedStudent);
        log.info("main student created from staged student {}", mainStudent.getAssessmentStudentID());

        AssessmentStudentEntity savedMainStudent = assessmentStudentService.saveAssessmentStudentWithHistoryInCurrentTransaction(mainStudent);

        log.debug("Created new student {} from staged student {}", savedMainStudent.getAssessmentStudentID(), stagedStudent.getAssessmentStudentID());
        log.info("Created new student {} from staged student {}", savedMainStudent.getAssessmentStudentID(), stagedStudent.getAssessmentStudentID());
    }

    private void updateMainStudentFromStaged(AssessmentStudentEntity existing, StagedAssessmentStudentEntity staged) {
        existing.setAssessmentFormID(staged.getAssessmentFormID());
        existing.setSchoolAtWriteSchoolID(staged.getSchoolAtWriteSchoolID());
        existing.setAssessmentCenterSchoolID(staged.getAssessmentCenterSchoolID());
        existing.setSchoolOfRecordSchoolID(staged.getSchoolOfRecordSchoolID());
        existing.setStudentID(staged.getStudentID());
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
                .studentStatusCode(StudentStatusCodes.ACTIVE.getCode())
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
    public void startStudentTransferProcessingSaga(TransferOnApprovalSagaData transferOnApprovalSagaData) throws JsonProcessingException {
        String payload = JsonUtil.getJsonStringFromObject(transferOnApprovalSagaData);
        AssessmentSagaEntity saga = sagaService.createSagaRecordInDB(
                this.getSagaName(),
                "ASSESSMENT-API",
                payload,
                null,
                UUID.fromString(transferOnApprovalSagaData.getStagedStudentAssessmentID())
        );
        this.startSaga(saga);
        log.debug("Started student transfer processing saga for student: {}", transferOnApprovalSagaData.getStagedStudentAssessmentID());
    }
}
