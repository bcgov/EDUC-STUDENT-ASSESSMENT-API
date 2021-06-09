package ca.bc.gov.educ.api.studentassessment.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.service.StudentAssessmentService;
import ca.bc.gov.educ.api.studentassessment.util.GradValidation;
import ca.bc.gov.educ.api.studentassessment.util.ResponseHelper;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiConstants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@CrossOrigin
@RestController
@RequestMapping(StudentAssessmentApiConstants.STUDENT_ASSESSMENT_API_ROOT_MAPPING)
@EnableResourceServer
@OpenAPIDefinition(info = @Info(
        title = "API for Student Assessments.", description = "This API is for Reading Student Assessments data.", version = "1"),
        security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_GRAD_STUDENT_ASSESSMENT_DATA"})})
public class StudentAssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentAssessmentController.class);

    @Autowired
    StudentAssessmentService studentAssessmentService;
   
    @Autowired
	GradValidation validation;
   
    @Autowired
	ResponseHelper response;

    @GetMapping(StudentAssessmentApiConstants.GET_STUDENT_ASSESSMENT_BY_PEN_MAPPING)
    @PreAuthorize("#oauth2.hasScope('READ_GRAD_STUDENT_ASSESSMENT_DATA')")
    @Operation(summary = "Find All Student Assessments by PEN", description = "Get All Student Assessments by PEN", tags = { "Student Assessments" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "204", description = "NO CONTENT")})
    public ResponseEntity<List<StudentAssessment>> getStudentAssessmentByPEN(
            @PathVariable String pen, @RequestParam(value = "sortForUI",required = false,defaultValue = "false") boolean sortForUI) {
        validation.requiredField(pen, "Pen");
        if(validation.hasErrors()) {
        	validation.stopOnErrors();
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }else {
            logger.debug("#Get All Student Assessments by PEN: " + pen);
	        OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails(); 
	    	String accessToken = auth.getTokenValue();
	    	List<StudentAssessment> studentAssessmentList = studentAssessmentService.getStudentAssessmentList(pen,accessToken,sortForUI);
	    	if(studentAssessmentList.isEmpty()) {
	        	return response.NO_CONTENT();
	        }
	    	return response.GET(studentAssessmentList);
        }
    }
}
