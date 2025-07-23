package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class StudentResult extends BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "stagedStudentResultID cannot be null")
    private String stagedStudentResultID;
    @NotNull(message = "assessmentID cannot be null")
    private String assessmentID;

    private String pen;

    private String mincode;

    private String stagedStudentResultStatus;

    private String componentType;
    @NotNull(message = "assessmentFormID cannot be null")
    private String assessmentFormID;

    private String oeMarks;

    private String mcMarks;

    private String choicePath;

    private String provincialSpecialCaseCode;

    private Integer proficiencyScore;

    private String adaptedAssessmentCode;

    private String irtScore;

    private String markingSession;

}
