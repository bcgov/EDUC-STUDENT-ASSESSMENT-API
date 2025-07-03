package ca.bc.gov.educ.assessment.api.batch.service;

import ca.bc.gov.educ.assessment.api.batch.exception.ResultsFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultFile;
import ca.bc.gov.educ.assessment.api.constants.v1.ComponentSubTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.ComponentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.LegacyComponentTypeCodes;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.util.PenUtil;
import ca.bc.gov.educ.assessment.api.util.TransformUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentResultsBatchFile.*;
import static ca.bc.gov.educ.assessment.api.batch.exception.ResultFileError.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentResultService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    private final AssessmentFormRepository assessmentFormRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentStudentService assessmentStudentService;
    private final CodeTableService codeTableService;
    private final String[] validChoicePaths = {"I", "E"};
    private final String[] validSpecialCaseCodes = {"A", "Q", "X"};
    private final Pattern pattern = Pattern.compile("^[0-9.]*$");

    public static final String LOAD_FAIL = "LOADFAIL";
    private final RestUtils restUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void populateBatchFileAndLoadData(String correlationID, DataSet ds, UUID sessionID, AssessmentResultFileUpload fileUpload) throws ResultsFileUnProcessableException {
        val batchFile = new AssessmentResultFile();

        AssessmentSessionEntity validSession =
                assessmentSessionRepository.findById(sessionID)
                        .orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_INCOMING_REQUEST_SESSION, correlationID, LOAD_FAIL));
        populateAssessmentResultsFile(ds, batchFile, correlationID);
        processLoadedRecordsInBatchFile(correlationID, batchFile, validSession, fileUpload);
    }

    private void populateAssessmentResultsFile(final DataSet ds, final AssessmentResultFile batchFile, final String guid) throws ResultsFileUnProcessableException {
        int index = 0;
        while (ds.next()) {
            batchFile.getAssessmentResultData().add(getAssessmentResultDetailRecordFromFile(ds, guid, Integer.toString(index)));
            index++;
        }
    }

    private void processLoadedRecordsInBatchFile(@NonNull final String correlationID, @NonNull final AssessmentResultFile batchFile, AssessmentSessionEntity validSession, AssessmentResultFileUpload fileUpload) throws ResultsFileUnProcessableException {
        var typeCode = batchFile.getAssessmentResultData().getFirst().getAssessmentCode();
        assessmentTypeCodeRepository.findByAssessmentTypeCode(typeCode).orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, correlationID, LOAD_FAIL));

        var assessmentEntity = assessmentRepository.findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(validSession.getSessionID(), typeCode)
                .orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, correlationID, LOAD_FAIL));

        for(val studentResult : batchFile.getAssessmentResultData()) {
            Student studentApiStudent = restUtils.getStudentByPEN(UUID.randomUUID(), studentResult.getPen()).orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_PEN, correlationID, LOAD_FAIL));
            StagedAssessmentStudentEntity stagedStudent;
            var existingStudentRegistrationOpt = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(studentApiStudent.getStudentID()));
            var formEntity = assessmentFormRepository.findByAssessmentEntity_AssessmentIDAndFormCode(assessmentEntity.getAssessmentID(), studentResult.getFormCode()).orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_FORM_CODE, correlationID, LOAD_FAIL));
            var gradStudent = restUtils.getGradStudentRecordByStudentID(UUID.randomUUID(), UUID.fromString(studentApiStudent.getStudentID()));

            if (existingStudentRegistrationOpt.isPresent()) {
                stagedStudent = AssessmentStudentMapper.mapper.toStagingStudent(existingStudentRegistrationOpt.get());
                stagedStudent.setIrtScore(studentResult.getIrtScore());
                stagedStudent.setAssessmentFormID(formEntity.getAssessmentFormID());
                stagedStudent.setProficiencyScore(Integer.parseInt(studentResult.getProficiencyScore()));
                stagedStudent.setProvincialSpecialCaseCode(studentResult.getSpecialCaseCode());
                stagedStudent.setAdaptedAssessmentCode(studentResult.getAdaptedAssessmentIndicator());
                stagedStudent.setMarkingSession(studentResult.getMarkingSession());
                stagedStudent.setSchoolAtWriteSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : stagedStudent.getSchoolOfRecordSchoolID());
