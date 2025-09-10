package ca.bc.gov.educ.assessment.api.batch.service;

import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileError;
import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyFile;
import ca.bc.gov.educ.assessment.api.batch.validation.KeyFileValidator;
import ca.bc.gov.educ.assessment.api.exception.ConfirmationRequiredException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
import static ca.bc.gov.educ.assessment.api.batch.exception.KeyFileError.*;
import static ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentKeysBatchFileMapper.mapper;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentKeyService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentRepository assessmentRepository;
    private final KeyFileValidator keyFileValidator;
    private final CodeTableService codeTableService;
    private final String[] literacyCodes = {"LTE10", "LTP10", "LTE12", "LTF12", "LTP12"};
    private final String[] allowedTaskCodes = {"A", "E", "I"};

    public static final String LOAD_FAIL = "LOADFAIL";

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void populateBatchFileAndLoadData(String guid, DataSet ds, UUID sessionID, AssessmentKeyFileUpload fileUpload) throws KeyFileUnProcessableException {
        val batchFile = new AssessmentKeyFile();

        AssessmentSessionEntity validSession =
                assessmentSessionRepository.findById(sessionID)
                        .orElseThrow(() -> new KeyFileUnProcessableException(KeyFileError.INVALID_INCOMING_REQUEST_SESSION, guid, LOAD_FAIL));
        populateAssessmentKeyFile(ds, batchFile, validSession, guid);
        processLoadedRecordsInBatchFile(guid, batchFile, validSession, fileUpload);
    }

    private void populateAssessmentKeyFile(final DataSet ds, final AssessmentKeyFile batchFile, AssessmentSessionEntity validSession, final String guid) throws KeyFileUnProcessableException {
        long index = 0;
        while (ds.next()) {
            var fileSession = ds.getString(ASSMT_SESSION.getName());
            var fileAssessmentCode = ds.getString(ASSMT_CODE.getName());
            keyFileValidator.validateSessionAndAssessmentCode(fileSession, validSession, fileAssessmentCode, guid, index);
            batchFile.getAssessmentKeyData().add(getAssessmentKeyDetailRecordFromFile(ds, guid, index));
            index++;
        }
    }

    private void processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final AssessmentKeyFile batchFile, AssessmentSessionEntity validSession, AssessmentKeyFileUpload fileUpload) throws KeyFileUnProcessableException {
        var typeCode = batchFile.getAssessmentKeyData().getFirst().getAssessmentCode();
        assessmentTypeCodeRepository.findByAssessmentTypeCode(typeCode).orElseThrow(() -> new KeyFileUnProcessableException(KeyFileError.INVALID_ASSESSMENT_TYPE, guid, LOAD_FAIL));

        var assessmentEntity = assessmentRepository.findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(validSession.getSessionID(), typeCode)
                .orElseThrow(() -> new KeyFileUnProcessableException(KeyFileError.INVALID_ASSESSMENT_CODE, guid, LOAD_FAIL));

        if(!assessmentEntity.getAssessmentForms().isEmpty()) {
            List<UUID> formIds = assessmentEntity.getAssessmentForms().stream().map(AssessmentFormEntity::getAssessmentFormID).toList();
            var results = assessmentStudentRepository.findByAssessmentFormIDIn(formIds);
            if(!results.isEmpty()) {
                throw new KeyFileUnProcessableException(KeyFileError.ASSESSMENT_FORMS_HAVE_STUDENTS, guid, typeCode);
            }
        }

        if("N".equalsIgnoreCase(fileUpload.getReplaceKeyFlag()) && !assessmentEntity.getAssessmentForms().isEmpty()) {
            throw new ConfirmationRequiredException(ApiError.builder().timestamp(LocalDateTime.now()).message(typeCode).status(PRECONDITION_REQUIRED).build());
        }

        Map<String, List<AssessmentKeyDetails>> groupedData = batchFile.getAssessmentKeyData().stream().collect(Collectors.groupingBy(AssessmentKeyDetails::getFormCode));
        List<AssessmentFormEntity> formEntities = new ArrayList<>();
        for(val entry : groupedData.entrySet()) {
            AssessmentFormEntity formEntity = mapper.toFormEntity(entry.getKey(), assessmentEntity, fileUpload);
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
            formEntities.add(formEntity);
       }

        assessmentEntity.getAssessmentForms().clear();
        assessmentEntity.getAssessmentForms().addAll(formEntities);
        assessmentRepository.save(assessmentEntity);
    }

    private AssessmentComponentEntity createMultiChoiceComponent(AssessmentFormEntity assessmentFormEntity, List<AssessmentKeyDetails> multiChoice) {
        AssessmentComponentEntity multiComponentEntity = createAssessmentComponentEntity(assessmentFormEntity, "MUL_CHOICE", multiChoice.size(), multiChoice.size(), 0, 0);

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
        AtomicInteger itemCount = new AtomicInteger();

        var choiceQues = openEnded.stream().map(AssessmentKeyDetails::getItemType).filter(v -> v.contains("-C0-")).map(x -> {
            var splits = x.split("-");
            var marker = splits[3].toCharArray();
            return Integer.parseInt(String.valueOf(marker[1]));
        }).reduce(Integer::sum);

        int choiceInt = choiceQues.orElse(0);
        questionGroups.forEach(questionGroup -> {
            var nonChoiceInt = 0;
            var nonChoiceItemCount = 0;
            var nonchoiceQues = questionGroup.stream().map(AssessmentKeyDetails::getItemType).filter(v -> v.contains("-C1-")).findFirst();
            if(nonchoiceQues.isPresent()) {
                var splits = nonchoiceQues.get().split("-");
                var choiceType = splits[2].toCharArray();
                var marker = splits[3].toCharArray();
                nonChoiceInt = Integer.parseInt(String.valueOf(marker[1]));
                nonChoiceItemCount = nonChoiceInt + Integer.parseInt(String.valueOf(choiceType[1]));
            }
            markCount.set(choiceInt + nonChoiceInt);
            itemCount.set(choiceInt + nonChoiceItemCount);
        });
        AssessmentComponentEntity openEndedComponentEntity = createAssessmentComponentEntity(assessmentFormEntity, type, openEnded.size(), 0, markCount.get(), itemCount.get());

        AtomicInteger itemNumberCounter = new AtomicInteger(1);
        AtomicInteger repeatCounter = new AtomicInteger(0);
        openEnded.forEach(ques -> {
            var itemType = ques.getItemType().split("-");
            var marker = itemType[3].toCharArray();
            var index = Integer.parseInt(String.valueOf(marker[1]));
            for(int i = 0; i < index; i++) {
                final var quesEntity = mapper.toQuestionEntity(ques, openEndedComponentEntity);
                setOpenEndedWrittenQuestionEntityColumns(quesEntity, ques, questionGroups);
                setItemNumber(quesEntity, index, i, itemType, itemNumberCounter, repeatCounter);
                openEndedComponentEntity.getAssessmentQuestionEntities().add(quesEntity);
                if(i == 0 && itemType[2].equalsIgnoreCase("C1")) {
                    final var choiceEntity = createChoiceEntity(openEndedComponentEntity);
                    itemNumberCounter.getAndIncrement();
                    choiceEntity.setItemNumber(itemNumberCounter.get());
                    choiceEntity.setMasterQuestionNumber(quesEntity.getMasterQuestionNumber());
                    openEndedComponentEntity.getAssessmentChoiceEntities().add(choiceEntity);
                }
            }
        });
        return openEndedComponentEntity;
    }

    private void setItemNumberAndIncrement(AssessmentQuestionEntity questionEntity, AtomicInteger itemNumberCounter) {
        questionEntity.setItemNumber(itemNumberCounter.get());
        itemNumberCounter.getAndIncrement();
    }

    private void setItemNumber(AssessmentQuestionEntity questionEntity, int index, int i, String[] itemType, AtomicInteger itemNumberCounter, AtomicInteger repeatCounter) {
        var choiceType = itemType[2];

        if(choiceType.equalsIgnoreCase("C0")) {
            setItemNumberAndIncrement(questionEntity, itemNumberCounter);
        } else if(choiceType.equalsIgnoreCase("C1") && i > 0) {
            setItemNumberAndIncrement(questionEntity, itemNumberCounter);
        } else {
            if(i == 0) {
                repeatCounter.set(itemNumberCounter.get() - index);
            }
            questionEntity.setItemNumber(repeatCounter.get());
            repeatCounter.getAndIncrement();
        }
    }

    private AssessmentChoiceEntity createChoiceEntity(AssessmentComponentEntity openEndedComponentEntity) {
        return AssessmentChoiceEntity
                .builder()
                .assessmentComponentEntity(openEndedComponentEntity)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .createUser(openEndedComponentEntity.getCreateUser())
                .updateUser(openEndedComponentEntity.getUpdateUser())
                .build();
    }

    private AssessmentComponentEntity createAssessmentComponentEntity(final AssessmentFormEntity assessmentFormEntity, final String type, int quesCount, int numOmits, int markCount, int itemCount) {
        return  AssessmentComponentEntity
                .builder()
                .assessmentFormEntity(assessmentFormEntity)
                .componentTypeCode(type.equalsIgnoreCase("ER") || type.equalsIgnoreCase("EO") ? "OPEN_ENDED" : type)
                .componentSubTypeCode(type.equalsIgnoreCase("EO") ? "ORAL" : "NONE")
                .questionCount(quesCount)
                .numOmits(numOmits)
                .oeItemCount(itemCount)
                .oeMarkCount(markCount)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .createUser(assessmentFormEntity.getCreateUser())
                .updateUser(assessmentFormEntity.getUpdateUser())
                .build();
    }

    private void setMultiChoiceQuestionEntityColumns(AssessmentQuestionEntity keyEntity, List<AssessmentKeyDetails> multiChoiceQuesGroup, AssessmentKeyDetails value) {
        keyEntity.setQuestionNumber(StringUtils.isNotBlank(value.getQuestionNumber()) ? Integer.parseInt(value.getQuestionNumber()) : null);
        keyEntity.setItemNumber(StringUtils.isNotBlank(value.getQuestionNumber()) ? Integer.parseInt(value.getQuestionNumber()) : null);
        keyEntity.setMaxQuestionValue(StringUtils.isNotBlank(value.getMark()) && StringUtils.isNotBlank(value.getScaleFactor())
                ? new BigDecimal(value.getMark()).multiply(new BigDecimal(value.getScaleFactor()))
                : BigDecimal.ZERO);
        var lowestQues = multiChoiceQuesGroup.stream().map(AssessmentKeyDetails::getQuestionNumber).mapToInt(Integer::parseInt).min();
        keyEntity.setMasterQuestionNumber(lowestQues.getAsInt());
    }

    private void setOpenEndedWrittenQuestionEntityColumns(AssessmentQuestionEntity keyEntity, AssessmentKeyDetails value, List<List<AssessmentKeyDetails>> quesGroup) {
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

    private AssessmentKeyDetails getAssessmentKeyDetailRecordFromFile(final DataSet ds, final String guid, final long index) throws KeyFileUnProcessableException {
        final var assmtSession = StringUtils.trim(ds.getString(ASSMT_SESSION.getName()));
        if (StringUtils.isBlank(assmtSession) || assmtSession.length() > 6) {
            throw new KeyFileUnProcessableException(SESSION_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var assmtCode = StringMapper.trimAndUppercase(ds.getString(ASSMT_CODE.getName()));
        if (StringUtils.isBlank(assmtCode) || assmtCode.length() > 5) {
            throw new KeyFileUnProcessableException(ASSMT_CODE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var formCode = StringMapper.trimAndUppercase(ds.getString(FORM_CODE.getName()));
        if (StringUtils.isBlank(formCode) || formCode.length() > 1) {
            throw new KeyFileUnProcessableException(FORM_CODE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var quesNumber = StringUtils.trim(ds.getString(QUES_NUMBER.getName()));
        if (StringUtils.isBlank(quesNumber) || quesNumber.length() > 2) {
            throw new KeyFileUnProcessableException(QUES_NUM_LENGTH_ERROR, guid, String.valueOf(index + 1));
        } else if (!StringUtils.isNumeric(quesNumber)) {
            throw new KeyFileUnProcessableException(QUES_NUM_NOT_NUMERIC, guid, String.valueOf(index + 1));
        }

        final var itemType = StringMapper.trimAndUppercase(ds.getString(ITEM_TYPE.getName()));
        Pattern pattern = Pattern.compile("^E[R|O]-Q(\\d|\\d{2})-C(\\d)-M(\\d)$");
        if (StringUtils.isBlank(itemType) || (!itemType.equalsIgnoreCase("UNKNOWN") && !pattern.matcher(itemType).matches())) {
            throw new KeyFileUnProcessableException(INVALID_ITEM_TYPE, guid, String.valueOf(index + 1));
        } else if(itemType.length() > 12) {
            throw new KeyFileUnProcessableException(ITEM_TYPE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var answer = StringMapper.trimAndUppercase(ds.getString(MC_ANSWER150.getName()));
        if (StringUtils.isNotBlank(answer) && answer.length() > 150) {
            throw new KeyFileUnProcessableException(ANSWER_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var mark = StringUtils.trim(ds.getString(MARK_VALUE.getName()));
        if (StringUtils.isBlank(mark) || mark.length() > 2) {
            throw new KeyFileUnProcessableException(MARK_LENGTH_ERROR, guid, String.valueOf(index + 1));
        } else if (!StringUtils.isNumeric(mark)) {
            throw new KeyFileUnProcessableException(MARK_NOT_NUMERIC, guid, String.valueOf(index + 1));
        }

        final var cognLevel = StringMapper.trimAndUppercase(ds.getString(COGN_LEVEL.getName()));
        if(StringUtils.isNotBlank(cognLevel) && codeTableService.getAllCognitiveLevelCodes().stream().noneMatch(code -> code.getCognitiveLevelCode().equalsIgnoreCase(cognLevel))) {
            throw new KeyFileUnProcessableException(INVALID_COGNITIVE_LEVEL_CODE, guid, String.valueOf(index + 1));
        } else if(cognLevel.length() > 4) {
            throw new KeyFileUnProcessableException(COGN_LEVEL_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var taskCode = StringMapper.trimAndUppercase(ds.getString(TASK_CODE.getName()));
        if(StringUtils.isNotBlank(taskCode) && codeTableService.getAllTaskCodes().stream().noneMatch(code -> code.getTaskCode().equalsIgnoreCase(taskCode))) {
            throw new KeyFileUnProcessableException(INVALID_TASK_CODE, guid, String.valueOf(index + 1));
        } else if(StringUtils.isNotBlank(taskCode) && taskCode.length() > 2) {
            throw new KeyFileUnProcessableException(TASK_CODE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        } else if(Arrays.stream(literacyCodes).anyMatch(assmtCode::equalsIgnoreCase) && itemType.equalsIgnoreCase("UNKNOWN")
            && StringUtils.isNotBlank(taskCode)&& Arrays.stream(allowedTaskCodes).noneMatch(taskCode::equalsIgnoreCase)) {
            throw new KeyFileUnProcessableException(TASK_CODE_NOT_ALLOWED, guid, String.valueOf(index + 1));
        }

        final var claimCode = StringMapper.trimAndUppercase(ds.getString(CLAIM_CODE.getName()));
        if(StringUtils.isNotBlank(claimCode) && codeTableService.getAllClaimCodes().stream().noneMatch(code -> code.getClaimCode().equalsIgnoreCase(claimCode))) {
            throw new KeyFileUnProcessableException(INVALID_CLAIM_CODE, guid, String.valueOf(index + 1));
        } else if(StringUtils.isNotBlank(claimCode) && claimCode.length() > 3) {
            throw new KeyFileUnProcessableException(CLAIM_CODE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var contextCode = StringMapper.trimAndUppercase(ds.getString(CONTEXT_CODE.getName()));
        if(StringUtils.isNotBlank(contextCode) && codeTableService.getAllContextCodes().stream().noneMatch(code -> code.getContextCode().equalsIgnoreCase(contextCode))) {
            throw new KeyFileUnProcessableException(INVALID_CONTEXT_CODE, guid, String.valueOf(index + 1));
        } else if(StringUtils.isNotBlank(contextCode) && contextCode.length() > 1) {
            throw new KeyFileUnProcessableException(CONTEXT_CODE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var conceptsCode = StringMapper.trimAndUppercase(ds.getString(CONCEPTS_CODE.getName()));
        if(StringUtils.isNotBlank(conceptsCode) && codeTableService.getAllConceptCodes().stream().noneMatch(code -> code.getConceptCode().equalsIgnoreCase(conceptsCode))) {
            throw new KeyFileUnProcessableException(INVALID_CONCEPT_CODE, guid, String.valueOf(index + 1));
        } else if(StringUtils.isNotBlank(conceptsCode) && conceptsCode.length() > 3) {
            throw new KeyFileUnProcessableException(CONTEXT_CODE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var scale = StringMapper.trimAndUppercase(ds.getString(SCALE_FACTOR.getName()));
        if (StringUtils.isBlank(scale) || scale.length() > 8) {
            throw new KeyFileUnProcessableException(SCALE_FACTOR_LENGTH_ERROR, guid, String.valueOf(index + 1));
        } else if (!NumberUtils.isParsable(scale)) {
            throw new KeyFileUnProcessableException(SCALE_FACTOR_NOT_NUMERIC, guid, String.valueOf(index + 1));
        } else if (Double.parseDouble(scale) < -99.99 && Double.parseDouble(scale) > 99.99) {
            throw new KeyFileUnProcessableException(SCALE_FACTOR_NOT_IN_RANGE, guid, String.valueOf(index + 1));
        }

        final var assmtSection = StringMapper.trimAndUppercase(ds.getString(ASSMT_SECTION.getName()));
        if (StringUtils.isNotBlank(assmtSection) && assmtSection.length() > 8) {
            throw new KeyFileUnProcessableException(ASSMT_SECTION_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var topicType = StringMapper.trimAndUppercase(ds.getString(TOPIC_TYPE.getName()));
        if (StringUtils.isNotBlank(topicType) && topicType.length() > 1) {
            throw new KeyFileUnProcessableException(TOPIC_TYPE_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var item = StringMapper.trimAndUppercase(ds.getString(ITEM.getName()));
        if (StringUtils.isNotBlank(item) && topicType.length() > 4) {
            throw new KeyFileUnProcessableException(ITEM_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var irtColumn = StringMapper.trimAndUppercase(ds.getString(IRT_COLUMN.getName()));
        if (StringUtils.isNotBlank(irtColumn) && irtColumn.length() > 3) {
            throw new KeyFileUnProcessableException(IRT_COLUMN_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        final var quesOrigin = StringMapper.trimAndUppercase(ds.getString(QUES_ORIGIN.getName()));
        if (StringUtils.isNotBlank(quesOrigin) && quesOrigin.length() > 160) {
            throw new KeyFileUnProcessableException(QUES_ORIGIN_LENGTH_ERROR, guid, String.valueOf(index + 1));
        }

        return AssessmentKeyDetails.builder()
                .assessmentSession(assmtSession)
                .assessmentCode(assmtCode)
                .formCode(formCode)
                .questionNumber(quesNumber)
                .itemType(itemType)
                .answer(answer)
                .mark(mark)
                .cognLevel(cognLevel)
                .taskCode(taskCode)
                .claimCode(claimCode)
                .contextCode(contextCode)
                .conceptsCode(conceptsCode)
                .topicType(topicType)
                .scaleFactor(scale)
                .questionOrigin(quesOrigin)
                .item(item)
                .irt(irtColumn)
                .assessmentSection(assmtSection)
                .build();
    }
}
