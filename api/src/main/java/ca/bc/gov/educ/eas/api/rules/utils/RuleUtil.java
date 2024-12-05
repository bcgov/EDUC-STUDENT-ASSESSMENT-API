package ca.bc.gov.educ.eas.api.rules.utils;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.struct.external.studentapi.v1.Student;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class RuleUtil {

    public static boolean validateStudentSurnameMatches(AssessmentStudentEntity student, Student studentFromAPI) {
        return StringUtils.isNotBlank(studentFromAPI.getLegalLastName()) && StringUtils.isNotBlank(student.getSurName()) && studentFromAPI.getLegalLastName().equalsIgnoreCase(student.getSurName());
    }

    public static boolean validateStudentGivenNameMatches(AssessmentStudentEntity student, Student studentFromAPI) {
        return (StringUtils.isNotBlank(studentFromAPI.getLegalFirstName()) && StringUtils.isNotBlank(student.getGivenName()) && studentFromAPI.getLegalFirstName().equalsIgnoreCase(student.getGivenName())) ||
                (StringUtils.isBlank(studentFromAPI.getLegalFirstName()) && StringUtils.isBlank(student.getGivenName()));
    }

}