//                stagedStudent.setMcTotal();
//                stagedStudent.setOeTotal();
//                stagedStudent.setRawScore();
                stagedStudent.setUpdateDate(LocalDateTime.now());
                stagedStudent.setUpdateUser(fileUpload.getUpdateUser());
            } else {
                stagedStudent = new StagedAssessmentStudentEntity();
                var school = restUtils.getSchoolByMincode(studentResult.getMincode()).orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_MINCODE, correlationID, LOAD_FAIL));

                stagedStudent.setAssessmentEntity(assessmentEntity);
                stagedStudent.setAssessmentFormID(formEntity.getAssessmentFormID());
                stagedStudent.setSchoolAtWriteSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : UUID.fromString(school.getSchoolId()));
                stagedStudent.setSchoolOfRecordSchoolID(gradStudent != null ? UUID.fromString(gradStudent.getSchoolOfRecordId()) : UUID.fromString(school.getSchoolId()));
                stagedStudent.setStudentID(UUID.fromString(studentApiStudent.getStudentID()));
                stagedStudent.setGivenName(studentApiStudent.getLegalFirstName());
                stagedStudent.setSurname(studentApiStudent.getLegalLastName());
                stagedStudent.setPen(studentApiStudent.getPen());
                stagedStudent.setLocalID(studentApiStudent.getLocalID());
                stagedStudent.setProficiencyScore(Integer.parseInt(studentResult.getProficiencyScore()));
                stagedStudent.setProvincialSpecialCaseCode(studentResult.getSpecialCaseCode());
                stagedStudent.setNumberOfAttempts(Integer.parseInt(assessmentStudentService.getNumberOfAttempts(assessmentEntity.getAssessmentID().toString(), UUID.fromString(studentApiStudent.getStudentID()))));
                stagedStudent.setAdaptedAssessmentCode(studentResult.getAdaptedAssessmentIndicator());
                stagedStudent.setIrtScore(studentResult.getIrtScore());
