package ca.bc.gov.educ.api.studentassessment.service;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.studentassessment.model.dto.Assessment;
import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.transformer.StudentAssessmentTransformer;
import ca.bc.gov.educ.api.studentassessment.repository.StudentAssessmentRepository;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiConstants;

@Service
public class StudentAssessmentService {

    @Autowired
    private StudentAssessmentRepository studentAssessmentRepo;

    @Autowired
    private StudentAssessmentTransformer studentAssessmentTransformer;
    
    @Autowired
    RestTemplate restTemplate;
    
    @Value(StudentAssessmentApiConstants.ENDPOINT_ASSESSMENT_BY_ASSMT_CODE_URL)
    private String getAssessmentByAssmtCodeURL;

    private static Logger logger = LoggerFactory.getLogger(StudentAssessmentService.class);

     /**
     * Get all student assessments by PEN populated in Student Assessment DTO
     *
     * @return Student Assessment 
     * @throws java.lang.Exception
     */
    public List<StudentAssessment> getStudentAssessmentList(String pen) {
        List<StudentAssessment> studentAssessment  = new ArrayList<StudentAssessment>();

        try {
        	studentAssessment = studentAssessmentTransformer.transformToDTO(studentAssessmentRepo.findByPen(pen));
        	studentAssessment.forEach(sA -> {
        		Assessment assessment = restTemplate.getForObject(String.format(getAssessmentByAssmtCodeURL, sA.getAssessmentCode()), Assessment.class);
        		if(assessment != null) {
        			sA.setAssessmentName(assessment.getAssessmentName());
        		}
        	});
            logger.debug(studentAssessment.toString());
        } catch (Exception e) {
            logger.debug("Exception:" + e);
        }

        return studentAssessment;
    }
}
