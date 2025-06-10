package ca.bc.gov.educ.assessment.api.batch.validation;

import ca.bc.gov.educ.assessment.api.batch.exception.FileError;
import ca.bc.gov.educ.assessment.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import lombok.extern.slf4j.Slf4j;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.Record;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;

@Component
@Slf4j
public class FileValidator {
    public static final String TOO_LONG = "TOO LONG";
    public static final String MINCODE = "mincode";
    public static final String LOAD_FAIL = "LOADFAIL";
    private final RestUtils restUtils;

    public FileValidator(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    public byte[] getUploadedFileBytes(@NonNull final String guid, final AssessmentKeyFileUpload fileUpload) throws FileUnProcessableException {
        byte[] bytes = Base64.getDecoder().decode(fileUpload.getFileContents());
        if (bytes.length == 0) {
            throw new FileUnProcessableException(FileError.EMPTY_FILE, guid, LOAD_FAIL);
        }
        return bytes;
    }
    public void validateFileForFormatAndLength(@NonNull final String guid, @NonNull final DataSet ds, @NonNull final String lengthError) throws FileUnProcessableException {
        this.processDataSetForRowLengthErrors(guid, ds, lengthError);
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

    public void processDataSetForRowLengthErrors(@NonNull final String guid, @NonNull final DataSet ds, @NonNull final String lengthError) throws FileUnProcessableException {
        Optional<DataError> maybeError = ds
                .getErrors()
                .stream()
                .filter(error -> isMalformedRowError(error, lengthError))
                .findFirst();

        // Ignore trailer length errors due to inconsistency in flat files
        if (maybeError.isPresent() && ds.getRowCount() != maybeError.get().getLineNo()) {
            DataError error = maybeError.get();
            String message = this.getMalformedRowMessage(error.getErrorDesc(), error, lengthError);
            throw new FileUnProcessableException(
                    FileError.INVALID_ROW_LENGTH,
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

    public void validateFileHasCorrectExtension(@NonNull final String guid, final AssessmentKeyFileUpload fileUpload, String allowedExtension) throws FileUnProcessableException {
        String fileName = fileUpload.getFileName();
        int lastIndex = fileName.lastIndexOf('.');

        if(lastIndex == -1){
            throw new FileUnProcessableException(FileError.NO_FILE_EXTENSION, guid, LOAD_FAIL);
        }

        String extension = fileName.substring(lastIndex);

        if (!extension.equalsIgnoreCase(allowedExtension)) {
            throw new FileUnProcessableException(FileError.INVALID_FILE_EXTENSION, guid, LOAD_FAIL);
        }
    }

    public void validateMincode(@NonNull final String guid, final String schoolID, String fileMincode) throws FileUnProcessableException {
        String schoolMincode = getMincode(guid, schoolID);
        log.debug("Checking valid mincode for school ID {} :: and file mincode is listed: {} :: fetched mincode is: {}", schoolID, fileMincode, schoolMincode);
        if (StringUtils.isBlank(schoolMincode) || StringUtils.isBlank(fileMincode) || !fileMincode.equals(schoolMincode)) {
            throw new FileUnProcessableException(FileError.MINCODE_MISMATCH, guid,LOAD_FAIL,schoolMincode);
        }
    }
    public String getMincode(@NonNull final String guid,final String schoolID) throws FileUnProcessableException {
        Optional<SchoolTombstone> schoolOptional = restUtils.getSchoolBySchoolID(schoolID);
        SchoolTombstone school = schoolOptional.orElseThrow(() -> new FileUnProcessableException(FileError.INVALID_SCHOOL, guid, LOAD_FAIL, schoolID));
        return school.getMincode();
    }

    public SchoolTombstone getSchoolFromFileMincodeField(final String guid, final DataSet ds) throws FileUnProcessableException {
        var mincode = getSchoolMincode(guid, ds);
        var school = getSchoolUsingMincode(mincode);
        return school.orElseThrow(() -> new FileUnProcessableException(FileError.INVALID_SCHOOL, guid, LOAD_FAIL, mincode));
    }

    public SchoolTombstone getSchoolFromFileName(final String guid, String fileName) throws FileUnProcessableException {
        String mincode = fileName.split("\\.")[0];
        var school = getSchoolUsingMincode(mincode);
        return school.orElseThrow(() -> new FileUnProcessableException(FileError.INVALID_FILENAME, guid, LOAD_FAIL));
    }

    public String getSchoolMincode(final String guid, @NonNull final DataSet ds) throws FileUnProcessableException{
        ds.goTop();
        ds.next();

        Optional<Record> firstRow = ds.getRecord();
        String mincode = firstRow.map(row -> row.getString(MINCODE)).orElse(null);

        if(StringUtils.isBlank(mincode)){
            throw new FileUnProcessableException(FileError.MISSING_MINCODE, guid, LOAD_FAIL);
        }
        ds.goTop();
        return mincode;
    }

    public Optional<SchoolTombstone> getSchoolUsingMincode(final String mincode) {
        return restUtils.getSchoolByMincode(mincode);
    }

    public Optional<SchoolTombstone> getSchool(final String schoolID) {
        return restUtils.getSchoolBySchoolID(schoolID);
    }
}
