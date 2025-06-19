package ca.bc.gov.educ.assessment.api.batch.service;

import ca.bc.gov.educ.assessment.api.batch.exception.FileError;
import ca.bc.gov.educ.assessment.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyFile;
import ca.bc.gov.educ.assessment.api.batch.validation.FileValidator;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.Record;
import net.sf.flatpack.StreamingDataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.*;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.ASSMT_SECTION;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.CLAIM_CODE;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.COGN_LEVEL;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.CONCEPTS_CODE;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.CONTEXT_CODE;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.IRT_COLUMN;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.ITEM;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.ITEM_TYPE;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.MARK_VALUE;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.MC_ANSWER150;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.QUES_NUMBER;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.QUES_ORIGIN;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.SCALE_FACTOR;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.TASK_CODE;
import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.TOPIC_TYPE;
import static ca.bc.gov.educ.assessment.api.batch.exception.FileError.*;
import static ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentKeysBatchFileMapper.mapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentKeyService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    private final AssessmentFormRepository assessmentFormRepository;
    private final AssessmentRepository assessmentRepository;
    private final FileValidator fileValidator;
    private final CodeTableService codeTableService;

    public static final String LOAD_FAIL = "LOADFAIL";

    @Transactional(propagation = Propagation.MANDATORY)
    public void populateBatchFileAndLoadData(String guid, DataSet ds, String incomingSession) throws FileUnProcessableException {
        val batchFile = new AssessmentKeyFile();

        var courseYear = incomingSession.substring(0, 4);
        var courseMonth = incomingSession.substring(4);

        AssessmentSessionEntity validSession =
                assessmentSessionRepository.findByCourseYearAndCourseMonth(courseYear, courseMonth)
                        .orElseThrow(() -> new FileUnProcessableException(FileError.INVALID_INCOMING_REQUEST_SESSION, guid, LOAD_FAIL));
        populateAssessmentKeyFile(ds, batchFile, validSession, guid);
        processLoadedRecordsInBatchFile(guid, batchFile, validSession);
    }

    private void populateAssessmentKeyFile(final DataSet ds, final AssessmentKeyFile batchFile, AssessmentSessionEntity validSession, final String guid) throws FileUnProcessableException {
        long index = 0;
        while (ds.next()) {
            var fileSession = ds.getString(ASSMT_SESSION.getName());
            var fileAssessmentCode = ds.getString(ASSMT_CODE.getName());
            fileValidator.validateSessionAndAssessmentCode(fileSession, validSession, fileAssessmentCode, guid, index);
            batchFile.getAssessmentKeyData().add(getAssessmentKeyDetailRecordFromFile(ds, guid, index));
            index++;
        }
    }

    private void processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final AssessmentKeyFile batchFile, AssessmentSessionEntity validSession) throws FileUnProcessableException {
        var typeCode = batchFile.getAssessmentKeyData().getFirst().getAssessmentCode();
        assessmentTypeCodeRepository.findByAssessmentTypeCode(typeCode).orElseThrow(() -> new FileUnProcessableException(FileError.INVALID_ASSESSMENT_TYPE, guid, LOAD_FAIL));

        var assessmentEntity = assessmentRepository.findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(validSession.getSessionID(), typeCode)
                .orElseThrow(() -> new FileUnProcessableException(FileError.INVALID_ASSESSMENT_CODE, guid, LOAD_FAIL));

        Map<String, List<AssessmentKeyDetails>> groupedData = batchFile.getAssessmentKeyData().stream().collect(Collectors.groupingBy(AssessmentKeyDetails::getFormCode));

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
        AssessmentComponentEntity multiComponentEntity = createAssessmentComponentEntity(assessmentFormEntity, "M", multiChoice.size(), 0, 0);

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
        return  AssessmentComponentEntity
                .builder()
                .assessmentFormEntity(assessmentFormEntity)
                .componentTypeCode(type.equalsIgnoreCase("ER") || type.equalsIgnoreCase("EO") ? "O" : type)
                .componentSubTypeCode(type.equalsIgnoreCase("EO") ? "ORAL" : "NONE")
                .questionCount(quesCount)
                .numOmits(numOmits)
//              .oeItemCount()
                .oeMarkCount(markCount)
                .printMaterialFlag(type.equalsIgnoreCase("ER") ? "Y": "N")
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
        keyEntity.setMaxQuestionValue(new BigDecimal(value.getMark()).multiply(new BigDecimal(value.getScaleFactor())).multiply(new BigDecimal(marker[1])));

        if(choiceType.equalsIgnoreCase("C0")) {
            keyEntity.setMasterQuestionNumber(StringUtils.isNotBlank(String.valueOf(question[1])) ? Integer.parseInt(String.valueOf(question[1])) : null);
        } else {
            var sublistWithMatchedItemType = quesGroup.stream().filter(subList -> subList.stream().anyMatch(v -> v.getItemType().equalsIgnoreCase(value.getItemType()))).toList();
            var lowestQues = sublistWithMatchedItemType.getFirst().stream().map(AssessmentKeyDetails::getQuestionNumber).mapToInt(Integer::parseInt).min();
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

    private AssessmentKeyDetails getAssessmentKeyDetailRecordFromFile(final DataSet ds, final String guid, final long index) throws FileUnProcessableException {
        final var itemType = StringMapper.trimAndUppercase(ds.getString(ITEM_TYPE.getName()));
        Pattern pattern = Pattern.compile("^E[R|O]-Q(\\d|\\d{2})-C(\\d)-M(\\d)$");
        if (StringUtils.isBlank(itemType) || (!itemType.equalsIgnoreCase("UNKNOWN") && !pattern.matcher(itemType).matches())) {
            throw new FileUnProcessableException(INVALID_ITEM_TYPE, guid, String.valueOf(index + 1));
        }

        final var cognLevel = StringMapper.trimAndUppercase(ds.getString(COGN_LEVEL.getName()));
        if(StringUtils.isNotBlank(cognLevel) && codeTableService.getAllCognitiveLevelCodes().stream().noneMatch(code -> code.getCognitiveLevelCode().equalsIgnoreCase(cognLevel))) {
            throw new FileUnProcessableException(INVALID_COGNITIVE_LEVEL_CODE, guid, String.valueOf(index + 1));
        }

        final var taskCode = StringMapper.trimAndUppercase(ds.getString(TASK_CODE.getName()));
        if(StringUtils.isNotBlank(taskCode) && codeTableService.getAllTaskCodes().stream().noneMatch(code -> code.getTaskCode().equalsIgnoreCase(taskCode))) {
            throw new FileUnProcessableException(INVALID_TASK_CODE, guid, String.valueOf(index + 1));
        }

        final var claimCode = StringMapper.trimAndUppercase(ds.getString(CLAIM_CODE.getName()));
        if(StringUtils.isNotBlank(claimCode) && codeTableService.getAllClaimCodes().stream().noneMatch(code -> code.getClaimCode().equalsIgnoreCase(claimCode))) {
            throw new FileUnProcessableException(INVALID_CLAIM_CODE, guid, String.valueOf(index + 1));
        }

        final var contextCode = StringMapper.trimAndUppercase(ds.getString(CONTEXT_CODE.getName()));
        if(StringUtils.isNotBlank(contextCode) && codeTableService.getAllContextCodes().stream().noneMatch(code -> code.getContextCode().equalsIgnoreCase(contextCode))) {
            throw new FileUnProcessableException(INVALID_CONTEXT_CODE, guid, String.valueOf(index + 1));
        }

        final var conceptsCode = StringMapper.trimAndUppercase(ds.getString(CONCEPTS_CODE.getName()));
        if(StringUtils.isNotBlank(conceptsCode) && codeTableService.getAllConceptCodes().stream().noneMatch(code -> code.getConceptCode().equalsIgnoreCase(conceptsCode))) {
            throw new FileUnProcessableException(INVALID_CONCEPT_CODE, guid, String.valueOf(index + 1));
        }

        return AssessmentKeyDetails.builder()
                .assessmentSession(ds.getString(ASSMT_SESSION.getName()))
                .assessmentCode(ds.getString(ASSMT_CODE.getName()))
                .formCode(ds.getString(FORM_CODE.getName()))
                .questionNumber(ds.getString(QUES_NUMBER.getName()))
                .itemType(StringMapper.trimAndUppercase(ds.getString(ITEM_TYPE.getName())))
                .answer(StringMapper.trimAndUppercase(ds.getString(MC_ANSWER150.getName())))
                .mark(StringMapper.trimAndUppercase(ds.getString(MARK_VALUE.getName())))
                .cognLevel(StringMapper.trimAndUppercase(ds.getString(COGN_LEVEL.getName())))
                .taskCode(StringMapper.trimAndUppercase(ds.getString(TASK_CODE.getName())))
                .claimCode(StringMapper.trimAndUppercase(ds.getString(CLAIM_CODE.getName())))
                .contextCode(StringMapper.trimAndUppercase(ds.getString(CONTEXT_CODE.getName())))
                .conceptsCode(StringMapper.trimAndUppercase(ds.getString(CONCEPTS_CODE.getName())))
                .topicType(StringMapper.trimAndUppercase(ds.getString(TOPIC_TYPE.getName())))
                .scaleFactor(StringMapper.trimAndUppercase(ds.getString(SCALE_FACTOR.getName())))
                .questionOrigin(StringMapper.trimAndUppercase(ds.getString(QUES_ORIGIN.getName())))
                .item(StringMapper.trimAndUppercase(ds.getString(ITEM.getName())))
                .irt(StringMapper.trimAndUppercase(ds.getString(IRT_COLUMN.getName())))
                .assessmentSection(StringMapper.trimAndUppercase(ds.getString(ASSMT_SECTION.getName())))
                .build();
    }
}
