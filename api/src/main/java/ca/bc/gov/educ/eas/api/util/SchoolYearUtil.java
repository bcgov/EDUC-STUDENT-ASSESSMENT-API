package ca.bc.gov.educ.eas.api.util;

public class SchoolYearUtil {

    private SchoolYearUtil() {}

    public static String generateSchoolYearString(final int schoolYearStart) {
        int schoolYearEnd = schoolYearStart + 1;
        return schoolYearStart + "/" + schoolYearEnd;
    }
}
