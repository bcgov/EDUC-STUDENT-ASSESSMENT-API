package ca.bc.gov.educ.eas.api.batch.validation;

import ca.bc.gov.educ.eas.api.batch.constants.AssessmentFileStatus;
import ca.bc.gov.educ.eas.api.batch.constants.AssessmentKey;
import ca.bc.gov.educ.eas.api.batch.constants.AssessmentKeyConstants;
import ca.bc.gov.educ.eas.api.batch.constants.FileType;
import ca.bc.gov.educ.eas.api.batch.exception.AssessmentKeyFileError;
import ca.bc.gov.educ.eas.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.eas.api.mappers.StringMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFileUpload;
import io.micrometer.common.lang.NonNull;
import lombok.val;
import net.sf.flatpack.DataSet;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.eas.api.batch.constants.AssessmentKey.ASSMT_CODE;
import static ca.bc.gov.educ.eas.api.batch.constants.AssessmentKey.ASSMT_SESSION;

@Component
public class AssessmentKeyFileValidator {

    public byte[] getUploadedFileBytes(@NonNull final String guid, final AssessmentKeyFileUpload fileUpload, String fileType) throws FileUnProcessableException {
        byte[] bytes = Base64.getDecoder().decode(fileUpload.getFileContents());
        if (fileType.equalsIgnoreCase(FileType.ASSESSMENT_KEY.getAllowedExtension()) && bytes.length == 0) {
            throw new FileUnProcessableException(AssessmentKeyFileError.EMPTY_FILE, guid, AssessmentFileStatus.LOAD_FAIL);
        }
        return bytes;
    }

    public void validateFileHasCorrectExtension(@NonNull final String guid, final AssessmentKeyFileUpload fileUpload, String allowedExtension) throws FileUnProcessableException {
        String fileName = fileUpload.getFileName();
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            throw new FileUnProcessableException(AssessmentKeyFileError.NO_FILE_EXTENSION, guid, AssessmentFileStatus.LOAD_FAIL);
        }
        String extension = fileName.substring(lastIndex);
        if (!extension.equalsIgnoreCase(allowedExtension)) {
            throw new FileUnProcessableException(AssessmentKeyFileError.INVALID_FILE_EXTENSION, guid, AssessmentFileStatus.LOAD_FAIL);
        }
    }

    public UUID validateFileNameFormat(@NonNull final String guid, final AssessmentKeyFileUpload fileUpload, SessionEntity sessionEntity) throws FileUnProcessableException {
        String filename = StringUtils.substringBeforeLast(fileUpload.getFileName(),".");
        String[] filenameParts = StringUtils.split(filename, "_");
        if (filenameParts.length != 3 || !"TRAX".equals(filenameParts[0]) || !sessionEntity.getCourseYear().concat(sessionEntity.getCourseMonth()).equals(filenameParts[1])) {
            throw new FileUnProcessableException(AssessmentKeyFileError.INVALID_FILE_NAME, guid, AssessmentFileStatus.LOAD_FAIL);
        }
        Optional<AssessmentEntity> assessmentEntity = sessionEntity.getAssessments().stream()
                .filter(assessment -> assessment.getAssessmentTypeCode().equalsIgnoreCase(filenameParts[2]))
                .findFirst();
        return assessmentEntity.map(AssessmentEntity::getAssessmentID)
                .orElseThrow(() -> new FileUnProcessableException(AssessmentKeyFileError.INVALID_FILE_NAME, guid, AssessmentFileStatus.LOAD_FAIL));
    }

    public void validateFileUploadIsNotInProgress(@NonNull final String guid, final UUID assessmentID) throws FileUnProcessableException {
    }

    public void validateField(DataSet ds, AssessmentKey field, String expectedValue, String guid, Long rowIndex) throws FileUnProcessableException {
        if (!ds.getString(field.getName()).equals(expectedValue)) {
            throw new FileUnProcessableException(AssessmentKeyFileError.INVALID_DATA, guid, AssessmentFileStatus.LOAD_FAIL, field.name(), rowIndex.toString());
        }
    }

    public String validateAndGetStringValue(AssessmentKey fieldName, String guid, DataSet ds, Long index) throws FileUnProcessableException {
        Long rowIndex = index + 1;
        val value = StringMapper.trimAndUppercase(ds.getString(fieldName.getName()));
        if (!isAllowedValue(fieldName, value)) {
            throw new FileUnProcessableException(AssessmentKeyFileError.INVALID_DATA, guid, AssessmentFileStatus.LOAD_FAIL, fieldName.name(), rowIndex.toString());
        }
        return value;
    }

    private boolean isAllowedValue(AssessmentKey fieldName, String value) {
        return switch (fieldName) {
            case CLAIM_CODE -> EnumUtils.isValidEnum(AssessmentKeyConstants.ClaimCode.class, value);
            default -> true;
        };
    }

}
