package ca.bc.gov.educ.assessment.api.batch.processor;

import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileError;
import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.service.AssessmentKeyService;
import ca.bc.gov.educ.assessment.api.batch.validation.KeyFileValidator;
import ca.bc.gov.educ.assessment.api.exception.ConfirmationRequiredException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

@Component
@Slf4j
public class AssessmentKeysProcessor {

    public static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";
    public static final String ASSESSMENT_KEY_UPLOAD = "assessmentKeyUpload";
    private final KeyFileValidator keyFileValidator;
    public final AssessmentKeyService assessmentKeyService;

    public AssessmentKeysProcessor(KeyFileValidator keyFileValidator, AssessmentKeyService assessmentKeyService) {
        this.keyFileValidator = keyFileValidator;
        this.assessmentKeyService = assessmentKeyService;
    }

    public void processAssessmentKeys(AssessmentKeyFileUpload fileUpload, UUID assessmentSessionID) {
        val stopwatch = Stopwatch.createStarted();
        final var guid = UUID.randomUUID().toString();
        Optional<Reader> batchFileReaderOptional = Optional.empty();
        try {
            final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("assessmentKeyMapper.xml")).getFile());
            var byteArrayOutputStream = new ByteArrayInputStream(keyFileValidator.getUploadedFileBytes(guid, fileUpload));
            batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
            final DataSet ds = DefaultParserFactory.getInstance()
                    .newDelimitedParser(mapperReader, batchFileReaderOptional.get(), '\t', '"', false)
                    .setStoreRawDataToDataError(true)
                    .setStoreRawDataToDataSet(true)
                    .setNullEmptyStrings(true)
                    .setIgnoreExtraColumns(true)
                    .parse();

            keyFileValidator.validateFileHasCorrectExtension(guid, fileUpload);
            assessmentKeyService.populateBatchFileAndLoadData(guid, ds, assessmentSessionID, fileUpload);
        } catch (final KeyFileUnProcessableException keyFileUnProcessableException) {
            log.error("File could not be processed exception :: {}", keyFileUnProcessableException);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_UPLOAD, assessmentSessionID, keyFileUnProcessableException.getReason());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } catch (final ConfirmationRequiredException e) {
            log.warn("Confirmation required while processing the file with guid :: {}", guid);
            throw new ConfirmationRequiredException(e.getError());
        } catch (final Exception e) {
            log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_UPLOAD, assessmentSessionID , KeyFileError.GENERIC_ERROR_MESSAGE.getMessage());
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
