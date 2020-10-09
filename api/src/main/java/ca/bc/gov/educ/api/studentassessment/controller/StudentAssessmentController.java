package ca.bc.gov.educ.api.studentassessment.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.service.StudentAssessmentService;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiConstants;

@CrossOrigin
@RestController
@RequestMapping(StudentAssessmentApiConstants.STUDENT_ASSESSMENT_API_ROOT_MAPPING)
public class StudentAssessmentController {

    private static Logger logger = LoggerFactory.getLogger(StudentAssessmentController.class);

   @Autowired
    StudentAssessmentService studentAssessmentService;

    @GetMapping(StudentAssessmentApiConstants.GET_STUDENT_ASSESSMENT_BY_PEN_MAPPING)
    public List<StudentAssessment> getStudentAssessmentByPEN(@PathVariable String pen) {
        logger.debug("#Get All Course Achievements by PEN: " + pen);
        return studentAssessmentService.getStudentAssessmentList(pen);
    }
}
