package ca.bc.gov.educ.assessment.api.batch.service;

import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileError;
import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.exception.ResultsFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultFile;
import ca.bc.gov.educ.assessment.api.batch.validation.KeyFileValidator;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentFormRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentTypeCodeRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.util.PenUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentResultsBatchFile.*;
import static ca.bc.gov.educ.assessment.api.batch.exception.ResultFileError.*;
import static ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentKeysBatchFileMapper.mapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentResultService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    private final AssessmentFormRepository assessmentFormRepository;
    private final AssessmentRepository assessmentRepository;
    private final KeyFileValidator keyFileValidator;
    private final CodeTableService codeTableService;
    private final String[] validChoicePaths = {"I", "E"};
    private final String answerRegex = "^(\\d*\\.?\\d+|\\.\\d+)$";
    private final Pattern pattern = Pattern.compile(answerRegex);

    public static final String LOAD_FAIL = "LOADFAIL";
    private final RestUtils restUtils;

    @Transactional(propagation = Propagation.MANDATORY)
    public void populateBatchFileAndLoadData(String guid, DataSet ds, UUID sessionID) throws ResultsFileUnProcessableException {
        val batchFile = new AssessmentResultFile();

        AssessmentSessionEntity validSession =
                assessmentSessionRepository.findById(sessionID)
                        .orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_INCOMING_REQUEST_SESSION, guid, LOAD_FAIL));
        populateAssessmentResultsFile(ds, batchFile, validSession, guid);
        processLoadedRecordsInBatchFile(guid, batchFile, validSession);
    }

    private void populateAssessmentResultsFile(final DataSet ds, final AssessmentResultFile batchFile, AssessmentSessionEntity validSession, final String guid) throws ResultsFileUnProcessableException {
        long index = 0;
        while (ds.next()) {
            batchFile.getAssessmentResultData().add(getAssessmentResultDetailRecordFromFile(ds, guid, index));
            index++;
        }
    }

    private void processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final AssessmentResultFile batchFile, AssessmentSessionEntity validSession) throws ResultsFileUnProcessableException {
        var typeCode = batchFile.getAssessmentResultData().getFirst().getAssessmentCode();
        assessmentTypeCodeRepository.findByAssessmentTypeCode(typeCode).orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, guid, LOAD_FAIL));

        var assessmentEntity = assessmentRepository.findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(validSession.getSessionID(), typeCode)
                .orElseThrow(() -> new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, guid, LOAD_FAIL));

        Map<String, List<AssessmentResultDetails>> groupedData = batchFile.getAssessmentResultData().stream().collect(Collectors.groupingBy(AssessmentResultDetails::getFormCode));

        for(val entry : groupedData.entrySet()) {
            AssessmentFormEntity formEntity = mapper.toFormEntity(entry.getKey(), assessmentEntity);
            var multiChoice = entry.getValue().stream().filter(value -> value.getItemType().equalsIgnoreCase("UNKNOWN")).toList();
            var openEndedWritten = entry.getValue().stream().filter(value -> {
                var itemType = value.getItemType().split("-");
                return itemType[0].equalsIgnoreCase("ER");
            }).toList();

            var openEndedOral = entry.getValue().stream().filter(value -> {
                var itemType = value.getItemType().split("-");
                return itemType[0].equalsIgnoreCase("EO");
            }).toList();

            if(!multiChoice.isEmpty()) {
                formEntity.getAssessmentComponentEntities().add(createMultiChoiceComponent(formEntity, multiChoice));
            }

            if(!openEndedWritten.isEmpty()) {
                formEntity.getAssessmentComponentEntities().add(createOpenEndedComponent(formEntity, openEndedWritten, "ER"));
            }

            if(!openEndedOral.isEmpty()) {
                formEntity.getAssessmentComponentEntities().add(createOpenEndedComponent(formEntity, openEndedOral, "EO"));
            }
            craftStudentSetAndMarkInitialLoadComplete(formEntity);
       }
    }

    @Retryable(retryFor = {Exception.class}, backoff = @Backoff(multiplier = 3, delay = 2000))
    public void craftStudentSetAndMarkInitialLoadComplete(@NonNull final AssessmentFormEntity assessmentFormEntity) {
      var formEntity = assessmentFormRepository.findByAssessmentEntity_AssessmentIDAndFormCode(assessmentFormEntity.getAssessmentEntity().getAssessmentID(), assessmentFormEntity.getFormCode());
      if(formEntity.isPresent()) {
          var existingFormEntity = formEntity.get();
          assessmentFormEntity.getAssessmentComponentEntities().forEach(comp -> comp.setAssessmentFormEntity(existingFormEntity));
          var updatedComponents = assessmentFormEntity.getAssessmentComponentEntities().stream().toList();

          existingFormEntity.setUpdateUser(assessmentFormEntity.getUpdateUser());
          existingFormEntity.setUpdateDate(LocalDateTime.now());
          existingFormEntity.getAssessmentComponentEntities().clear();
          existingFormEntity.getAssessmentComponentEntities().addAll(updatedComponents);
          assessmentFormRepository.save(assessmentFormEntity);
      } else {
          assessmentFormRepository.save(assessmentFormEntity);
      }
    }

    private AssessmentComponentEntity createMultiChoiceComponent(AssessmentFormEntity assessmentFormEntity, List<AssessmentKeyDetails> multiChoice) {
        AssessmentComponentEntity multiComponentEntity = createAssessmentComponentEntity(assessmentFormEntity, "MUL_CHOICE", multiChoice.size(), multiChoice.size(), 0);

        multiChoice.forEach(ques -> {
            final var quesEntity = mapper.toQuestionEntity(ques, multiComponentEntity);
            setMultiChoiceQuestionEntityColumns(quesEntity, multiChoice, ques);
            multiComponentEntity.getAssessmentQuestionEntities().add(quesEntity);
        });
        return multiComponentEntity;
    }

    private AssessmentComponentEntity createOpenEndedComponent(AssessmentFormEntity assessmentFormEntity, List<AssessmentKeyDetails> openEnded, final String type) {
        List<List<AssessmentKeyDetails>> questionGroups = createQuestionGroups(openEnded);

        AtomicInteger markCount = new AtomicInteger();
        questionGroups.forEach(questionGroup -> {
            var choiceQues = questionGroup.stream().map(AssessmentKeyDetails::getItemType).filter(v -> v.contains("-C0-")).map(x -> {
                var splits = x.split("-");
                var marker = splits[3].toCharArray();
                return Integer.parseInt(String.valueOf(marker[1]));
            }).reduce(Integer::sum);

            var choiceInt = choiceQues.orElse(0);

            //need to update logic
            var nonchoiceQues = questionGroup.stream().map(AssessmentKeyDetails::getItemType).filter(v -> v.contains("-C1-")).toList();
            markCount.set(choiceInt + nonchoiceQues.size());
        });
        AssessmentComponentEntity openEndedComponentEntity = createAssessmentComponentEntity(assessmentFormEntity, type, openEnded.size(), openEnded.size(), markCount.get());

        openEnded.forEach(ques -> {
            final var quesEntity = mapper.toQuestionEntity(ques, openEndedComponentEntity);
            setOpenEndedWrittenQuestionEntityColumns(quesEntity, ques, questionGroups);
            var itemType = ques.getItemType().split("-");
            var marker = itemType[3].toCharArray();
            var index = Integer.parseInt(String.valueOf(marker[1]));
            while (index > 0) {
                openEndedComponentEntity.getAssessmentQuestionEntities().add(quesEntity);
                index--;
            }
        });
        return openEndedComponentEntity;
    }

    private AssessmentComponentEntity createAssessmentComponentEntity(final AssessmentFormEntity assessmentFormEntity, final String type, int quesCount, int numOmits, int markCount) {
        return  AssessmentStudentComponentEntity
                .builder()
                .assessmentFormEntity(assessmentFormEntity)
                .componentTypeCode(type.equalsIgnoreCase("ER") || type.equalsIgnoreCase("EO") ? "OPEN_ENDED" : type)
                .componentSubTypeCode(type.equalsIgnoreCase("EO") ? "ORAL" : "NONE")
                .questionCount(quesCount)
                .numOmits(numOmits)
//              .oeItemCount()
                .oeMarkCount(markCount)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .createUser(assessmentFormEntity.getCreateUser())
                .updateUser(assessmentFormEntity.getUpdateUser())
                .build();
    }

    private void setMultiChoiceQuestionEntityColumns(AssessmentQuestionEntity keyEntity, List<AssessmentKeyDetails> multiChoiceQuesGroup, AssessmentKeyDetails value) {
        //item number
        keyEntity.setQuestionNumber(StringUtils.isNotBlank(value.getQuestionNumber()) ? Integer.parseInt(value.getQuestionNumber()) : null);
        keyEntity.setMaxQuestionValue(StringUtils.isNotBlank(value.getMark()) && StringUtils.isNotBlank(value.getScaleFactor())
                ? new BigDecimal(value.getMark()).multiply(new BigDecimal(value.getScaleFactor()))
                : BigDecimal.ZERO);
        var lowestQues = multiChoiceQuesGroup.stream().map(AssessmentKeyDetails::getQuestionNumber).mapToInt(Integer::parseInt).min();
        keyEntity.setMasterQuestionNumber(lowestQues.getAsInt());
    }

    private void setOpenEndedWrittenQuestionEntityColumns(AssessmentQuestionEntity keyEntity, AssessmentKeyDetails value, List<List<AssessmentKeyDetails>> quesGroup) {
        //item number
        var itemType = value.getItemType().split("-");
        var question = itemType[1].toCharArray();
        var choiceType = itemType[2];
        var marker = itemType[3].toCharArray();

        keyEntity.setQuestionNumber(StringUtils.isNotBlank(String.valueOf(question[1])) ? Integer.parseInt(String.valueOf(question[1])) : null);
        keyEntity.setMaxQuestionValue(new BigDecimal(value.getMark()).multiply(new BigDecimal(value.getScaleFactor())).multiply(new BigDecimal(String.valueOf(marker[1]))));

        if(choiceType.equalsIgnoreCase("C0")) {
            keyEntity.setMasterQuestionNumber(StringUtils.isNotBlank(String.valueOf(question[1])) ? Integer.parseInt(String.valueOf(question[1])) : null);
        } else {
            var sublistWithMatchedItemType = quesGroup.stream().filter(subList -> subList.stream().anyMatch(v -> v.getItemType().equalsIgnoreCase(value.getItemType()))).toList();
            var lowestQues = sublistWithMatchedItemType.getFirst().stream().map(AssessmentKeyDetails::getItemType).map(v -> {
                var typeSplit = v.split("-")[1].toCharArray();
                return String.valueOf(typeSplit[1]);
            }).mapToInt(Integer::parseInt).min();
            keyEntity.setMasterQuestionNumber(lowestQues.getAsInt());
        }
    }

    private List<List<AssessmentKeyDetails>> createQuestionGroups(List<AssessmentKeyDetails> questions) {
        var indexList = IntStream.range(0, questions.size()).filter(v -> questions.get(v).getItemType().contains("-C1-")).boxed().toList();

        return IntStream.range(0, indexList.size())
                .mapToObj(j -> j == indexList.size() - 1 ? questions.subList(indexList.get(j), questions.size())
                        : questions.subList(indexList.get(j), indexList.get(j + 1)))
                .toList();
    }

    private AssessmentResultDetails getAssessmentResultDetailRecordFromFile(final DataSet ds, final String guid, final long index) throws ResultsFileUnProcessableException {
        final var txID = StringMapper.trimAndUppercase(ds.getString(TX_ID.getName()));
        if (StringUtils.isBlank(txID) || !txID.equalsIgnoreCase("")) {
            throw new ResultsFileUnProcessableException(INVALID_TXID, guid, txID);
        }

        final var componentType = StringMapper.trimAndUppercase(ds.getString(COMPONENT_TYPE.getName()));
        if(StringUtils.isNotBlank(componentType) && codeTableService.getAllComponentTypeCodes().stream().noneMatch(code -> code.getComponentTypeCode().equalsIgnoreCase(componentType))) {
            throw new ResultsFileUnProcessableException(INVALID_COMPONENT_TYPE_CODE, guid, componentType);
        }

        final var specialCaseCode = StringMapper.trimAndUppercase(ds.getString(SPECIAL_CASE_CODE.getName()));
        if(StringUtils.isNotBlank(specialCaseCode) && codeTableService.getAllProvincialSpecialCaseCodes().stream().noneMatch(code -> code.getProvincialSpecialCaseCode().equalsIgnoreCase(specialCaseCode))) {
            throw new ResultsFileUnProcessableException(INVALID_SPECIAL_CASE_CODE, guid, specialCaseCode);
        }

        final var adaptedAssessmentIndicator = StringMapper.trimAndUppercase(ds.getString(ADAPTED_ASSESSMENT_INDICATOR.getName()));
        if(StringUtils.isNotBlank(adaptedAssessmentIndicator) && codeTableService.getAdaptedAssessmentIndicatorCodes().stream().noneMatch(code -> code.getLegacyCode().equalsIgnoreCase(adaptedAssessmentIndicator))) {
            throw new ResultsFileUnProcessableException(INVALID_ADAPTED_ASSESSMENT_CODE, guid, adaptedAssessmentIndicator);
        }

        final var assessmentCode = StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_CODE.getName()));
        if(StringUtils.isNotBlank(assessmentCode) && codeTableService.getAllAssessmentTypeCodes().stream().noneMatch(code -> code.getAssessmentTypeCode().equalsIgnoreCase(assessmentCode))) {
            throw new ResultsFileUnProcessableException(INVALID_ASSESSMENT_TYPE, guid, assessmentCode);
        }

        final var assessmentSession = StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_SESSION.getName()));
        if(StringUtils.isNotBlank(assessmentSession) && codeTableService.getAllAssessmentSessionCodes().stream().noneMatch(code -> (code.getCourseYear() + code.getCourseMonth()).equalsIgnoreCase(assessmentSession))) {
            throw new ResultsFileUnProcessableException(INVALID_ASSESSMENT_SESSION, guid, assessmentSession);
        }

        final var mincode = StringMapper.trimAndUppercase(ds.getString(MINCODE.getName()));
        if(StringUtils.isNotBlank(mincode) && restUtils.getSchoolByMincode(mincode).isEmpty() ) {
            throw new ResultsFileUnProcessableException(INVALID_MINCODE_ASSESSMENT_CENTER, guid, mincode);
        }

        final var proficiencyScore = StringMapper.trimAndUppercase(ds.getString(PROFICIENCY_SCORE.getName()));
        if(StringUtils.isNotBlank(proficiencyScore) && !StringUtils.isNumeric(proficiencyScore) ) {
            throw new ResultsFileUnProcessableException(INVALID_PROFICIENCY_SCORE, guid, proficiencyScore);
        }

        final var irtScore = StringMapper.trimAndUppercase(ds.getString(IRT_SCORE.getName()));
        if(StringUtils.isNotBlank(irtScore) && !StringUtils.isNumeric(irtScore) ) {
            throw new ResultsFileUnProcessableException(INVALID_IRT_SCORE, guid, irtScore);
        }

        final var choicePath = StringMapper.trimAndUppercase(ds.getString(CHOICE_PATH.getName()));
        if(StringUtils.isNotBlank(choicePath) && Arrays.stream(validChoicePaths).noneMatch(choicePath::equalsIgnoreCase)) {
            throw new ResultsFileUnProcessableException(INVALID_CHOICE_PATH, guid, choicePath);
        }

        final var pen = StringMapper.trimAndUppercase(ds.getString(PEN.getName()));
        if (StringUtils.isNotEmpty(pen) && !PenUtil.validCheckDigit(pen)) {
            throw new ResultsFileUnProcessableException(INVALID_PEN, guid, pen);
        }

        final var markingSession = StringMapper.trimAndUppercase(ds.getString(MARKING_SESSION.getName()));
        if(StringUtils.isNotBlank(markingSession) && codeTableService.getAllAssessmentSessionCodes().stream().noneMatch(code -> (code.getCourseYear() + code.getCourseMonth()).equalsIgnoreCase(markingSession))) {
            throw new ResultsFileUnProcessableException(INVALID_MARKING_SESSION, guid, markingSession);
        }

        final var formCode = StringMapper.trimAndUppercase(ds.getString(FORM_CODE.getName()));
        var courseYear = assessmentSession.substring(0, 4);
        var courseMonth = assessmentSession.substring(4);

        if (StringUtils.isNotEmpty(formCode) && assessmentFormRepository.findFormBySessionAndAssessmentType(courseYear, courseMonth, assessmentCode, formCode).isEmpty()) {
            throw new ResultsFileUnProcessableException(INVALID_FORM_CODE, guid, pen);
        }

        final var openEndedMarks = StringMapper.trimAndUppercase(ds.getString(OPEN_ENDED_MARKS.getName()));
        if(StringUtils.isNotBlank(openEndedMarks) && !pattern.matcher(openEndedMarks).matches()) {
            throw new ResultsFileUnProcessableException(INVALID_OPEN_ENDED_MARKS, guid, openEndedMarks);
        }

        final var multiChoiceMarks = StringMapper.trimAndUppercase(ds.getString(MUL_CHOICE_MARKS.getName()));
        if(StringUtils.isNotBlank(multiChoiceMarks) && !pattern.matcher(multiChoiceMarks).matches()) {
            throw new ResultsFileUnProcessableException(INVALID_SELECTED_CHOICE_MARKS, guid, multiChoiceMarks);
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
