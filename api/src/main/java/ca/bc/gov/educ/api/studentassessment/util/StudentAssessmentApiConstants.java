package ca.bc.gov.educ.api.studentassessment.util;

import java.util.Date;

public class StudentAssessmentApiConstants {

    //API end-point Mapping constants
    public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String COURSE_ACHIEVEMENT_API_ROOT_MAPPING = "/api/" + API_VERSION + "/studentcourse";
    public static final String GET_COURSE_ACHIEVEMENT_BY_ID_MAPPING = "/{studentCourseId}";
    public static final String GET_COURSE_ACHIEVEMENT_BY_PEN_MAPPING = "/pen/{pen}";

    //Attribute Constants
    public static final String COURSE_ACHIEVEMENT_ID_ATTRIBUTE = "studentCourseID";

    //Default Attribute value constants
    public static final String DEFAULT_CREATED_BY = "StudentCourseAPI";
    public static final Date DEFAULT_CREATED_TIMESTAMP = new Date();
    public static final String DEFAULT_UPDATED_BY = "StudentCourseAPI";
    public static final Date DEFAULT_UPDATED_TIMESTAMP = new Date();

    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "dd-MMM-yyyy";
}
