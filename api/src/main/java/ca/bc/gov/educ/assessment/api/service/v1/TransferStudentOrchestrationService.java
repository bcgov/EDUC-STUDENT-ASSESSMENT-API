package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentResultMapper;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.nimbusds.jose.util.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class TransferStudentOrchestrationService {

    protected final SagaService sagaService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentSessionRepository assessmentSessionRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentEventRepository assessmentEventRepository;
    private final MessagePublisher messagePublisher;
    private static final String EVENT_EMPTY_MSG = "Event String is empty, skipping the publish to topic :: {}";

    public TransferStudentOrchestrationService(AssessmentStudentService assessmentStudentService, SagaService sagaService, AssessmentSessionRepository assessmentSessionRepository, AssessmentEventRepository assessmentEventRepository, MessagePublisher messagePublisher) {
        this.assessmentStudentService = assessmentStudentService;
        this.sagaService = sagaService;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentEventRepository = assessmentEventRepository;
        this.messagePublisher = messagePublisher;
    }

    @Async("publisherExecutor")
    @Transactional
    public void prepareAndSendStudentsForFurtherProcessing(final List <StagedAssessmentStudentEntity> transferStudents) {
        final List<TransferOnApprovalSagaData> transferSaga = transferStudents.stream()
                .map(el -> {
                    var transferOnApprovalStudent = TransferOnApprovalSagaData.builder()
                            .stagedStudentAssessmentID(String.valueOf(el.getAssessmentStudentID()))
                            .studentID(String.valueOf(el.getStudentID()))
                            .assessmentID(String.valueOf(el.getAssessmentEntity().getAssessmentID()))
                            .build();
                    return transferOnApprovalStudent;
                }).toList();
        this.publishUnprocessedStudentRecordsForProcessing(transferSaga);
    }

    public void publishUnprocessedStudentRecordsForProcessing(final List<TransferOnApprovalSagaData> transferSaga) {
        transferSaga.forEach(this::sendIndividualStudentAsMessageToTopic);
    }

    private void sendIndividualStudentAsMessageToTopic(final TransferOnApprovalSagaData transferSaga) {
        final var eventPayload = JsonUtil.getJsonString(transferSaga);
        if (eventPayload.isPresent()) {
            final Event event = Event.builder().eventType(EventType.TRANSFER_STUDENT_RESULT).eventOutcome(EventOutcome.READ_TRANSFER_STUDENT_RESULT_SUCCESS).eventPayload(eventPayload.get())
                    .assessmentStudentID(String.valueOf(transferSaga.getStagedStudentAssessmentID()))
                    .assessmentID(String.valueOf(transferSaga.getAssessmentID()))
                    .build();
            final var eventString = JsonUtil.getJsonString(event);
            if (eventString.isPresent()) {
                this.messagePublisher.dispatchMessage(TopicsEnum.READ_TRANSFER_STUDENT_TOPIC.toString(), eventString.get().getBytes());
            } else {
                log.error(EVENT_EMPTY_MSG, transferSaga);
            }
        } else {
            log.error(EVENT_EMPTY_MSG, transferSaga);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<List<AssessmentEventEntity>, List<UUID>> getStudentRegistrationEvents(UUID studentID) {
        var events = List.of(assessmentStudentService.generateStudentUpdatedEvent(studentID.toString()));
        assessmentEventRepository.saveAll(events);
        return Pair.of(events, List.of(studentID));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transferStagedStudentToMainTables(UUID stagedStudentId) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteFromStagingTable(UUID stagedAssessmentStudentId) {
        assessmentStudentService.deleteStagedStudent(stagedAssessmentStudentId);
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
    }

    private void createNewStudentFromStaged(StagedAssessmentStudentEntity stagedStudent) {
        AssessmentStudentEntity mainStudent = createMainStudentFromStaged(stagedStudent);

        AssessmentStudentEntity savedMainStudent = assessmentStudentService.saveAssessmentStudentWithHistoryInCurrentTransaction(mainStudent);

        log.debug("Created new student {} from staged student {}", savedMainStudent.getAssessmentStudentID(), stagedStudent.getAssessmentStudentID());
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
                .createDate(LocalDateTime.now())
                .updateUser(staged.getUpdateUser())
                .updateDate(LocalDateTime.now())
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
                .createDate(LocalDateTime.now())
                .updateUser(staged.getUpdateUser())
                .updateDate(LocalDateTime.now())
                .build();

        // Transfer answers
        Set<AssessmentStudentAnswerEntity> mainAnswers = new HashSet<>();
        for (StagedAssessmentStudentAnswerEntity stagedAnswer : staged.getStagedAssessmentStudentAnswerEntities()) {
            AssessmentStudentAnswerEntity mainAnswer = createMainAnswerFromStaged(stagedAnswer, main);
            mainAnswers.add(mainAnswer);
        }

        //Transfer choices
        Set<AssessmentStudentChoiceEntity> choices = new HashSet<>();
        for (StagedAssessmentStudentChoiceEntity choice : staged.getStagedAssessmentStudentChoiceEntities()) {
            AssessmentStudentChoiceEntity choiceEntity = createMainChoiceFromStaged(choice, main);
            choices.add(choiceEntity);
        }

        main.setAssessmentStudentAnswerEntities(mainAnswers);
        main.setAssessmentStudentChoiceEntities(choices);
        return main;
    }

    private AssessmentStudentAnswerEntity createMainAnswerFromStaged(StagedAssessmentStudentAnswerEntity staged, AssessmentStudentComponentEntity mainComponent) {
        return AssessmentStudentAnswerEntity.builder()
                .assessmentStudentComponentEntity(mainComponent)
                .assessmentQuestionID(staged.getAssessmentQuestionID())
                .score(staged.getScore())
                .createUser(staged.getCreateUser())
                .createDate(LocalDateTime.now())
                .updateUser(staged.getUpdateUser())
                .updateDate(LocalDateTime.now())
                .build();
    }

    private AssessmentStudentChoiceEntity createMainChoiceFromStaged(StagedAssessmentStudentChoiceEntity staged, AssessmentStudentComponentEntity mainComponent) {
        var choice =  AssessmentStudentChoiceEntity.builder()
                .assessmentStudentComponentEntity(mainComponent)
                .assessmentChoiceEntity(staged.getAssessmentChoiceEntity())
                .createUser(staged.getCreateUser())
                .createDate(LocalDateTime.now())
                .updateUser(staged.getUpdateUser())
                .updateDate(LocalDateTime.now())
                .build();

        staged.getStagedAssessmentStudentChoiceQuestionSetEntities().forEach(stagedAssessmentStudentChoiceQuestionSetEntity -> {
            var questionSet  = new AssessmentStudentChoiceQuestionSetEntity();
            questionSet.setAssessmentStudentChoiceEntity(choice);
            questionSet.setAssessmentQuestionID(stagedAssessmentStudentChoiceQuestionSetEntity.getAssessmentQuestionID());
            questionSet.setCreateUser(stagedAssessmentStudentChoiceQuestionSetEntity.getCreateUser());
            questionSet.setCreateDate(LocalDateTime.now());
            questionSet.setUpdateUser(stagedAssessmentStudentChoiceQuestionSetEntity.getUpdateUser());
            questionSet.setUpdateDate(LocalDateTime.now());

            choice.getAssessmentStudentChoiceQuestionSetEntities().add(questionSet);
        });

        return choice;
    }
}
