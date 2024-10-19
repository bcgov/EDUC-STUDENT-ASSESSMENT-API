package ca.bc.gov.educ.eas.api.constants;

public enum EventType {
  INITIATED, //Default. Used by BaseOrchestrator.
  MARK_SAGA_COMPLETE, //Default. Used by BaseOrchestrator.
  GET_PAGINATED_SCHOOLS,
  CREATE_STUDENT_REGISTRATION,
  GET_STUDENT_REGISTRATION,
  GET_OPEN_ASSESSMENT_SESSIONS
}
