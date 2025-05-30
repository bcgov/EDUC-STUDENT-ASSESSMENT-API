package ca.bc.gov.educ.assessment.api.struct.external.institute.v1;

import ca.bc.gov.educ.assessment.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * The type Student.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Note extends BaseRequest implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  private String noteId;

  private String schoolId;

  private String districtId;

  private String independentAuthorityId;

  @Size(max = 4000)
  @NotNull(message = "content cannot be null")
  private String content;

}
