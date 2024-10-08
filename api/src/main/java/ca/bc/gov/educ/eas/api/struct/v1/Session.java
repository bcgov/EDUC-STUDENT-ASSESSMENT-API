package ca.bc.gov.educ.eas.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session extends BaseRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  private String sessionID;

  @Size(max = 6)
  private int courseSession;

  @Size(max = 4)
  private int courseYear;

  @Size(max =  2)
  private int courseMonth;

  @Size(max =  10)
  @NotNull(message = "statusCode cannot be null")
  private String statusCode;

  @NotNull(message = "activeFromDate cannot be null")
  private LocalDateTime activeFromDate;

  @NotNull(message = "activeUntilDate cannot be null")
  private LocalDateTime activeUntilDate;
}
