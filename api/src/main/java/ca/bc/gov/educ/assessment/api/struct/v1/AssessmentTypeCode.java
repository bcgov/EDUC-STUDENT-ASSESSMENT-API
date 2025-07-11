package ca.bc.gov.educ.assessment.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class AssessmentTypeCode extends BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ReadOnlyProperty
    private String assessmentTypeCode;

    @ReadOnlyProperty
    private String label;

    @ReadOnlyProperty
    private Integer displayOrder;

    @ReadOnlyProperty
    private String language;

    @ReadOnlyProperty
    private String effectiveDate;

    @ReadOnlyProperty
    private String expiryDate;

}
