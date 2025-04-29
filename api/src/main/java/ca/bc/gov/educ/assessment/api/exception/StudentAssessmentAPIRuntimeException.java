package ca.bc.gov.educ.assessment.api.exception;

public class StudentAssessmentAPIRuntimeException extends RuntimeException {

  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 5241655513745148898L;

  public StudentAssessmentAPIRuntimeException(String message) {
		super(message);
	}

  public StudentAssessmentAPIRuntimeException(Throwable exception) {
    super(exception);
  }

}
