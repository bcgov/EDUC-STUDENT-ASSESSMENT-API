package ca.bc.gov.educ.api.studentassessment.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.service.StudentAssessmentService;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiConstants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@CrossOrigin
@RestController
@RequestMapping(StudentAssessmentApiConstants.STUDENT_ASSESSMENT_API_ROOT_MAPPING)
@EnableResourceServer
@OpenAPIDefinition(info = @Info(title = "API for Code Data.", description = "This Read API is for Reading Code data.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_GRAD_STUDENT_ASSESSMENT_DATA"})})
public class StudentAssessmentController {

    private static Logger logger = LoggerFactory.getLogger(StudentAssessmentController.class);

   @Autowired
    StudentAssessmentService studentAssessmentService;

    @GetMapping(StudentAssessmentApiConstants.GET_STUDENT_ASSESSMENT_BY_PEN_MAPPING)
    @PreAuthorize("#oauth2.hasScope('READ_GRAD_STUDENT_ASSESSMENT_DATA')")
    public List<StudentAssessment> getStudentAssessmentByPEN(@PathVariable String pen) {
        logger.debug("#Get All Student Assessments by PEN: " + pen);
        OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails(); 
    	String accessToken = auth.getTokenValue();
        return studentAssessmentService.getStudentAssessmentList(pen,accessToken);
    }
}
