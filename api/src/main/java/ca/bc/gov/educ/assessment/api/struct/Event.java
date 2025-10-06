package ca.bc.gov.educ.assessment.api.struct;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * The type Event.
 */
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  private EventType eventType;
  private EventOutcome eventOutcome;
  private UUID sagaId;
  private String replyTo;
  private String eventPayload; // json string
  private String stagedStudentResultID;
  private String assessmentStudentID;
}
