package ca.bc.gov.educ.api.studentassessment.service;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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
    
    @Autowired
    WebClient webClient;
    
    @Value(StudentAssessmentApiConstants.ENDPOINT_ASSESSMENT_BY_ASSMT_CODE_URL)
    private String getAssessmentByAssmtCodeURL;

    private static Logger logger = LoggerFactory.getLogger(StudentAssessmentService.class);

     /**
     * Get all student assessments by PEN populated in Student Assessment DTO
     * @param accessToken 
     *
     * @return Student Assessment 
     * @throws java.lang.Exception
     */
    public List<StudentAssessment> getStudentAssessmentList(String pen, String accessToken,boolean sortForUI) {
        List<StudentAssessment> studentAssessment  = new ArrayList<StudentAssessment>();
        try {
        	studentAssessment = studentAssessmentTransformer.transformToDTO(studentAssessmentRepo.findByPen(pen));
        	studentAssessment.forEach(sA -> {
        		Assessment assessment = webClient.get().uri(String.format(getAssessmentByAssmtCodeURL, sA.getAssessmentCode().trim())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(Assessment.class).block();
        		if(assessment != null) {
        			sA.setAssessmentName(assessment.getAssessmentName());
        			sA.setAssessmentDetails(assessment);
        		}
        	});
            logger.debug(studentAssessment.toString());
        } catch (Exception e) {
            logger.debug("Exception:" + e);
        }
        if(sortForUI) {
        Collections.sort(studentAssessment, Comparator.comparing(StudentAssessment::getPen)
                .thenComparing(StudentAssessment::getAssessmentCode)
                .thenComparing(StudentAssessment::getSessionDate));
        }
        return studentAssessment;
    }
}
