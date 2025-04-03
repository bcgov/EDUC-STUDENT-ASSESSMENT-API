package ca.bc.gov.educ.eas.api.rules.utils;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.eas.api.struct.external.studentapi.v1.Student;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Slf4j
public class RuleUtil {

    public static boolean validateStudentSurnameMatches(AssessmentStudentEntity student, Student studentFromAPI) {
        return StringUtils.isNotBlank(studentFromAPI.getLegalLastName()) && StringUtils.isNotBlank(student.getSurname()) && studentFromAPI.getLegalLastName().equalsIgnoreCase(student.getSurname());
    }

    public static boolean validateStudentGivenNameMatches(AssessmentStudentEntity student, Student studentFromAPI) {
        return (StringUtils.isNotBlank(studentFromAPI.getLegalFirstName()) && StringUtils.isNotBlank(student.getGivenName()) && studentFromAPI.getLegalFirstName().equalsIgnoreCase(student.getGivenName())) ||
                (StringUtils.isBlank(studentFromAPI.getLegalFirstName()) && StringUtils.isBlank(student.getGivenName()));
    }

    public static boolean isSchoolValid(SchoolTombstone school){
        var currentDate = LocalDateTime.now();
        LocalDateTime openDate = null;
        LocalDateTime closeDate = null;
        try {
            openDate = LocalDateTime.parse(school.getOpenedDate());

            if (openDate.isAfter(currentDate)){
                return false;
            }

            if(school.getClosedDate() != null) {
                closeDate = LocalDateTime.parse(school.getClosedDate());
            }else{
                closeDate = LocalDateTime.now().plusDays(5);
            }
        } catch (DateTimeParseException e) {
            return false;
        }

        if (!(openDate.isBefore(currentDate) && closeDate.isAfter(currentDate))) {
            return false;
        }
    return true;
    }

}

