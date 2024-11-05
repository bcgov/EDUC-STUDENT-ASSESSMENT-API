package ca.bc.gov.educ.eas.api.constants.v1;

/**
 * This class consolidates all the URL exposed by EAS service.
 */
public final class URL {

  private URL(){
  }

  public static final String BASE_URL= "/api/v1/eas";
  public static final String PAGINATED="/paginated";
  public static final String BASE_URL_STUDENT = BASE_URL + "/student";  
  public static final String SESSIONS_URL = BASE_URL + "/sessions";
  public static final String ASSESSMENTS_URL = BASE_URL + "/assessments";


}
