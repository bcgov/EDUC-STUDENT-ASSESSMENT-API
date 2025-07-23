package ca.bc.gov.educ.assessment.api.struct.v1;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class ProvincialSpecialCaseCode extends BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ReadOnlyProperty
    private String provincialSpecialCaseCode;

    @ReadOnlyProperty
    private String label;

    @ReadOnlyProperty
    private Integer displayOrder;

}
