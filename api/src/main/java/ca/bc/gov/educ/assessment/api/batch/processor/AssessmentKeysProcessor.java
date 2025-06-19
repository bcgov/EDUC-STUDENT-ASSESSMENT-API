package ca.bc.gov.educ.assessment.api.batch.processor;

import ca.bc.gov.educ.assessment.api.batch.exception.FileError;
import ca.bc.gov.educ.assessment.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.service.AssessmentKeyService;
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
import net.sf.flatpack.StreamingDataSet;
import net.sf.flatpack.brparse.BuffReaderParseFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentKeysBatchFile.*;
import static ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentKeysBatchFileMapper.mapper;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
@Slf4j
public class AssessmentKeysProcessor {

    public static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";
    public static final String ASSESSMENT_KEY_UPLOAD = "assessmentKeyUpload";
    private final FileValidator fileValidator;
    public final AssessmentKeyService assessmentKeyService;

    public AssessmentKeysProcessor(FileValidator fileValidator, AssessmentKeyService assessmentKeyService) {
        this.fileValidator = fileValidator;
        this.assessmentKeyService = assessmentKeyService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAssessmentKeys(AssessmentKeyFileUpload fileUpload, UUID sessionID) {
        val stopwatch = Stopwatch.createStarted();
        final var guid = UUID.randomUUID().toString();
        Optional<Reader> batchFileReaderOptional = Optional.empty();
        try {
            final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("assessmentKeyMapper.xml")).getFile());
            var byteArrayOutputStream = new ByteArrayInputStream(fileValidator.getUploadedFileBytes(guid, fileUpload));
            batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
            final DataSet ds = DefaultParserFactory.getInstance()
                    .newDelimitedParser(mapperReader, batchFileReaderOptional.get(), '\t', '"', false)
                    .setStoreRawDataToDataError(true)
                    .setStoreRawDataToDataSet(true)
                    .setNullEmptyStrings(true)
                    .setIgnoreExtraColumns(true)
                    .parse();

            fileValidator.validateFileHasCorrectExtension(guid, fileUpload);
            assessmentKeyService.populateBatchFileAndLoadData(guid, ds, sessionID);
        } catch (final FileUnProcessableException fileUnProcessableException) {
            log.error("File could not be processed exception :: {}", fileUnProcessableException);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_UPLOAD, sessionID, fileUnProcessableException.getReason());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } catch (final Exception e) {
            log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_UPLOAD, sessionID , FileError.GENERIC_ERROR_MESSAGE.getMessage());
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
