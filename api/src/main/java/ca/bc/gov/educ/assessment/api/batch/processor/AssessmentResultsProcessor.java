package ca.bc.gov.educ.assessment.api.batch.processor;

import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileError;
import ca.bc.gov.educ.assessment.api.batch.exception.ResultsFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.service.AssessmentResultService;
import ca.bc.gov.educ.assessment.api.batch.validation.ResultsFileValidator;
import ca.bc.gov.educ.assessment.api.exception.ConfirmationRequiredException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
@Slf4j
public class AssessmentResultsProcessor {

    public static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";
    public static final String ASSESSMENT_RESULT_UPLOAD = "assessmentResultUpload";
    private final ResultsFileValidator resultsFileValidator;
    private final AssessmentResultService assessmentResultService;

    public AssessmentResultsProcessor(ResultsFileValidator resultsFileValidator, AssessmentResultService assessmentResultService) {
        this.resultsFileValidator = resultsFileValidator;
        this.assessmentResultService = assessmentResultService;
    }

    public void processAssessmentResults(AssessmentResultFileUpload fileUpload, UUID assessmentSessionID) {
        val stopwatch = Stopwatch.createStarted();
        final var correlationID = UUID.randomUUID().toString();
        Optional<Reader> batchFileReaderOptional = Optional.empty();
        try {
            final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("assessmentResultMapper.xml")).getFile());
            var byteArrayOutputStream = new ByteArrayInputStream(resultsFileValidator.getUploadedFileBytes(correlationID, fileUpload));
            batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
            final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();

            resultsFileValidator.validateFileForFormatAndLength(correlationID, ds, "SHOULD BE 486");
            assessmentResultService.populateBatchFileAndLoadData(correlationID, ds, assessmentSessionID, fileUpload);
        } catch (final ResultsFileUnProcessableException resultsFileUnProcessableException) {
            log.error("File could not be processed exception :: {}", resultsFileUnProcessableException);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_RESULT_UPLOAD, assessmentSessionID, resultsFileUnProcessableException.getReason());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } catch (final ConfirmationRequiredException e) {
            log.warn("Confirmation required while processing the file");
            throw new ConfirmationRequiredException(e.getError());
        } catch (final Exception e) {
            log.error("Exception while processing the file with correlationID :: {} :: Exception :: {}", correlationID, e);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_RESULT_UPLOAD, assessmentSessionID , KeyFileError.GENERIC_ERROR_MESSAGE.getMessage());
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