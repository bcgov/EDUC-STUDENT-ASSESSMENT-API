package ca.bc.gov.educ.api.studentassessment.service;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.transformer.StudentAssessmentTransformer;
import ca.bc.gov.educ.api.studentassessment.repository.StudentAssessmentRepository;

@Service
public class StudentAssessmentService {

    @Autowired
    private StudentAssessmentRepository studentAssessmentRepo;

    @Autowired
    private StudentAssessmentTransformer studentAssessmentTransformer;

    private static Logger logger = LoggerFactory.getLogger(StudentAssessmentService.class);

     /**
     * Get all student courses by PEN populated in Student Course DTO
     *
     * @return Student Course 
     * @throws java.lang.Exception
     */
    public List<StudentAssessment> getStudentAssessmentList(String pen) {
        List<StudentAssessment> studentAssessment  = new ArrayList<StudentAssessment>();

        try {
        	studentAssessment = studentAssessmentTransformer.transformToDTO(studentAssessmentRepo.findByPen(pen));
            logger.debug(studentAssessment.toString());
        } catch (Exception e) {
            logger.debug("Exception:" + e);
        }

        return studentAssessment;
    }
}
