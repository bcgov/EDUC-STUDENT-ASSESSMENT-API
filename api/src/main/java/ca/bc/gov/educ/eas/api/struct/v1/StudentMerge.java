package ca.bc.gov.educ.eas.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * The type Student merge.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentMerge extends BaseRequest implements Serializable {
    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The Student merge id.
     */
    @NotNull(message = "Student Merge ID cannot be null")
    String studentMergeID;
    /**
     * The Student id.
     */
    @NotNull(message = "Student ID cannot be null.")
    String studentID;
    /**
     * The Merge student id.
     */
    @NotNull(message = "Merge Student ID cannot be null.")
    String mergeStudentID;
    /**
     * The Student merge direction code.
     */
    @NotNull(message = "Student Merge Direction Code cannot be null.")
    String studentMergeDirectionCode;
    /**
     * The Student merge source code.
     */
    @NotNull(message = "Student Merge Source Code cannot be null.")
    String studentMergeSourceCode;
}
