package ca.bc.gov.educ.eas.api.struct.v1;

import ca.bc.gov.educ.eas.api.constants.v1.CourseStatusCodes;
import ca.bc.gov.educ.eas.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.eas.api.struct.OnUpdate;
import ca.bc.gov.educ.eas.api.validator.constraint.IsAllowedValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentStudent extends BaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(groups = OnUpdate.class, message = "assessmentStudentID cannot be null ")
    private String assessmentStudentID;

    @NotBlank(message = "assessmentID cannot be null")
    private String assessmentID;

    @NotBlank(message = "schoolID cannot be null")
    private String schoolID;

    @NotBlank(message = "studentID cannot be null")
    private String studentID;

    @NotBlank(message = "pen cannot be null")
    @Size(max = 9)
    private String pen;

    @Size(max = 12)
    private String localID;

    private Boolean isElectronicExam;

    @Size(max = 1)
    private Integer proficiencyScore;

    @Size(max = 1)
    @IsAllowedValue(enumClass = ProvincialSpecialCaseCodes.class, message = "Invalid provincial special case code.")
    private String provincialSpecialCaseCode;

    @Size(max = 1)
    @IsAllowedValue(enumClass = CourseStatusCodes.class, message = "Invalid course status code.")
    private String courseStatusCode;
}
