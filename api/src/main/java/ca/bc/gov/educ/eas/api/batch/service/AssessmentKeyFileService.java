package ca.bc.gov.educ.eas.api.batch.service;

import ca.bc.gov.educ.eas.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.eas.api.batch.mappers.AssessmentKeyFileMapper;
import ca.bc.gov.educ.eas.api.batch.struct.AssessmentKeyDetail;
import ca.bc.gov.educ.eas.api.batch.struct.AssessmentKeyFile;
import ca.bc.gov.educ.eas.api.batch.validation.AssessmentKeyFileValidator;
import ca.bc.gov.educ.eas.api.mappers.StringMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentKeyEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentKeyFileEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFileUpload;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ca.bc.gov.educ.eas.api.batch.constants.AssessmentKey.*;

@AllArgsConstructor
@Service
@Slf4j
public class AssessmentKeyFileService {

    private static final AssessmentKeyFileMapper mapper = AssessmentKeyFileMapper.mapper;
    private final AssessmentKeyFileValidator assessmentKeyFileValidator;

    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentKeyFileEntity populateBatchFileAndLoadData(String guid, final DataSet ds, final AssessmentKeyFileUpload fileUpload, final UUID assessmentID) throws FileUnProcessableException {
        val assessmentKeyFile = new AssessmentKeyFile();
        String[] filenameParts = StringUtils.split(StringUtils.substringBeforeLast(fileUpload.getFileName(),"."), "_");
        this.populateBatchFile(guid, ds, filenameParts[1], filenameParts[2], assessmentKeyFile);
        assessmentKeyFileValidator.validateFileUploadIsNotInProgress(guid, assessmentID);
        return this.processLoadedRecordsInBatchFile(guid, assessmentKeyFile, fileUpload, assessmentID);
    }

    public AssessmentKeyFileEntity processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final AssessmentKeyFile assessmentKeyFile, @NonNull final AssessmentKeyFileUpload fileUpload, final UUID assessmentID) throws FileUnProcessableException {
        log.debug("Going to persist TAB data for batch :: {}", guid);
        final AssessmentKeyFileEntity entity = mapper.toIncomingAssessmentKeyEntity(fileUpload, assessmentID);

        for (final var assessmentKey : assessmentKeyFile.getAssessmentKeyData()) {
            entity.getAssessmentKeyEntities().add(mapper.toAssessmentKeyEntity(assessmentKey, entity));
        }
        return craftAssessmentKeyAndMarkInitialLoadComplete(entity, assessmentID);
    }

    //@Retryable(retryFor = {Exception.class}, backoff = @Backoff(multiplier = 3, delay = 2000))
    public AssessmentKeyFileEntity craftAssessmentKeyAndMarkInitialLoadComplete(@NonNull final AssessmentKeyFileEntity incomingFilesetEntity, @NonNull final UUID assessmentID) {
        //TO BE IMPLEMENTED
        return null;
    }


    public void populateBatchFile(final String guid, final DataSet ds, final String sessionID, final String assessmentType,final AssessmentKeyFile batchFile) throws FileUnProcessableException {
        long index = 0;
        while (ds.next()) {
            validateAssessment(guid, ds, sessionID,assessmentType, index);
            batchFile.getAssessmentKeyData().add(this.getAssessmentKeyRecordFromFile(ds, guid, index));
            index++;
        }
    }

    private boolean validateAssessment(String guid, final DataSet ds, final String sessionID, final String assessmentType, Long index) throws FileUnProcessableException {
        Long rowIndex = index + 1;
        assessmentKeyFileValidator.validateField(ds, ASSMT_SESSION, sessionID, guid, rowIndex);
        assessmentKeyFileValidator.validateField(ds, ASSMT_CODE, assessmentType, guid, rowIndex);
        return true;
    }

    private AssessmentKeyDetail getAssessmentKeyRecordFromFile(final DataSet ds, final String guid, final long index) throws FileUnProcessableException {
        return AssessmentKeyDetail.builder()
                .assessmentSession(assessmentKeyFileValidator.validateAndGetStringValue(ASSMT_SESSION, guid, ds, index))
                .assessmentTypeCode(assessmentKeyFileValidator.validateAndGetStringValue(ASSMT_CODE, guid, ds, index))
                .formCode(assessmentKeyFileValidator.validateAndGetStringValue(FORM_CODE, guid, ds, index))
                .questionNumber(Integer.valueOf(assessmentKeyFileValidator.validateAndGetStringValue(QUES_NUMBER, guid, ds, index)))
                .itemType(assessmentKeyFileValidator.validateAndGetStringValue(ITEM_TYPE, guid, ds, index))
                .multipleChoiceAnswer(assessmentKeyFileValidator.validateAndGetStringValue(MC_ANSWER150, guid, ds, index))
                .markValue(Integer.valueOf(StringMapper.trimAndUppercase(ds.getString(MARK_VALUE.getName()))))
                .cognitiveLevel(assessmentKeyFileValidator.validateAndGetStringValue(COGN_LEVEL, guid, ds, index))
                .taskCode(assessmentKeyFileValidator.validateAndGetStringValue(TASK_CODE, guid, ds, index))
                .claimCode(assessmentKeyFileValidator.validateAndGetStringValue(CLAIM_CODE, guid, ds, index))
                .conceptsCode(assessmentKeyFileValidator.validateAndGetStringValue(CONCEPTS_CODE, guid, ds, index))
                .topicType(assessmentKeyFileValidator.validateAndGetStringValue(TOPIC_TYPE, guid, ds, index))
                .scaleFactor(assessmentKeyFileValidator.validateAndGetStringValue(SCALE_FACTOR, guid, ds, index))
                .questionOrigin(assessmentKeyFileValidator.validateAndGetStringValue(QUES_ORIGIN, guid, ds, index))
                .item(assessmentKeyFileValidator.validateAndGetStringValue(ITEM, guid, ds, index))
                .irtColumn(Integer.valueOf(assessmentKeyFileValidator.validateAndGetStringValue(IRT_COLUMN, guid, ds, index)))
                .assessmentSection(assessmentKeyFileValidator.validateAndGetStringValue(ASSMT_SECTION, guid, ds, index))
                .build();
    }


}
