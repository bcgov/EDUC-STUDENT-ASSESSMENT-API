package ca.bc.gov.educ.api.studentassessment.service;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.studentassessment.model.dto.Assessment;
import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.transformer.AssessmentTransformer;
import ca.bc.gov.educ.api.studentassessment.model.transformer.StudentAssessmentTransformer;
import ca.bc.gov.educ.api.studentassessment.repository.AssessmentRepository;
import ca.bc.gov.educ.api.studentassessment.repository.StudentAssessmentRepository;

@Service
public class StudentAssessmentService {

    @Autowired
    private StudentAssessmentRepository studentAssessmentRepo;

    @Autowired
    private StudentAssessmentTransformer studentAssessmentTransformer;
    
    @Autowired
    private AssessmentRepository assessmentRepo;  

    @Autowired
    private AssessmentTransformer assessmentTransformer;

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
        		Assessment assessment = assessmentTransformer.transformToDTO(assessmentRepo.findByAssessmentCode(sA.getAssessmentCode()));
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
