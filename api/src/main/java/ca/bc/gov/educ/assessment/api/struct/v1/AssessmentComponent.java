package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentComponent extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String assessmentComponentID;
    private String assessmentFormID;
    private String componentTypeCode;
    private String componentSubTypeCode;
    private Integer questionCount;
    private Integer numOmits;
    private Integer oeItemCount;
    private Integer oeMarkCount;
    private List<AssessmentQuestion> assessmentQuestions;
}
