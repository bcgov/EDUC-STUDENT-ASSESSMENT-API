package ca.bc.gov.educ.eas.api.struct.v1;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for Session entity.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Session extends BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ReadOnlyProperty
    private String assessmentSessionID;

    @ReadOnlyProperty
    private Integer courseSession;

    @ReadOnlyProperty
    private Integer courseMonth;

    @ReadOnlyProperty
    private Integer courseYear;

    @ReadOnlyProperty
    private String status;

    @NotNull(message = "Open Date cannot be null")
    private LocalDateTime activeFromDate;

    @NotNull(message = "Close Date cannot be null")
    private LocalDateTime activeUntilDate;

}
