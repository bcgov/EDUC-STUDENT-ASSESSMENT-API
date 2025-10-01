package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.ComponentSubTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.ComponentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.LegacyComponentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentResultMapper;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResult;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import ca.bc.gov.educ.assessment.api.util.AssessmentUtil;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentAssessmentResultService {

    private final MessagePublisher messagePublisher;
    private final RestUtils restUtils;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final StagedStudentResultRepository stagedStudentResultRepository;
    private final StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentChoiceRepository assessmentChoiceRepository;
    private static final String EVENT_EMPTY_MSG = "Event String is empty, skipping the publish to topic :: {}";

    @Async("publisherExecutor")
    @Transactional
    public void prepareAndSendSdcStudentsForFurtherProcessing(final List <StagedStudentResultEntity> stagedResultEntities) {
        final List<StudentResultSagaData> resultSagaDatas = stagedResultEntities.stream()
                .map(el -> {
                    val studentResultSagaData = new StudentResultSagaData();
                    var school = this.restUtils.getSchoolByMincode(el.getMincode());
                    studentResultSagaData.setSchool(school.get());
                    studentResultSagaData.setStudentResult(AssessmentStudentResultMapper.mapper.toStructure(el));
                    return studentResultSagaData;
                }).toList();
        this.publishUnprocessedStudentRecordsForProcessing(resultSagaDatas);
    }

    public void publishUnprocessedStudentRecordsForProcessing(final List<StudentResultSagaData> studentResultSagaData) {
        studentResultSagaData.forEach(this::sendIndividualStudentAsMessageToTopic);
    }

    private void sendIndividualStudentAsMessageToTopic(final StudentResultSagaData studentResultSagaData) {
        final var eventPayload = JsonUtil.getJsonString(studentResultSagaData);
        if (eventPayload.isPresent()) {
            final Event event = Event.builder().eventType(EventType.READ_STUDENT_RESULT_FOR_PROCESSING).eventOutcome(EventOutcome.READ_STUDENT_RESULT_FOR_PROCESSING_SUCCESS).eventPayload(eventPayload.get()).stagedStudentResultID(String.valueOf(studentResultSagaData.getStudentResult().getStagedStudentResultID())).build();
            final var eventString = JsonUtil.getJsonString(event);
            if (eventString.isPresent()) {
                this.messagePublisher.dispatchMessage(TopicsEnum.READ_STUDENT_RESULT_RECORD.toString(), eventString.get().getBytes());
            } else {
                log.error(EVENT_EMPTY_MSG, studentResultSagaData);
            }
        } else {
            log.error(EVENT_EMPTY_MSG, studentResultSagaData);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processLoadedRecordsInBatchFile(StudentResultSagaData studentResultSagaData) {
        var studentResult = studentResultSagaData.getStudentResult();
        var assessmentEntity = assessmentRepository.findById(UUID.fromString(studentResult.getAssessmentID()))
                .orElseThrow(() -> new EntityNotFoundException(AssessmentEntity.class, "assessmentID", studentResult.getAssessmentID()));

        var formEntity = assessmentEntity.getAssessmentForms().stream()
                .filter(form -> Objects.equals(form.getAssessmentFormID(), UUID.fromString(studentResult.getAssessmentFormID())))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(AssessmentFormEntity.class, "assessmentFormID", studentResult.getAssessmentFormID()));

        var stagedStudentResult = stagedStudentResultRepository.findById(UUID.fromString(studentResult.getStagedStudentResultID()))
                .orElseThrow(() -> new EntityNotFoundException(StagedStudentResultEntity.class, "stagedStudentResultID", studentResult.getStagedStudentResultID()));

        var optStudent = restUtils.getStudentByPEN(UUID.randomUUID(), studentResult.getPen());
        var penMatchFound = optStudent.isPresent();
        StagedAssessmentStudentEntity stagedStudent;
        boolean isMergedRecord = false;
        if(penMatchFound) {
            Student studentApiStudent = optStudent.get();
            Student trueStudentApiStudentRecord = null;
            if(optStudent.get().getStatusCode().equalsIgnoreCase("M")) {
                List<Student> mergedStudent = restUtils.getStudents(UUID.randomUUID(), Set.of(optStudent.get().getTrueStudentID()));
                trueStudentApiStudentRecord = mergedStudent.getFirst();
                isMergedRecord = true;
            }

            var studentID = isMergedRecord ? trueStudentApiStudentRecord.getStudentID() : studentApiStudent.getStudentID();
            var studentApiRecord = isMergedRecord ? trueStudentApiStudentRecord : studentApiStudent;
            var existingStudentRegistrationOpt = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(UUID.fromString(studentResult.getAssessmentID()), UUID.fromString(studentID));
            var gradStudent = restUtils.getGradStudentRecordByStudentID(UUID.randomUUID(), UUID.fromString(studentID)).orElse(null);
            stagedStudent = existingStudentRegistrationOpt.isPresent() ?
                    createFromExistingStudentEntity(studentResult, gradStudent, existingStudentRegistrationOpt.get(), formEntity.getAssessmentFormID())
                    : createNewStagedAssessmentStudentEntity(studentResult, studentResultSagaData.getSchool(), studentApiRecord, gradStudent, formEntity.getAssessmentFormID(), assessmentEntity);
            stagedStudent.setIsPreRegistered(existingStudentRegistrationOpt.isPresent());
            if(isMergedRecord) {
                stagedStudent.setStagedAssessmentStudentStatus("MERGED");
                stagedStudent.setMergedPen(studentApiStudent.getPen());
            } else {
                stagedStudent.setStagedAssessmentStudentStatus("ACTIVE");
            }
        } else {
            stagedStudent = createNewStagedAssessmentStudentEntity(studentResult, studentResultSagaData.getSchool(), null, null, formEntity.getAssessmentFormID(), assessmentEntity);
            stagedStudent.setIsPreRegistered(false);
            stagedStudent.setStagedAssessmentStudentStatus("NOPENFOUND");
        }

        switch (LegacyComponentTypeCodes.findByValue(studentResult.getComponentType()).orElseThrow()) {
            case MUL_CHOICE -> addStagedStudentComponent(formEntity, stagedStudent, studentResult, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE);
            case OPEN_ENDED -> addStagedStudentComponent(formEntity, stagedStudent, studentResult, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE);
            case ORAL -> addStagedStudentComponent(formEntity, stagedStudent, studentResult, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.ORAL);
            case BOTH -> {
                addStagedStudentComponent(formEntity, stagedStudent, studentResult, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE);
                addStagedStudentComponent(formEntity, stagedStudent, studentResult, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE);
            }
        }

        var mcTotal = setStagedStudentTotals(stagedStudent, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE, formEntity);
        var oeTotal = setStagedStudentTotals(stagedStudent, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE, formEntity);
        var oralTotal = setStagedStudentTotals(stagedStudent, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.ORAL, formEntity);

        stagedStudent.setMcTotal(mcTotal);
        stagedStudent.setOeTotal(oeTotal.add(oralTotal));
        stagedStudent.setRawScore(mcTotal.add(oralTotal).add(oeTotal));

        stagedAssessmentStudentRepository.save(stagedStudent);

        stagedStudentResult.setStagedStudentResultStatus("COMPLETED");
        stagedStudentResult.setUpdateDate(LocalDateTime.now());
        stagedStudentResultRepository.save(stagedStudentResult);

    }

    private BigDecimal setStagedStudentTotals(StagedAssessmentStudentEntity stagedStudent, ComponentTypeCodes componentType, ComponentSubTypeCodes componentSubType, AssessmentFormEntity formEntity) {
        var component = formEntity.getAssessmentComponentEntities().stream()
                .filter(ac -> ac.getComponentTypeCode().equals(componentType.getCode()) && ac.getComponentSubTypeCode().equals(componentSubType.getCode()))
                .findFirst();
        if(component.isPresent()) {
            var componentEntity = stagedStudent.getStagedAssessmentStudentComponentEntities().stream()
                    .filter(comp -> Objects.equals(comp.getAssessmentComponentID(), component.get().getAssessmentComponentID()))
                    .findFirst();
            var totalStudentScore = componentEntity.map(stagedAssessmentStudentComponentEntity -> stagedAssessmentStudentComponentEntity.getStagedAssessmentStudentAnswerEntities().stream()
                    .map(StagedAssessmentStudentAnswerEntity::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)).orElse(BigDecimal.ZERO);

            var totalScale = component.get().getAssessmentQuestionEntities()
                    .stream()
                    .map(AssessmentQuestionEntity::getScaleFactor)
                    .reduce(0, Integer::sum);
            return (totalStudentScore.multiply(BigDecimal.valueOf(totalScale))).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal setAssessmentStudentTotals(AssessmentStudentEntity assessmentStudent, ComponentTypeCodes componentType, ComponentSubTypeCodes componentSubType, AssessmentFormEntity formEntity) {
        var component = formEntity.getAssessmentComponentEntities().stream()
                .filter(ac -> ac.getComponentTypeCode().equals(componentType.getCode()) && ac.getComponentSubTypeCode().equals(componentSubType.getCode()))
                .findFirst();
        if(component.isPresent()) {
            var componentEntity = assessmentStudent.getAssessmentStudentComponentEntities().stream()
                    .filter(comp -> Objects.equals(comp.getAssessmentComponentID(), component.get().getAssessmentComponentID()))
                    .findFirst();
            var totalStudentScore = componentEntity.map(stagedAssessmentStudentComponentEntity -> stagedAssessmentStudentComponentEntity.getAssessmentStudentAnswerEntities().stream()
                    .map(AssessmentStudentAnswerEntity::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)).orElse(BigDecimal.ZERO);

            var totalScale = component.get().getAssessmentQuestionEntities()
                    .stream()
                    .map(AssessmentQuestionEntity::getScaleFactor)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);
            return (totalStudentScore.multiply(BigDecimal.valueOf(totalScale))).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private StagedAssessmentStudentEntity createFromExistingStudentEntity(StudentResult studentResult, GradStudentRecord gradStudent, AssessmentStudentEntity existingStudent, UUID assessmentFormID) {
        StagedAssessmentStudentEntity stagedStudent = AssessmentStudentMapper.mapper.toStagingStudent(existingStudent);
        if(studentResult.getComponentType().equalsIgnoreCase("1") || studentResult.getComponentType().equalsIgnoreCase("3")) {
            stagedStudent.setIrtScore(studentResult.getIrtScore());
            stagedStudent.setMarkingSession(studentResult.getMarkingSession());
            stagedStudent.setAdaptedAssessmentCode(AssessmentUtil.getAdaptedAssessmentCode(studentResult.getAdaptedAssessmentCode()));
        }

        stagedStudent.setAssessmentFormID(assessmentFormID);
        stagedStudent.setProficiencyScore(studentResult.getProficiencyScore());
        stagedStudent.setProvincialSpecialCaseCode(studentResult.getProvincialSpecialCaseCode());
        stagedStudent.setSchoolAtWriteSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : stagedStudent.getSchoolOfRecordSchoolID());
        stagedStudent.setGradeAtRegistration(gradStudent != null ? gradStudent.getStudentGrade() : null);
        stagedStudent.setUpdateDate(LocalDateTime.now());
        stagedStudent.setUpdateUser(studentResult.getUpdateUser());

        return stagedStudent;
    }

    private StagedAssessmentStudentEntity createNewStagedAssessmentStudentEntity(StudentResult studentResult, SchoolTombstone school, Student studentApiStudent, GradStudentRecord gradStudent, UUID assessmentFormID, AssessmentEntity assessmentEntity) {
        StagedAssessmentStudentEntity stagedStudent = new StagedAssessmentStudentEntity();
        var noOfAttempts = studentApiStudent != null
                ? assessmentStudentRepository.findNumberOfAttemptsForStudent(UUID.fromString(studentApiStudent.getStudentID()), AssessmentUtil.getAssessmentTypeCodeList(assessmentEntity.getAssessmentTypeCode()))
                : 0;
        if(studentResult.getComponentType().equalsIgnoreCase("1") || studentResult.getComponentType().equalsIgnoreCase("3")) {
            stagedStudent.setIrtScore(studentResult.getIrtScore());
            stagedStudent.setMarkingSession(studentResult.getMarkingSession());
            stagedStudent.setAdaptedAssessmentCode(AssessmentUtil.getAdaptedAssessmentCode(studentResult.getAdaptedAssessmentCode()));
        }

        stagedStudent.setAssessmentEntity(assessmentEntity);
        stagedStudent.setAssessmentFormID(assessmentFormID);
        stagedStudent.setSchoolAtWriteSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : UUID.fromString(school.getSchoolId()));
        stagedStudent.setSchoolOfRecordSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : UUID.fromString(school.getSchoolId()));
        stagedStudent.setStudentID(studentApiStudent != null ? UUID.fromString(studentApiStudent.getStudentID()) : null);
        stagedStudent.setGivenName(studentApiStudent != null ? studentApiStudent.getLegalFirstName() : null);
        stagedStudent.setSurname(studentApiStudent != null ? studentApiStudent.getLegalLastName(): null);
        stagedStudent.setPen(studentApiStudent != null ? studentApiStudent.getPen(): studentResult.getPen());
        stagedStudent.setLocalID(studentApiStudent != null ? studentApiStudent.getLocalID() : null);
        stagedStudent.setGradeAtRegistration(gradStudent != null ? gradStudent.getStudentGrade() : null);
        stagedStudent.setProficiencyScore(studentResult.getProficiencyScore());
        stagedStudent.setProvincialSpecialCaseCode(studentResult.getProvincialSpecialCaseCode());
        stagedStudent.setNumberOfAttempts(noOfAttempts);
        stagedStudent.setCreateUser(studentResult.getCreateUser());
        stagedStudent.setCreateDate(LocalDateTime.now());
        stagedStudent.setUpdateUser(studentResult.getUpdateUser());
        stagedStudent.setUpdateDate(LocalDateTime.now());

        return stagedStudent;
    }

    private void addStagedStudentComponent(AssessmentFormEntity formEntity, StagedAssessmentStudentEntity assessmentStudent, StudentResult studentResult, ComponentTypeCodes componentType, ComponentSubTypeCodes componentSubType) {
        var studentComponent = new StagedAssessmentStudentComponentEntity();
        studentComponent.setStagedAssessmentStudentEntity(assessmentStudent);

        var component = formEntity.getAssessmentComponentEntities().stream()
                .filter(ac -> ac.getComponentTypeCode().equals(componentType.getCode()) && ac.getComponentSubTypeCode().equals(componentSubType.getCode()))
                .findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentComponentEntity.class, componentType.getCode()));

        studentComponent.setAssessmentComponentID(component.getAssessmentComponentID());
        studentComponent.setCreateUser(studentResult.getCreateUser());
        studentComponent.setCreateDate(LocalDateTime.now());
        studentComponent.setUpdateUser(studentResult.getUpdateUser());
        studentComponent.setUpdateDate(LocalDateTime.now());

        if(componentType == ComponentTypeCodes.MUL_CHOICE) {
            studentComponent.setChoicePath(studentResult.getChoicePath());
            var multiChoiceMarks = TransformUtil.splitStringEveryNChars(studentResult.getMcMarks(), 4);
            AtomicInteger questionCounter = new AtomicInteger(1);
            AtomicInteger itemCounter = new AtomicInteger(1);
            for(var multiChoiceMark: multiChoiceMarks){
                var answer = new StagedAssessmentStudentAnswerEntity();
                answer.setStagedAssessmentStudentComponentEntity(studentComponent);
                var question = component.getAssessmentQuestionEntities().stream()
                        .filter(q -> q.getQuestionNumber().equals(questionCounter.get()) && q.getItemNumber().equals(itemCounter.get()))
                        .findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentQuestionEntity.class, "questionNumber", questionCounter.toString()));
                questionCounter.getAndIncrement();
                itemCounter.getAndIncrement();
                if (StringUtils.isNotBlank(multiChoiceMark) && !multiChoiceMark.equalsIgnoreCase("9999")) {
                    answer.setAssessmentQuestionID(question.getAssessmentQuestionID());
                    answer.setScore(new BigDecimal(multiChoiceMark));
                    answer.setCreateUser(studentResult.getCreateUser());
                    answer.setCreateDate(LocalDateTime.now());
                    answer.setUpdateUser(studentResult.getUpdateUser());
                    answer.setUpdateDate(LocalDateTime.now());
                    studentComponent.getStagedAssessmentStudentAnswerEntities().add(answer);
                }
            }
        } else if(componentType == ComponentTypeCodes.OPEN_ENDED) {
            var openEndedMarks = TransformUtil.splitStringEveryNChars(studentResult.getOeMarks(), 4);
            int questionCounter = 1;
            AtomicInteger itemCounter = new AtomicInteger(1);
            int answerForChoiceCounter = 0;
            int choiceQuestionNumber = 0;
            for(var openEndedMark: openEndedMarks) {
                Optional<AssessmentQuestionEntity> question;
                var quesCount = answerForChoiceCounter != 0 ? choiceQuestionNumber : questionCounter++;
                question = component.getAssessmentQuestionEntities().stream()
                        .filter(q -> q.getQuestionNumber().equals(quesCount) && q.getItemNumber().equals(itemCounter.get()))
                        .findFirst();
                var optAssessmentChoice = assessmentChoiceRepository.findByItemNumberAndAssessmentComponentEntity_AssessmentComponentID(itemCounter.get(), component.getAssessmentComponentID());

                itemCounter.getAndIncrement();
                if (answerForChoiceCounter != 0) {
                    answerForChoiceCounter--;
                }

                if (StringUtils.isNotBlank(openEndedMark) && !openEndedMark.equalsIgnoreCase("9999")) {
                    if (question.isEmpty()) {
                        //It's a choice!
                        //Value in 4 chars is the question number
                        var questionNumber = getQuestionNumberFromString(openEndedMark);
                        //Pull the number of rows that have this question number in this component
                        answerForChoiceCounter = component.getAssessmentQuestionEntities().stream()
                                .filter(q -> q.getQuestionNumber().equals(questionNumber))
                                .toList().size();
                        //Based on number of rows returned, we know how many answers are coming
                        //Item numbers are sequential, while skipping the choice records
                        choiceQuestionNumber = questionNumber;

                        if(optAssessmentChoice.isPresent()) {
                            var choice = new StagedAssessmentStudentChoiceEntity();
                            choice.setStagedAssessmentStudentComponentEntity(studentComponent);
                            choice.setAssessmentChoiceEntity(optAssessmentChoice.get());
                            choice.setCreateUser(studentResult.getCreateUser());
                            choice.setCreateDate(LocalDateTime.now());
                            choice.setUpdateUser(studentResult.getUpdateUser());
                            choice.setUpdateDate(LocalDateTime.now());
                            studentComponent.getStagedAssessmentStudentChoiceEntities().add(choice);
                        }
                    } else {
                        var answer = new StagedAssessmentStudentAnswerEntity();
                        var studentChoice = studentComponent.getStagedAssessmentStudentChoiceEntities()
                                .stream()
                                .filter(studentChoiceEntity -> optAssessmentChoice.isPresent() && Objects.equals(studentChoiceEntity.getAssessmentChoiceEntity().getAssessmentChoiceID(), optAssessmentChoice.get().getAssessmentChoiceID()))
                                .findFirst().orElse(null);

                        if(studentChoice != null) {
                            answer.setAssessmentStudentChoiceEntity(studentChoice);
                        }
                        answer.setStagedAssessmentStudentComponentEntity(studentComponent);
                        answer.setAssessmentQuestionID(question.get().getAssessmentQuestionID());
                        answer.setScore(new BigDecimal(openEndedMark));
                        answer.setCreateUser(studentResult.getCreateUser());
                        answer.setCreateDate(LocalDateTime.now());
                        answer.setUpdateUser(studentResult.getUpdateUser());
                        answer.setUpdateDate(LocalDateTime.now());
                        studentComponent.getStagedAssessmentStudentAnswerEntities().add(answer);
                    }
                }
            }
        }
        assessmentStudent.getStagedAssessmentStudentComponentEntities().add(studentComponent);
    }

    public AssessmentStudentEntity createNewAssessmentStudentEntity(AssessmentResultDetails studentResult, Student studentApiStudent, GradStudentRecord gradStudent, UUID assessmentFormID, AssessmentEntity assessmentEntity) {
        var school = this.restUtils.getSchoolByMincode(studentResult.getMincode());
        AssessmentStudentEntity student = new AssessmentStudentEntity();
        var noOfAttempts = studentApiStudent != null
                ? assessmentStudentRepository.findNumberOfAttemptsForStudent(UUID.fromString(studentApiStudent.getStudentID()), AssessmentUtil.getAssessmentTypeCodeList(assessmentEntity.getAssessmentTypeCode()))
                : 0;
        if(studentResult.getComponentType().equalsIgnoreCase("1") || studentResult.getComponentType().equalsIgnoreCase("3")) {
            student.setIrtScore(studentResult.getIrtScore());
            student.setMarkingSession(studentResult.getMarkingSession());
            student.setAdaptedAssessmentCode(AssessmentUtil.getAdaptedAssessmentCode(studentResult.getAdaptedAssessmentIndicator()));
        }
        student.setAssessmentEntity(assessmentEntity);
        student.setAssessmentFormID(assessmentFormID);
        student.setSchoolAtWriteSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : UUID.fromString(school.get().getSchoolId()));
        student.setSchoolOfRecordSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : UUID.fromString(school.get().getSchoolId()));
        student.setStudentID(studentApiStudent != null ? UUID.fromString(studentApiStudent.getStudentID()) : null);
        student.setGivenName(studentApiStudent != null ? studentApiStudent.getLegalFirstName() : null);
        student.setSurname(studentApiStudent != null ? studentApiStudent.getLegalLastName(): null);
        student.setPen(studentApiStudent != null ? studentApiStudent.getPen(): studentResult.getPen());
        student.setLocalID(studentApiStudent != null ? studentApiStudent.getLocalID() : null);
        student.setGradeAtRegistration(gradStudent != null ? gradStudent.getStudentGrade() : null);
        student.setProficiencyScore(Integer.valueOf(studentResult.getProficiencyScore()));
        student.setProvincialSpecialCaseCode(studentResult.getSpecialCaseCode());
        student.setNumberOfAttempts(noOfAttempts);

        return student;
    }

    public void addAssessmentStudentComponent(AssessmentFormEntity formEntity, AssessmentStudentEntity assessmentStudent, AssessmentResultFileUpload fileUpload, AssessmentResultDetails studentResult, ComponentTypeCodes componentType, ComponentSubTypeCodes componentSubType) {
        var studentComponent = new AssessmentStudentComponentEntity();
        studentComponent.setAssessmentStudentEntity(assessmentStudent);

        var component = formEntity.getAssessmentComponentEntities().stream()
                .filter(ac -> ac.getComponentTypeCode().equals(componentType.getCode()) && ac.getComponentSubTypeCode().equals(componentSubType.getCode()))
                .findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentComponentEntity.class, componentType.getCode()));

        studentComponent.setAssessmentComponentID(component.getAssessmentComponentID());
        studentComponent.setCreateUser(fileUpload.getCreateUser());
        studentComponent.setCreateDate(LocalDateTime.now());
        studentComponent.setUpdateUser(fileUpload.getUpdateUser());
        studentComponent.setUpdateDate(LocalDateTime.now());

        if(componentType == ComponentTypeCodes.MUL_CHOICE) {
            studentComponent.setChoicePath(studentResult.getChoicePath());
            var multiChoiceMarks = TransformUtil.splitStringEveryNChars(studentResult.getMultiChoiceMarks(), 4);
            AtomicInteger questionCounter = new AtomicInteger(1);
            AtomicInteger itemCounter = new AtomicInteger(1);
            for(var multiChoiceMark: multiChoiceMarks){
                if (StringUtils.isNotBlank(multiChoiceMark) && !multiChoiceMark.equalsIgnoreCase("9999")) {
                    var answer = new AssessmentStudentAnswerEntity();
                    answer.setAssessmentStudentComponentEntity(studentComponent);
                    var question = component.getAssessmentQuestionEntities().stream()
                            .filter(q -> q.getQuestionNumber().equals(questionCounter.get()) && q.getItemNumber().equals(itemCounter.get()))
                            .findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentQuestionEntity.class, "questionNumber", questionCounter.toString()));
                    questionCounter.getAndIncrement();
                    itemCounter.getAndIncrement();
                    answer.setAssessmentQuestionID(question.getAssessmentQuestionID());
                    answer.setScore(new BigDecimal(multiChoiceMark));
                    answer.setCreateUser(fileUpload.getCreateUser());
                    answer.setCreateDate(LocalDateTime.now());
                    answer.setUpdateUser(fileUpload.getUpdateUser());
                    answer.setUpdateDate(LocalDateTime.now());
                    studentComponent.getAssessmentStudentAnswerEntities().add(answer);
                }
            }
        } else if(componentType == ComponentTypeCodes.OPEN_ENDED) {
            var openEndedMarks = TransformUtil.splitStringEveryNChars(studentResult.getOpenEndedMarks(), 4);
            int questionCounter = 1;
            AtomicInteger itemCounter = new AtomicInteger(1);
            int answerForChoiceCounter = 0;
            int choiceQuestionNumber = 0;
            for(var openEndedMark: openEndedMarks){
                if(StringUtils.isNotBlank(openEndedMark) && !openEndedMark.equalsIgnoreCase("9999")) {
                    Optional<AssessmentQuestionEntity> question;
                    var quesCount = answerForChoiceCounter != 0 ? choiceQuestionNumber : questionCounter++;
                    question = component.getAssessmentQuestionEntities().stream()
                            .filter(q -> q.getQuestionNumber().equals(quesCount) && q.getItemNumber().equals(itemCounter.get()))
                            .findFirst();
                    var optAssessmentChoice = assessmentChoiceRepository.findByItemNumberAndAssessmentComponentEntity_AssessmentComponentID(itemCounter.get(), component.getAssessmentComponentID());

                    itemCounter.getAndIncrement();
                    if(answerForChoiceCounter != 0) {
                        answerForChoiceCounter--;
                    }

                    if(question.isEmpty()){
                        //It's a choice!
                        //Value in 4 chars is the question number
                        var questionNumber = getQuestionNumberFromString(openEndedMark);
                        //Pull the number of rows that have this question number in this component
                        answerForChoiceCounter = component.getAssessmentQuestionEntities().stream()
                                .filter(q -> q.getQuestionNumber().equals(questionNumber))
                                .toList().size();
                        //Based on number of rows returned, we know how many answers are coming
                        //Item numbers are sequential, while skipping the choice records
                        choiceQuestionNumber = questionNumber;

                        if(optAssessmentChoice.isPresent()) {
                            var choice = new AssessmentStudentChoiceEntity();
                            choice.setAssessmentStudentComponentEntity(studentComponent);
                            choice.setAssessmentChoiceEntity(optAssessmentChoice.get());
                            choice.setCreateUser(fileUpload.getCreateUser());
                            choice.setCreateDate(LocalDateTime.now());
                            choice.setUpdateUser(fileUpload.getUpdateUser());
                            choice.setUpdateDate(LocalDateTime.now());
                            studentComponent.getAssessmentStudentChoiceEntities().add(choice);
                        }
                    } else {
                        var answer = new AssessmentStudentAnswerEntity();
                        answer.setAssessmentStudentComponentEntity(studentComponent);
                        answer.setAssessmentQuestionID(question.get().getAssessmentQuestionID());
                        answer.setScore(new BigDecimal(openEndedMark));
                        answer.setCreateUser(fileUpload.getCreateUser());
                        answer.setCreateDate(LocalDateTime.now());
                        answer.setUpdateUser(fileUpload.getUpdateUser());
                        answer.setUpdateDate(LocalDateTime.now());
                        studentComponent.getAssessmentStudentAnswerEntities().add(answer);
                    }
                }
            }
            assessmentStudent.getAssessmentStudentComponentEntities().add(studentComponent);
        }
    }



    private int getQuestionNumberFromString(String s) {
        try {
            double dValue = Double.parseDouble(s);
            return (int) Math.round(dValue);
        } catch(NumberFormatException e){
            log.error("Error parsing question number from {}: {}", s, e.getMessage());
            return 0;
        }
    }

}
