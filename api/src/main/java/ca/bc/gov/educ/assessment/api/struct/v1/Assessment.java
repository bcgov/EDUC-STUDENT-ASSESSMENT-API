package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Assessment extends BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ReadOnlyProperty
    private String assessmentID;

    @ReadOnlyProperty
    private String sessionID;

    @ReadOnlyProperty
    private String assessmentTypeCode;

}
