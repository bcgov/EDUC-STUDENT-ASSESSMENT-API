package ca.bc.gov.educ.assessment.api.batch.validation;

import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileError;
import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentTypeCodeRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import lombok.extern.slf4j.Slf4j;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;

@Component
@Slf4j
public class KeyFileValidator {
    public static final String TOO_LONG = "TOO LONG";
    public static final String LOAD_FAIL = "LOADFAIL";
    public static final String LENGTH = "256";
    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;

    public KeyFileValidator(AssessmentTypeCodeRepository assessmentTypeCodeRepository) {
        this.assessmentTypeCodeRepository = assessmentTypeCodeRepository;
    }

    public byte[] getUploadedFileBytes(@NonNull final String guid, final AssessmentKeyFileUpload fileUpload) throws KeyFileUnProcessableException {
        byte[] bytes = Base64.getDecoder().decode(fileUpload.getFileContents());
        if (bytes.length == 0) {
            throw new KeyFileUnProcessableException(KeyFileError.EMPTY_FILE, guid, LOAD_FAIL);
        }
        return bytes;
    }
    public void validateFileForFormatAndLength(@NonNull final String guid, @NonNull final DataSet ds) throws KeyFileUnProcessableException {
        this.processDataSetForRowLengthErrors(guid, ds, LENGTH);
    }
    private static boolean isMalformedRowError(DataError error, String lengthError) {
        String description = error.getErrorDesc();
        return description.contains(lengthError);
    }
    private String getMalformedRowMessage(String errorDescription, DataError error, String lengthError) {
        if (errorDescription.contains(lengthError)) {
            return this.getDetailRowLengthIncorrectMessage(error, errorDescription);
        }
        return "The uploaded file contains a malformed row that could not be identified.";
    }

    public void processDataSetForRowLengthErrors(@NonNull final String guid, @NonNull final DataSet ds, @NonNull final String lengthError) throws KeyFileUnProcessableException {
        Optional<DataError> maybeError = ds
                .getErrors()
                .stream()
                .filter(error -> isMalformedRowError(error, lengthError))
                .findFirst();

        // Ignore trailer length errors due to inconsistency in flat files
        if (maybeError.isPresent() && ds.getRowCount() != maybeError.get().getLineNo()) {
            DataError error = maybeError.get();
            String message = this.getMalformedRowMessage(error.getErrorDesc(), error, lengthError);
            throw new KeyFileUnProcessableException(
                    KeyFileError.INVALID_ROW_LENGTH,
                    guid,
                    LOAD_FAIL,
                    message
            );
        }
    }

    /**
     * Gets detail row length incorrect message.
     * here 1 is subtracted from the line number as line number starts from header record and here header record
     * needs to
     * be  discarded
     *
     * @param errorDescription the {@link DataError} description
     * @param error the error
     * @return the detail row length incorrect message
     */
    public String getDetailRowLengthIncorrectMessage(final DataError error, String errorDescription) {
        if (errorDescription.contains(TOO_LONG)) {
            return "Line " + (error.getLineNo()) + " has too many characters.";
        }
        return "Line " + (error.getLineNo()) + " is missing characters.";
    }

    public void validateFileHasCorrectExtension(@NonNull final String guid, final AssessmentKeyFileUpload fileUpload) throws KeyFileUnProcessableException {
        String fileName = fileUpload.getFileName();
        int lastIndex = fileName.lastIndexOf('.');

        if(lastIndex == -1){
            throw new KeyFileUnProcessableException(KeyFileError.NO_FILE_EXTENSION, guid, LOAD_FAIL);
        }

        String extension = fileName.substring(lastIndex);

        if (!extension.equalsIgnoreCase(".txt")) {
            throw new KeyFileUnProcessableException(KeyFileError.INVALID_FILE_EXTENSION, guid, LOAD_FAIL);
        }
    }

    public void validateSessionAndAssessmentCode(String fileSession, AssessmentSessionEntity validSession, String fileAssessmentCode, String guid, long index) throws KeyFileUnProcessableException {
        var courseYear = fileSession.substring(0, 4);
        var courseMonth = fileSession.substring(4);

        if(!courseYear.equals(validSession.getCourseYear()) ||  !courseMonth.equals(validSession.getCourseMonth())){
            throw new KeyFileUnProcessableException(KeyFileError.INVALID_ASSESSMENT_KEY_SESSION, guid, String.valueOf(index + 1));
        }

        assessmentTypeCodeRepository.findByAssessmentTypeCode(fileAssessmentCode)
                .orElseThrow(() -> new KeyFileUnProcessableException(KeyFileError.INVALID_ASSESSMENT_TYPE, guid, String.valueOf(index + 1)));
    }

}
