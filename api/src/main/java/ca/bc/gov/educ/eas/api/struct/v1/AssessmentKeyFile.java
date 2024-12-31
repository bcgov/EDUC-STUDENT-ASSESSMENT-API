package ca.bc.gov.educ.eas.api.struct.v1;

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
public class AssessmentKeyFile extends BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "incomingFileID cannot be null")
    String incomingFileID;

    @NotNull(message = "sessionID cannot be null")
    String sessionID;

    String fileName;

    String fileUploadDate;
}
