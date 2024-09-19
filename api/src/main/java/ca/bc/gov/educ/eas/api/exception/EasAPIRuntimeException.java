package ca.bc.gov.educ.eas.api.exception;

/**
 * The type EAS api runtime exception.
 */
public class EasAPIRuntimeException extends RuntimeException {

  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 5241655513745148898L;

  /**
   * Instantiates a new EAS api runtime exception.
   *
   * @param message the message
   */
  public EasAPIRuntimeException(String message) {
		super(message);
	}

  public EasAPIRuntimeException(Throwable exception) {
    super(exception);
  }

}
