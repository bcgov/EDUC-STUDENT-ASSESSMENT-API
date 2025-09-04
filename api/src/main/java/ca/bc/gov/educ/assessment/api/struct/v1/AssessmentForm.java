package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentForm extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @ReadOnlyProperty
    private String assessmentFormID;
    @ReadOnlyProperty
    private String assessmentID;
    @ReadOnlyProperty
    private String formCode;
//    @ReadOnlyProperty
//    private List<AssessmentComponent> assessmentComponents;
}