//                stagedStudent.setMcTotal();
//                stagedStudent.setOeTotal();
//                stagedStudent.setRawScore();
                stagedStudent.setMarkingSession(studentResult.getMarkingSession());
                stagedStudent.setCreateUser(fileUpload.getCreateUser());
                stagedStudent.setCreateDate(LocalDateTime.now());
                stagedStudent.setUpdateUser(fileUpload.getUpdateUser());
                stagedStudent.setUpdateDate(LocalDateTime.now());
            }

            switch (LegacyComponentTypeCodes.findByValue(studentResult.getComponentType()).orElseThrow()) {
                case MUL_CHOICE -> addStudentComponent(formEntity, stagedStudent, studentResult, fileUpload, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE, correlationID);
                case OPEN_ENDED -> addStudentComponent(formEntity, stagedStudent, studentResult, fileUpload, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE, correlationID);
                case ORAL -> addStudentComponent(formEntity, stagedStudent, studentResult, fileUpload, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.ORAL, correlationID);
                case BOTH -> {
                    addStudentComponent(formEntity, stagedStudent, studentResult, fileUpload, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE, correlationID);
                    addStudentComponent(formEntity, stagedStudent, studentResult, fileUpload, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE, correlationID);
                }
            }
            stagedAssessmentStudentRepository.save(stagedStudent);
       }
    }

    private void addStudentComponent(AssessmentFormEntity formEntity, StagedAssessmentStudentEntity assessmentStudent, AssessmentResultDetails studentResult, AssessmentResultFileUpload fileUpload, ComponentTypeCodes componentType, ComponentSubTypeCodes componentSubType, String correlationID) throws ResultsFileUnProcessableException {
        var studentComponent = new StagedAssessmentStudentComponentEntity();
        studentComponent.setStagedAssessmentStudentEntity(assessmentStudent);
        var component = assessmentComponentRepository.findByAssessmentFormEntity_AssessmentFormIDAndComponentTypeCodeAndComponentSubTypeCode(formEntity.getAssessmentFormID(), componentType.getCode(), componentSubType.getCode()).orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_COMPONENT, correlationID, LOAD_FAIL));
        studentComponent.setAssessmentComponentID(component.getAssessmentComponentID());
        studentComponent.setCreateUser(fileUpload.getCreateUser());
        studentComponent.setCreateDate(LocalDateTime.now());
        studentComponent.setUpdateUser(fileUpload.getUpdateUser());
        studentComponent.setUpdateDate(LocalDateTime.now());

        if(componentType == ComponentTypeCodes.MUL_CHOICE) {
            studentComponent.setChoicePath(studentResult.getChoicePath());
            var multiChoiceMarks = TransformUtil.splitStringEveryNChars(studentResult.getMultiChoiceMarks(), 4);
            int questionCounter = 1;
            int itemCounter = 1;
            for(var multiChoiceMark: multiChoiceMarks){
                var answer = new StagedAssessmentStudentAnswerEntity();
                answer.setStagedAssessmentStudentComponentEntity(studentComponent);
                var question = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentIDAndQuestionNumberAndItemNumber(component.getAssessmentComponentID(), questionCounter++, itemCounter++).orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_QUESTION, correlationID, LOAD_FAIL));
                answer.setAssessmentQuestionID(question.getAssessmentQuestionID());
                answer.setScore(new BigDecimal(multiChoiceMark));
                answer.setCreateUser(fileUpload.getCreateUser());
                answer.setCreateDate(LocalDateTime.now());
                answer.setUpdateUser(fileUpload.getUpdateUser());
                answer.setUpdateDate(LocalDateTime.now());
                studentComponent.getStagedAssessmentStudentAnswerEntities().add(answer);
            }
        }else if(componentType == ComponentTypeCodes.OPEN_ENDED) {
            var openEndedMarks = TransformUtil.splitStringEveryNChars(studentResult.getOpenEndedMarks(), 4);
            int questionCounter = 1;
            int itemCounter = 1;
            int answerForChoiceCounter = 0;
            int choiceQuestionNumber = 0;
            for(var openEndedMark: openEndedMarks){
                Optional<AssessmentQuestionEntity> question;
                question = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentIDAndQuestionNumberAndItemNumber(component.getAssessmentComponentID(), answerForChoiceCounter != 0 ? choiceQuestionNumber : questionCounter++, itemCounter++);

                if(answerForChoiceCounter != 0) {
                    answerForChoiceCounter--;
                }

                if(question.isEmpty()){
                    //It's a choice!
                    //Value in 4 chars is the question number
                    var questionNumber = getQuestionNumberFromString(openEndedMark, correlationID);
                    //Pull the number of rows that have this question number in this component
                    answerForChoiceCounter = assessmentQuestionRepository.countAllByAssessmentComponentEntity_AssessmentComponentIDAndQuestionNumber(component.getAssessmentComponentID(), questionNumber);
                    //Based on number of rows returned, we know how many answers are coming
                    //Item numbers are sequential, while skipping the choice records
                    choiceQuestionNumber = questionNumber;
                }else {
                    var answer = new StagedAssessmentStudentAnswerEntity();
                    answer.setStagedAssessmentStudentComponentEntity(studentComponent);
                    answer.setAssessmentQuestionID(question.get().getAssessmentQuestionID());
                    answer.setScore(new BigDecimal(openEndedMark));
                    answer.setCreateUser(fileUpload.getCreateUser());
                    answer.setCreateDate(LocalDateTime.now());
                    answer.setUpdateUser(fileUpload.getUpdateUser());
                    answer.setUpdateDate(LocalDateTime.now());
                    studentComponent.getStagedAssessmentStudentAnswerEntities().add(answer);
                }
            }
        }

        assessmentStudent.getStagedAssessmentStudentComponentEntities().add(studentComponent);
    }

    private int getQuestionNumberFromString(String s, String correlationID) throws ResultsFileUnProcessableException {
        try {
            double dValue = Double.parseDouble(s);
            return (int) Math.round(dValue);
        }catch(NumberFormatException e){
            throw new ResultsFileUnProcessableException(INVALID_MINCODE, correlationID, LOAD_FAIL);
        }
    }

    private AssessmentResultDetails getAssessmentResultDetailRecordFromFile(final DataSet ds, final String guid, final String lineNumber) throws ResultsFileUnProcessableException {
        final var txID = StringMapper.trimAndUppercase(ds.getString(TX_ID.getName()));
        if (StringUtils.isBlank(txID) || !txID.equalsIgnoreCase("A01")) {
            throw new ResultsFileUnProcessableException(INVALID_TXID, guid, lineNumber);
        }

        final var componentType = StringMapper.trimAndUppercase(ds.getString(COMPONENT_TYPE.getName()));
        if(StringUtils.isNotBlank(componentType) && LegacyComponentTypeCodes.findByValue(componentType).isEmpty()) {
            throw new ResultsFileUnProcessableException(INVALID_COMPONENT_TYPE_CODE, guid, lineNumber);
        }

        final var specialCaseCode = StringMapper.trimAndUppercase(ds.getString(SPECIAL_CASE_CODE.getName()));
        if(StringUtils.isNotBlank(specialCaseCode) && Arrays.stream(validSpecialCaseCodes).noneMatch(specialCaseCode::equalsIgnoreCase)) {
            throw new ResultsFileUnProcessableException(INVALID_SPECIAL_CASE_CODE, guid, lineNumber);
        }

        final var adaptedAssessmentIndicator = StringMapper.trimAndUppercase(ds.getString(ADAPTED_ASSESSMENT_INDICATOR.getName()));
        if(StringUtils.isNotBlank(adaptedAssessmentIndicator) && codeTableService.getAdaptedAssessmentIndicatorCodes().stream().noneMatch(code -> code.getLegacyCode().equalsIgnoreCase(adaptedAssessmentIndicator))) {
            throw new ResultsFileUnProcessableException(INVALID_ADAPTED_ASSESSMENT_CODE, guid, lineNumber);
        }

        final var assessmentCode = StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_CODE.getName()));
        if(StringUtils.isNotBlank(assessmentCode) && codeTableService.getAllAssessmentTypeCodes().stream().noneMatch(code -> code.getAssessmentTypeCode().equalsIgnoreCase(assessmentCode))) {
            throw new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, guid, lineNumber);
        }

        final var assessmentSession = StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_SESSION.getName()));
        if(StringUtils.isNotBlank(assessmentSession) && codeTableService.getAllAssessmentSessionCodes().stream().noneMatch(code -> (code.getCourseYear() + code.getCourseMonth()).equalsIgnoreCase(assessmentSession))) {
            throw new ResultsFileUnProcessableException(INVALID_ASSESSMENT_SESSION, guid, lineNumber);
        }

        final var mincode = StringMapper.trimAndUppercase(ds.getString(MINCODE.getName()));
        if(StringUtils.isNotBlank(mincode) && restUtils.getSchoolByMincode(mincode).isEmpty() ) {
            throw new ResultsFileUnProcessableException(INVALID_MINCODE, guid, lineNumber);
        }

        final var proficiencyScore = StringMapper.trimAndUppercase(ds.getString(PROFICIENCY_SCORE.getName()));
        if(StringUtils.isNotBlank(proficiencyScore) && !StringUtils.isNumeric(proficiencyScore) ) {
            throw new ResultsFileUnProcessableException(INVALID_PROFICIENCY_SCORE, guid, lineNumber);
        }

        final var irtScore = StringMapper.trimAndUppercase(ds.getString(IRT_SCORE.getName()));
        if(StringUtils.isNotBlank(irtScore)) {
            try {
                Double.parseDouble(irtScore);
            } catch (NumberFormatException e) {
                throw new ResultsFileUnProcessableException(INVALID_IRT_SCORE, guid, lineNumber);
            }
        }

        final var choicePath = StringMapper.trimAndUppercase(ds.getString(CHOICE_PATH.getName()));
        if(StringUtils.isNotBlank(choicePath) && Arrays.stream(validChoicePaths).noneMatch(choicePath::equalsIgnoreCase)) {
            throw new ResultsFileUnProcessableException(INVALID_CHOICE_PATH, guid, lineNumber);
        }

        final var pen = StringMapper.trimAndUppercase(ds.getString(PEN.getName()));
        if (StringUtils.isNotEmpty(pen) && !PenUtil.validCheckDigit(pen)) {
            throw new ResultsFileUnProcessableException(INVALID_PEN, guid, lineNumber);
        }

        final var markingSession = StringMapper.trimAndUppercase(ds.getString(MARKING_SESSION.getName()));
        if(StringUtils.isNotBlank(markingSession) && codeTableService.getAllAssessmentSessionCodes().stream().noneMatch(code -> (code.getCourseYear() + code.getCourseMonth()).equalsIgnoreCase(markingSession))) {
            throw new ResultsFileUnProcessableException(INVALID_MARKING_SESSION, guid, lineNumber);
        }

        final var formCode = StringMapper.trimAndUppercase(ds.getString(FORM_CODE.getName()));
        var courseYear = assessmentSession.substring(0, 4);
        var courseMonth = assessmentSession.substring(4);

        if (StringUtils.isNotEmpty(formCode) && assessmentFormRepository.findFormBySessionAndAssessmentType(courseYear, courseMonth, assessmentCode, formCode).isEmpty()) {
            throw new ResultsFileUnProcessableException(INVALID_FORM_CODE, guid, lineNumber);
        }

        final var openEndedMarks = StringMapper.trimAndUppercase(ds.getString(OPEN_ENDED_MARKS.getName()));
        if(StringUtils.isNotBlank(openEndedMarks) && (!pattern.matcher(openEndedMarks).matches() || openEndedMarks.length() % 4 != 0)) {
            throw new ResultsFileUnProcessableException(INVALID_OPEN_ENDED_MARKS, guid, lineNumber);
        }

        final var multiChoiceMarks = StringMapper.trimAndUppercase(ds.getString(MUL_CHOICE_MARKS.getName()));
        if(StringUtils.isNotBlank(multiChoiceMarks) && (!pattern.matcher(multiChoiceMarks).matches() || multiChoiceMarks.length() % 4 != 0)) {
            throw new ResultsFileUnProcessableException(INVALID_SELECTED_CHOICE_MARKS, guid, lineNumber);
        }

        return AssessmentResultDetails.builder()
                .txID(StringMapper.trimAndUppercase(ds.getString(TX_ID.getName())))
                .componentType(StringMapper.trimAndUppercase(ds.getString(COMPONENT_TYPE.getName())))
                .assessmentCode(StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_CODE.getName())))
                .assessmentSession(StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_SESSION.getName())))
                .mincode(StringMapper.trimAndUppercase(ds.getString(MINCODE.getName())))
                .pen(StringMapper.trimAndUppercase(ds.getString(PEN.getName())))
                .formCode(StringMapper.trimAndUppercase(ds.getString(FORM_CODE.getName())))
                .openEndedMarks(StringMapper.trimAndUppercase(ds.getString(OPEN_ENDED_MARKS.getName())))
                .multiChoiceMarks(StringMapper.trimAndUppercase(ds.getString(MUL_CHOICE_MARKS.getName())))
                .choicePath(StringMapper.trimAndUppercase(ds.getString(CHOICE_PATH.getName())))
                .specialCaseCode(StringMapper.trimAndUppercase(ds.getString(SPECIAL_CASE_CODE.getName())))
                .proficiencyScore(StringMapper.trimAndUppercase(ds.getString(PROFICIENCY_SCORE.getName())))
                .irtScore(StringMapper.trimAndUppercase(ds.getString(IRT_SCORE.getName())))
                .adaptedAssessmentIndicator(StringMapper.trimAndUppercase(ds.getString(ADAPTED_ASSESSMENT_INDICATOR.getName())))
                .markingSession(StringMapper.trimAndUppercase(ds.getString(MARKING_SESSION.getName())))
                .build();
    }
}
