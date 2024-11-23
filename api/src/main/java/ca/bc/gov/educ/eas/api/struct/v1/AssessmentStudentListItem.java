package ca.bc.gov.educ.eas.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.ReadOnlyProperty;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentStudentListItem extends AssessmentStudent {

    @ReadOnlyProperty
    private String sessionID;

    @ReadOnlyProperty
    private String assessmentTypeCode;

    @ReadOnlyProperty
    private String courseMonth;

    @ReadOnlyProperty
    private String courseYear;

    @ReadOnlyProperty
    private Integer numberOfAttempts;

}
