package ca.bc.gov.educ.assessment.api.struct.external.sdc.v1;

import ca.bc.gov.educ.assessment.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class BaseSdcSchoolStudent extends BaseRequest {

    private String sdcSchoolCollectionStudentID;

    @NotNull(message = "sdcSchoolCollectionID cannot be null")
    private String sdcSchoolCollectionID;

    private String sdcDistrictCollectionID;

    @Size(max = 9)
    private String studentPen;

    @Size(max = 255)
    private String legalFirstName;

    @Size(max = 255)
    private String legalMiddleNames;

    @Size(max = 255)
    @NotNull(message = "legalLastName cannot be null")
    private String legalLastName;

    @Size(max = 255)
    private String usualFirstName;

    @Size(max = 255)
    private String usualMiddleNames;

    @Size(max = 255)
    private String usualLastName;

    @Size(max = 8)
    @NotNull(message = "dob cannot be null")
    private String dob;

    @Size(max = 1)
    @NotNull(message = "gender cannot be null")
    private String gender;

    @Size(max = 10)
    private String specialEducationCategoryCode;

    @Size(max = 10)
    private String schoolFundingCode;

    @Size(max = 1)
    @NotNull(message = "nativeAncestryInd cannot be null")
    private String nativeAncestryInd;

    @Size(max = 10)
    @NotNull(message = "enrolledGradeCode cannot be null")
    private String enrolledGradeCode;

    @Size(max = 10)
    @NotNull(message = "enrolledProgramCodes cannot be null")
    private String enrolledProgramCodes;

    private String schoolID;

    @Size(max = 4)
    private String bandCode;

    @Size(max = 5)
    private String isAdult;

    @Size(max = 5)
    private String isSchoolAged;

    @DecimalMin(value = "0")
    private BigDecimal fte;

    @Size(max = 10)
    private String assignedPen;

    private String assignedStudentId;

}
