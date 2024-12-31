package ca.bc.gov.educ.eas.api.batch.processor;

import ca.bc.gov.educ.eas.api.batch.constants.AssessmentFileStatus;
import ca.bc.gov.educ.eas.api.batch.constants.FileType;
import ca.bc.gov.educ.eas.api.batch.exception.AssessmentKeyFileError;
import ca.bc.gov.educ.eas.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.eas.api.batch.service.AssessmentKeyFileService;
import ca.bc.gov.educ.eas.api.batch.validation.AssessmentKeyFileValidator;
import ca.bc.gov.educ.eas.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.eas.api.exception.errors.ApiError;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentKeyFileEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.service.v1.SessionService;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.eas.api.util.ValidationUtil;
import com.google.common.base.Stopwatch;
import lombok.AllArgsConstructor;
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

@AllArgsConstructor
@Component
@Slf4j
public class AssessmentKeyFileProcessor {

    public static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";
    public static final String ASSESSMENT_KEY_FILE_UPLOAD = "AssessmentKeyFileUpload";

    private final AssessmentKeyFileValidator assessmentKeyFileValidator;
    private final AssessmentKeyFileService assessmentKeyFileService;
    private final SessionService sessionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentKeyFileEntity processBatchFile(AssessmentKeyFileUpload fileUpload, String sessionID) {
        return processFile(fileUpload, sessionID);
    }

    public AssessmentKeyFileEntity processFile(AssessmentKeyFileUpload fileUpload, String sessionID) {
        val stopwatch = Stopwatch.createStarted();
        final var guid = UUID.randomUUID().toString();
        Optional<Reader> batchFileReaderOptional = Optional.empty();
        try {
            //Check Session ID
            Optional<SessionEntity> sessionEntity = sessionService.getSessionById(UUID.fromString(sessionID));
            if(sessionEntity.isEmpty()) {
                throw new FileUnProcessableException(AssessmentKeyFileError.INVALID_SESSION, guid, AssessmentFileStatus.LOAD_FAIL);
            }
            //Check file type
            FileType fileDetail = FileType.findByCode(fileUpload.getFileType()).orElseThrow(() -> new FileUnProcessableException(AssessmentKeyFileError.FILE_NOT_ALLOWED, guid, AssessmentFileStatus.LOAD_FAIL));
            final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource(fileDetail.getMapperFileName())).getFile());
            //Checks for empty file and throws exception else bytes
            var byteArrayOutputStream = new ByteArrayInputStream(assessmentKeyFileValidator.getUploadedFileBytes(guid, fileUpload, fileDetail.getCode()));
            batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
            final DataSet ds1 = DefaultParserFactory.getInstance().newDelimitedParser(mapperReader, batchFileReaderOptional.get(), '\t', '"', false).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
            //Check file extension
            assessmentKeyFileValidator.validateFileHasCorrectExtension(guid, fileUpload, fileDetail.getAllowedExtension());
            //Check file name format
            UUID assessmentID = assessmentKeyFileValidator.validateFileNameFormat(guid, fileUpload, sessionEntity.get());
            return assessmentKeyFileService.populateBatchFileAndLoadData(guid, ds1, fileUpload, assessmentID);
        } catch (FileUnProcessableException fileUnProcessableException) {
            log.error("File could not be processed exception :: {}", fileUnProcessableException);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_FILE_UPLOAD, sessionID, fileUnProcessableException.getFileError(), fileUnProcessableException.getReason());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } catch (FileNotFoundException e) {
            log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_KEY_FILE_UPLOAD, sessionID, null, AssessmentKeyFileError.GENERIC_ERROR_MESSAGE.getMessage());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } finally {
            batchFileReaderOptional.ifPresent(this::closeBatchFileReader);
            stopwatch.stop();
            log.info("Time taken for processing assessment key file is :: {} milli seconds", stopwatch.elapsed(TimeUnit.MILLISECONDS));
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
