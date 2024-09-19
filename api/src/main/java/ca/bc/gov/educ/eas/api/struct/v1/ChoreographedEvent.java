package ca.bc.gov.educ.eas.api.struct.v1;

import ca.bc.gov.educ.eas.api.constants.EventOutcome;
import ca.bc.gov.educ.eas.api.constants.EventType;
import lombok.Data;

@Data
public class ChoreographedEvent {
    /**
     * The Event id.
     */
    String eventID; // the primary key of student event table.
    /**
     * The Event type.
     */
    EventType eventType;
    /**
     * The Event outcome.
     */
    EventOutcome eventOutcome;
    /**
     * The Event payload.
     */
    String eventPayload;
    /**
     * The Create user.
     */
    String createUser;
    /**
     * The Update user.
     */
    String updateUser;
}
