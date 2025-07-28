package ca.bc.gov.educ.assessment.api.batch.service;

import ca.bc.gov.educ.assessment.api.batch.exception.ResultsFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentResultsBatchFileMapper;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultFile;
import ca.bc.gov.educ.assessment.api.constants.v1.LegacyComponentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.ConfirmationRequiredException;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
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
    private final AssessmentRepository assessmentRepository;
    private final CodeTableService codeTableService;
    private final String[] validChoicePaths = {"I", "E"};
    private final String[] validSpecialCaseCodes = {"A", "Q", "X"};
    private final Pattern pattern = Pattern.compile("^[0-9.]*$");

    public static final String LOAD_FAIL = "LOADFAIL";
    private final RestUtils restUtils;
    private static final AssessmentResultsBatchFileMapper assessmentResultsBatchFileMapper = AssessmentResultsBatchFileMapper.mapper;
    private final StagedStudentResultRepository  stagedStudentResultRepository;

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
            resultEntity.setAssessmentFormID(formEntity.getAssessmentFormID());
            resultEntity.setStagedStudentResultStatus("LOADED");
            stagedStudentResultRepository.save(resultEntity);
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
