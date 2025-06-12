package ca.bc.gov.educ.assessment.api.batch.processor;

import ca.bc.gov.educ.assessment.api.batch.exception.FileError;
import ca.bc.gov.educ.assessment.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyFile;
import ca.bc.gov.educ.assessment.api.batch.validation.FileValidator;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import com.google.common.base.Stopwatch;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.*;
import static ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentKeysBatchFileMapper.mapper;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
@Slf4j
public class AssessmentKeysProcessor {

    public static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";
    public static final String ASSESSMENT_KEY_UPLOAD = "assessmentKeyUpload";
    private final FileValidator fileValidator;

    public AssessmentKeysProcessor(FileValidator fileValidator) {
        this.fileValidator = fileValidator;
    }

    public void processAssessmentKeys(AssessmentKeyFileUpload fileUpload, String session) {
        val stopwatch = Stopwatch.createStarted();
        final var guid = UUID.randomUUID().toString();
        Optional<Reader> batchFileReaderOptional = Optional.empty();
        try {
            final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("assessmentKeyMapper.xml")).getFile());
            var byteArrayOutputStream = new ByteArrayInputStream(fileValidator.getUploadedFileBytes(guid, fileUpload));
            batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
            final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();

            populateBatchFileAndLoadData(guid, ds, fileUpload);
        } catch (final FileUnProcessableException fileUnProcessableException) {
            log.error("File could not be processed exception :: {}", fileUnProcessableException);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_UPLOAD, session, fileUnProcessableException.getReason());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } catch (final Exception e) {
            log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_UPLOAD, session , FileError.GENERIC_ERROR_MESSAGE.getMessage());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } finally {
            batchFileReaderOptional.ifPresent(this::closeBatchFileReader);
            stopwatch.stop();
            log.info("Time taken for batch processed is :: {} milli seconds", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private void populateBatchFileAndLoadData(String guid, DataSet ds, AssessmentKeyFileUpload fileUpload) throws FileUnProcessableException {
        val batchFile = new AssessmentKeyFile();
        populateAssessmentKeyFile(guid, ds, batchFile);
        //load
    }

    public void populateAssessmentKeyFile(final String guid, final DataSet ds, final AssessmentKeyFile batchFile) throws FileUnProcessableException {
        long index = 0;
        while (ds.next()) {
            batchFile.getAssessmentKeyData().add(getAssessmentKeyDetailRecordFromFile(ds));
            index++;
        }
    }

    public AssessmentQuestionEntity processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final AssessmentKeyFile batchFile, @NonNull final AssessmentKeyFileUpload fileUpload, final String schoolID, final String districtID) throws FileUnProcessableException {
        log.debug("Going to persist DEM data for batch :: {}", guid);
        List<AssessmentQuestionEntity> questionEntityList = new ArrayList<>();

        // batch file can be processed further and persisted.
        for (final var key : batchFile.getAssessmentKeyData()) {
            final var keyEntity = mapper.toKeyEntity(key);
            questionEntityList.add(keyEntity);
        }
        return craftStudentSetAndMarkInitialLoadComplete(questionEntityList);
    }

    @Retryable(retryFor = {Exception.class}, backoff = @Backoff(multiplier = 3, delay = 2000))
    public AssessmentQuestionEntity craftStudentSetAndMarkInitialLoadComplete(@NonNull final List<AssessmentQuestionEntity> incomingFilesetEntity) {
//        return incomingFilesetService.saveIncomingFilesetRecord(incomingFilesetEntity);
        return null;
    }

    private AssessmentKeyDetails getAssessmentKeyDetailRecordFromFile(final DataSet ds) {
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

    private void closeBatchFileReader(final Reader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (final IOException e) {
            log.warn("Error closing the batch file :: ", e);
        }
    }
}
