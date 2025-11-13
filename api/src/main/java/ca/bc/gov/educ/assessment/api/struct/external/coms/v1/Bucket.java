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
public class Bucket implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bucketId;
    private String bucket;
    private String endpoint;
    private String key;
    private String secretKey;
    private String region;
    private Boolean active;
}

