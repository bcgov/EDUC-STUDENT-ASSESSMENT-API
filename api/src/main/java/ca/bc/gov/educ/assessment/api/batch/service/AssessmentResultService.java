package ca.bc.gov.educ.assessment.api.batch.service;

import ca.bc.gov.educ.assessment.api.batch.exception.ResultsFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentResultsBatchFileMapper;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultFile;
import ca.bc.gov.educ.assessment.api.constants.v1.ComponentSubTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.ComponentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.LegacyComponentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.ConfirmationRequiredException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.service.v1.StudentAssessmentResultService;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.util.PenUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentResultsBatchFile.*;
import static ca.bc.gov.educ.assessment.api.batch.exception.ResultFileError.*;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentResultService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentRepository assessmentRepository;
    private final CodeTableService codeTableService;
    private final String[] validChoicePaths = {"I", "E"};
    private final String[] validSpecialCaseCodes = {"A", "Q", "X"};
    private final Pattern pattern = Pattern.compile("^[0-9.]*$");

    public static final String LOAD_FAIL = "LOADFAIL";
    private final RestUtils restUtils;
    private static final AssessmentResultsBatchFileMapper assessmentResultsBatchFileMapper = AssessmentResultsBatchFileMapper.mapper;
    private final StagedStudentResultRepository  stagedStudentResultRepository;
    private final StudentAssessmentResultService studentAssessmentResultService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void populateBatchFileAndLoadData(String correlationID, DataSet ds, UUID sessionID, AssessmentResultFileUpload fileUpload, boolean isSingleResult) throws ResultsFileUnProcessableException {
        val batchFile = new AssessmentResultFile();

        AssessmentSessionEntity validSession =
                assessmentSessionRepository.findById(sessionID)
                        .orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_INCOMING_REQUEST_SESSION, correlationID, LOAD_FAIL));
        populateAssessmentResultsFile(ds, batchFile, correlationID);
        validatePENsInFile(batchFile, correlationID);
        if(isSingleResult) {
            processCorrectionRecordsInBatchFile(correlationID, batchFile, validSession, fileUpload);
        } else {
            processLoadedRecordsInBatchFile(correlationID, batchFile, validSession, fileUpload);
        }
    }
    
    private void validatePENsInFile(AssessmentResultFile batchFile, String correlationID) throws ResultsFileUnProcessableException {
        for(var assessmentResultData: batchFile.getAssessmentResultData()){
            var pen = assessmentResultData.getPen();
            var componentType = assessmentResultData.getComponentType();
            var filteredPEN = batchFile.getAssessmentResultData().stream().filter(student -> student.getPen().equals(pen) && student.getComponentType().equalsIgnoreCase(componentType)).count();
            if(filteredPEN > 1) {
                throw new ResultsFileUnProcessableException(INVALID_PEN_DUPLICATE_IN_FILE, correlationID, pen);
            }
        }
    }

    private void populateAssessmentResultsFile(final DataSet ds, final AssessmentResultFile batchFile, final String guid) throws ResultsFileUnProcessableException {
        int index = 0;
        while (ds.next()) {
            batchFile.getAssessmentResultData().add(getAssessmentResultDetailRecordFromFile(ds, guid, Integer.toString(index + 1)));
            index++;
        }
    }

    private void processLoadedRecordsInBatchFile(@NonNull final String correlationID, @NonNull final AssessmentResultFile batchFile, AssessmentSessionEntity validSession, AssessmentResultFileUpload fileUpload) throws ResultsFileUnProcessableException {
        var typeCode = batchFile.getAssessmentResultData().getFirst().getAssessmentCode();
        var assessmentEntity = assessmentRepository.findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(validSession.getSessionID(), typeCode)
                .orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, correlationID, LOAD_FAIL));

        Map<String, AssessmentFormEntity> formMap;
        if(!assessmentEntity.getAssessmentForms().isEmpty()) {
            formMap = assessmentEntity.getAssessmentForms().stream()
                    .collect(Collectors.toMap(AssessmentFormEntity::getFormCode, obj -> obj,(oldValue, newValue) -> newValue, HashMap::new));
        } else {
            throw new ResultsFileUnProcessableException(INVALID_KEY, correlationID, LOAD_FAIL);
        }

        List<UUID> formIds = assessmentEntity.getAssessmentForms().stream().map(AssessmentFormEntity::getAssessmentFormID).toList();

        var stud = stagedStudentResultRepository.findByAssessmentIdAndStagedStudentResultStatusOrderByCreateDateDesc(assessmentEntity.getAssessmentID());
        if(stud.isPresent()) {
            throw new ResultsFileUnProcessableException(RESULT_LOAD_ALREADY_IN_FLIGHT, correlationID, LOAD_FAIL);
        }
        
        Optional<StagedAssessmentStudentEntity> student = stagedAssessmentStudentRepository.findByAssessmentIdAndAssessmentFormIdOrderByCreateDateDesc(assessmentEntity.getAssessmentID(), formIds);
        if("N".equalsIgnoreCase(fileUpload.getReplaceResultsFlag()) && student.isPresent()) {
            throw new ConfirmationRequiredException(ApiError.builder().timestamp(LocalDateTime.now()).message(typeCode).status(PRECONDITION_REQUIRED).build());
        }
        
        stagedAssessmentStudentRepository.deleteAllByAssessmentID(assessmentEntity.getAssessmentID());

        for(val studentResult : batchFile.getAssessmentResultData()) {
            StagedStudentResultEntity resultEntity = assessmentResultsBatchFileMapper.toStagedStudentResultEntity(studentResult, assessmentEntity, fileUpload);
            var formEntity = formMap.get(studentResult.getFormCode());
            if(formEntity == null) {
                throw new ResultsFileUnProcessableException(INVALID_FORM_CODE, correlationID, LOAD_FAIL);
            }
            checkChoicePathValue(studentResult, formEntity, correlationID);
            resultEntity.setAssessmentFormID(formEntity.getAssessmentFormID());
            resultEntity.setStagedStudentResultStatus("LOADED");
            stagedStudentResultRepository.save(resultEntity);
        }
    }

    private void checkChoicePathValue(AssessmentResultDetails studentResult, AssessmentFormEntity formEntity, final String correlationID) throws ResultsFileUnProcessableException {
        final var choicePath = studentResult.getChoicePath();
        final var mcComponent = formEntity.getAssessmentComponentEntities().stream()
                .filter(component -> component.getComponentTypeCode().equalsIgnoreCase("MUL_CHOICE")).toList();
        if(StringUtils.isNotBlank(choicePath) && mcComponent.isEmpty()) {
            throw new ResultsFileUnProcessableException(BLANK_CHOICE_PATH, correlationID, studentResult.getLineNumber());
        } else if(StringUtils.isNotBlank(choicePath) && !mcComponent.isEmpty()) {
            var hasTaskCodes = mcComponent.getFirst()
                    .getAssessmentQuestionEntities()
                    .stream()
                    .anyMatch(questionEntity ->
                            questionEntity.getTaskCode().equalsIgnoreCase("I") || questionEntity.getTaskCode().equalsIgnoreCase("E"));

            if(hasTaskCodes && Arrays.stream(validChoicePaths).noneMatch(choicePath::equalsIgnoreCase)) {
                throw new ResultsFileUnProcessableException(INVALID_CHOICE_PATH, correlationID, studentResult.getLineNumber());
            }
        }
    }

    private void processCorrectionRecordsInBatchFile(@NonNull final String correlationID, @NonNull final AssessmentResultFile batchFile, AssessmentSessionEntity validSession, AssessmentResultFileUpload fileUpload) throws ResultsFileUnProcessableException {
        var typeCode = batchFile.getAssessmentResultData().getFirst().getAssessmentCode();
        var assessmentEntity = assessmentRepository.findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(validSession.getSessionID(), typeCode)
                .orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, correlationID, LOAD_FAIL));

        Map<String, AssessmentFormEntity> formMap;
        if(!assessmentEntity.getAssessmentForms().isEmpty()) {
            formMap = assessmentEntity.getAssessmentForms().stream()
                    .collect(Collectors.toMap(AssessmentFormEntity::getFormCode, obj -> obj,(oldValue, newValue) -> newValue, HashMap::new));
        } else {
            throw new ResultsFileUnProcessableException(INVALID_KEY, correlationID, LOAD_FAIL);
        }

        Map<String, List<AssessmentResultDetails>> groupedStudents = batchFile.getAssessmentResultData().stream().collect(Collectors.groupingBy(AssessmentResultDetails::getPen));

        for(val groupedResult : groupedStudents.entrySet()) {

            var studentResultOptional = groupedResult.getValue().stream().filter(e -> !e.getComponentType().equalsIgnoreCase("7")).findFirst();
            var studentResult =  studentResultOptional.get();
            var formEntity = formMap.get(studentResult.getFormCode());
            if (formEntity == null) {
                throw new ResultsFileUnProcessableException(INVALID_FORM_CODE, correlationID, LOAD_FAIL);
            }
            checkChoicePathValue(studentResult, formEntity, correlationID);
            createStudentRecordForCorrectionFile(groupedResult.getValue(), fileUpload, validSession, assessmentEntity, formEntity);
        }
    }

    private void createStudentRecordForCorrectionFile(List<AssessmentResultDetails> groupedResult, AssessmentResultFileUpload fileUpload, AssessmentSessionEntity validSession, AssessmentEntity assessmentEntity, AssessmentFormEntity formEntity) {
        var studentResultOptional = groupedResult.stream().filter(e -> !e.getComponentType().equalsIgnoreCase("7")).toList();
        var studentResult =  studentResultOptional.getFirst();

        var optStudent = restUtils.getStudentByPEN(UUID.randomUUID(), studentResult.getPen());
        if (validSession.getCompletionDate() != null) {
            //approved session
            if (optStudent.isPresent()) {
                Student studentApiStudent = optStudent.get();
                boolean isMergedRecord = false;
                Student trueStudentApiStudentRecord;
                Student currentStudent = optStudent.get();
                int mergeDepth = 0;
                int MAX_MERGE_DEPTH = 10;
                
                while(currentStudent.getStatusCode().equalsIgnoreCase("M") && mergeDepth < MAX_MERGE_DEPTH) {
                    List<Student> mergedStudents = restUtils.getStudents(UUID.randomUUID(), Set.of(currentStudent.getTrueStudentID()));
                    currentStudent = mergedStudents.getFirst();
                    mergeDepth++;
                    isMergedRecord = true;
                }
                
                trueStudentApiStudentRecord = currentStudent;
                var studentID = isMergedRecord ? trueStudentApiStudentRecord.getStudentID() : studentApiStudent.getStudentID();
                var studentApiRecord = isMergedRecord ? trueStudentApiStudentRecord : studentApiStudent;
                Optional<AssessmentStudentEntity> student = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(studentID));
                //delete existing student from assessment student table
                student.ifPresent(assessmentStudentEntity -> assessmentStudentRepository.deleteById(assessmentStudentEntity.getAssessmentStudentID()));

                var gradStudent = restUtils.getGradStudentRecordByStudentID(UUID.randomUUID(), UUID.fromString(studentID)).orElse(null);
                assessmentStudentRepository.save(createResultForApprovedSession(groupedResult, fileUpload, gradStudent, studentApiRecord, formEntity, assessmentEntity));
            }
        } else {
            //on-going session, delete student from staging table and load in the result upload table
            createResultForOngoingSession(optStudent, groupedResult, fileUpload, formEntity, assessmentEntity);
        }
    }

    private void createResultForOngoingSession(Optional<Student> optStudent, List<AssessmentResultDetails> groupedResult, AssessmentResultFileUpload fileUpload, AssessmentFormEntity formEntity, AssessmentEntity assessmentEntity) {
        if (optStudent.isPresent()) {
            Student studentApiStudent = optStudent.get();
            Optional<StagedAssessmentStudentEntity> optStagedStudent = stagedAssessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(studentApiStudent.getStudentID()));
            optStagedStudent.ifPresent(stagedAssessmentStudentEntity -> stagedAssessmentStudentRepository.deleteById(stagedAssessmentStudentEntity.getAssessmentStudentID()));
        }
        groupedResult.forEach(assessmentResultEntity -> {
            StagedStudentResultEntity resultEntity = assessmentResultsBatchFileMapper.toStagedStudentResultEntity(assessmentResultEntity, assessmentEntity, fileUpload);
            resultEntity.setAssessmentFormID(formEntity.getAssessmentFormID());
            resultEntity.setStagedStudentResultStatus("LOADED");
            stagedStudentResultRepository.save(resultEntity);
        });
    }

    private AssessmentStudentEntity createResultForApprovedSession(List<AssessmentResultDetails> groupedResult, AssessmentResultFileUpload fileUpload, GradStudentRecord gradStudent, Student studentApiRecord, AssessmentFormEntity formEntity, AssessmentEntity assessmentEntity) {
        var studentResultOptional = groupedResult.stream().filter(e -> !e.getComponentType().equalsIgnoreCase("7")).toList();
        var studentResult =  studentResultOptional.getFirst();

        var studentWithOralComponent = groupedResult.stream().filter(e -> e.getComponentType().equalsIgnoreCase("7")).findFirst();

        var assessmentStudent = studentAssessmentResultService.createNewAssessmentStudentEntity(studentResult, studentApiRecord, gradStudent, formEntity.getAssessmentFormID(), assessmentEntity);
        assessmentStudent.setCreateDate(LocalDateTime.now());
        assessmentStudent.setCreateUser(fileUpload.getCreateUser());
        assessmentStudent.setUpdateUser(fileUpload.getUpdateUser());
        assessmentStudent.setUpdateDate(LocalDateTime.now());

        createStudentComponent(formEntity, assessmentStudent, fileUpload, studentResult);
        studentWithOralComponent.ifPresent(stagedStudentResultEntity -> createStudentComponent(formEntity, assessmentStudent, fileUpload, studentResult));

        var mcTotal = studentAssessmentResultService.setAssessmentStudentTotals(assessmentStudent, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE, formEntity);
        var oeTotal = studentAssessmentResultService.setAssessmentStudentTotals(assessmentStudent, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE, formEntity);
        var oralTotal = studentAssessmentResultService.setAssessmentStudentTotals(assessmentStudent, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.ORAL, formEntity);

        assessmentStudent.setMcTotal(mcTotal);
        assessmentStudent.setOeTotal(oeTotal.add(oralTotal));
        assessmentStudent.setRawScore(mcTotal.add(oralTotal).add(oeTotal));

        return assessmentStudent;
    }

    private void createStudentComponent(AssessmentFormEntity formEntity, AssessmentStudentEntity assessmentStudent, AssessmentResultFileUpload fileUpload, AssessmentResultDetails studentResult) {
        switch (LegacyComponentTypeCodes.findByValue(studentResult.getComponentType()).orElseThrow()) {
            case MUL_CHOICE -> studentAssessmentResultService.addAssessmentStudentComponent(formEntity, assessmentStudent, fileUpload, studentResult, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE);
            case OPEN_ENDED -> studentAssessmentResultService.addAssessmentStudentComponent(formEntity, assessmentStudent, fileUpload, studentResult, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE);
            case ORAL -> studentAssessmentResultService.addAssessmentStudentComponent(formEntity, assessmentStudent, fileUpload, studentResult, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.ORAL);
            case BOTH -> {
                studentAssessmentResultService.addAssessmentStudentComponent(formEntity, assessmentStudent, fileUpload, studentResult, ComponentTypeCodes.MUL_CHOICE, ComponentSubTypeCodes.NONE);
                studentAssessmentResultService.addAssessmentStudentComponent(formEntity, assessmentStudent, fileUpload, studentResult, ComponentTypeCodes.OPEN_ENDED, ComponentSubTypeCodes.NONE);
            }
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
        if(StringUtils.isBlank(adaptedAssessmentIndicator) && StringUtils.isNotBlank(StringMapper.trimAndUppercase(ds.getString(MARKING_SESSION.getName())))) {
            throw new ResultsFileUnProcessableException(BLANK_ADAPTED_ASSESSMENT_CODE, guid, lineNumber);
        } else if(StringUtils.isNotBlank(adaptedAssessmentIndicator) && codeTableService.getAdaptedAssessmentIndicatorCodes().stream().noneMatch(code -> code.getLegacyCode().equalsIgnoreCase(adaptedAssessmentIndicator))) {
            throw new ResultsFileUnProcessableException(INVALID_ADAPTED_ASSESSMENT_CODE, guid, lineNumber);
        }

        final var markingSession = StringMapper.trimAndUppercase(ds.getString(MARKING_SESSION.getName()));
        if(StringUtils.isBlank(markingSession) && StringUtils.isNotBlank(adaptedAssessmentIndicator)) {
            throw new ResultsFileUnProcessableException(BLANK_MARKING_SESSION, guid, lineNumber);
        } else if(StringUtils.isNotBlank(markingSession) && codeTableService.getAllAssessmentSessionCodes().stream().noneMatch(code -> (code.getCourseYear() + code.getCourseMonth()).equalsIgnoreCase(markingSession))) {
            throw new ResultsFileUnProcessableException(INVALID_MARKING_SESSION, guid, lineNumber);
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

        final var pen = StringMapper.trimAndUppercase(ds.getString(PEN.getName()));
        if (StringUtils.isNotEmpty(pen) && !PenUtil.validCheckDigit(pen)) {
            throw new ResultsFileUnProcessableException(INVALID_PEN, guid, lineNumber);
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
                .lineNumber(lineNumber)
                .build();
    }
}
