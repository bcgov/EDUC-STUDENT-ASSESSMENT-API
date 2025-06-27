package ca.bc.gov.educ.assessment.api.batch.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The type File un processable exception.
 */
@Slf4j
public class ResultsFileUnProcessableException extends Exception {

  /**
   * The constant GUID_IS.
   */
  public static final String GUID_IS = " guid is :: ";
  private static final long serialVersionUID = -3024811399248399591L;
  /**
   * The File error.
   */
  @Getter
  private final ResultFileError resultFileError;
  /**
   * The Reason.
   */
  @Getter
  private final String reason;

  /**
   * Instantiates a new File un processable exception.
   *
   * @param resultFileError                 the file error
   * @param guid                      the guid
   * @param messageArgs               the message args
   */
  public ResultsFileUnProcessableException(final ResultFileError resultFileError, final String guid, final String... messageArgs) {
    super(resultFileError.getMessage() + GUID_IS + guid);
    this.resultFileError = resultFileError;
    var finalLogMessage = resultFileError.getMessage();
    if (messageArgs != null) {
      finalLogMessage = getFormattedMessage(finalLogMessage, messageArgs);
    }
    log.error(finalLogMessage + GUID_IS + guid);
    this.reason = finalLogMessage;
  }

  /**
   * Gets formatted message.
   *
   * @param msg           the msg
   * @param substitutions the substitutions
   * @return the formatted message
   */
  private static String getFormattedMessage(final String msg, final String... substitutions) {
    final String format = msg.replace("$?", "%s");
    return String.format(format, (Object[]) substitutions);
  }
}
