package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AssessmentKeyFileUpload extends BaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull(message = "fileName cannot be null")
    String fileName;
    @NotNull(message = "fileContents cannot be null")
    @ToString.Exclude
    String fileContents;
}
