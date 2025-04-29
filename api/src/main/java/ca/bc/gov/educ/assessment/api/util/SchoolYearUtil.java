package ca.bc.gov.educ.assessment.api.util;

public class SchoolYearUtil {

    private SchoolYearUtil() {}

    public static String generateSchoolYearString(final int schoolYearStart) {
        int schoolYearEnd = schoolYearStart + 1;
        return schoolYearStart + "/" + schoolYearEnd;
    }
}
