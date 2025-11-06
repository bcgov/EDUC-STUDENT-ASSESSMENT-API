package ca.bc.gov.educ.assessment.api.struct.external.coms.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String path;
    private String bucketId;
    private String name;
    private Long size;
    private String mimeType;
}

