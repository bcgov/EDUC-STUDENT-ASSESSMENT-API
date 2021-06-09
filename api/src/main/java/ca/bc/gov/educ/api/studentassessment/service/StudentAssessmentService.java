package ca.bc.gov.educ.api.studentassessment.service;


import ca.bc.gov.educ.api.studentassessment.model.dto.Assessment;
import ca.bc.gov.educ.api.studentassessment.model.dto.School;
import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.transformer.StudentAssessmentTransformer;
import ca.bc.gov.educ.api.studentassessment.repository.StudentAssessmentRepository;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class StudentAssessmentService {

    private static Logger logger = LoggerFactory.getLogger(StudentAssessmentService.class);

    private StudentAssessmentRepository studentAssessmentRepo;
    private StudentAssessmentTransformer studentAssessmentTransformer;
    private WebClient webClient;
    private StudentAssessmentApiConstants constants;

    public StudentAssessmentService(StudentAssessmentRepository studentAssessmentRepo, StudentAssessmentTransformer studentAssessmentTransformer,  WebClient webClient, StudentAssessmentApiConstants constants) {
        this.studentAssessmentRepo = studentAssessmentRepo;
        this.studentAssessmentTransformer = studentAssessmentTransformer;
        this.webClient = webClient;
        this.constants = constants;
    }

    /**
     * Get all student assessments by PEN populated in Student Assessment DTO
     *
     * @param accessToken
     * @return Student Assessment
     * @throws java.lang.Exception
     */
    public List<StudentAssessment> getStudentAssessmentList(String pen, String accessToken, boolean sortForUI) {
        List<StudentAssessment> studentAssessment = new ArrayList<StudentAssessment>();
        try {
            studentAssessment = studentAssessmentTransformer.transformToDTO(studentAssessmentRepo.findByPen(pen));
            studentAssessment.forEach(sA -> {
                Assessment assessment = webClient.get()
                        .uri(String.format(constants.getAssessmentByAssessmentCodeUrl(), sA.getAssessmentCode().trim()))
                        .headers(h -> h.setBearerAuth(accessToken))
                        .retrieve()
                        .bodyToMono(Assessment.class)
                        .block();
                if (assessment != null) {
                    sA.setAssessmentName(assessment.getAssessmentName());
                    sA.setAssessmentDetails(assessment);
                }

                if (StringUtils.isNotBlank(sA.getMincodeAssessment())) {
                    School schObj = webClient.get()
                            .uri(String.format(constants.getSchoolNameByMincodeUrl(), sA.getMincodeAssessment()))
                            .headers(h -> h.setBearerAuth(accessToken))
                            .retrieve()
                            .bodyToMono(School.class)
                            .block();
                    if (schObj != null)
                        sA.setMincodeAssessmentName(schObj.getSchoolName());
                }
            });
            logger.debug(studentAssessment.toString());
        } catch (Exception e) {
            logger.debug("Exception:" + e);
        }
        if (sortForUI) {
            Collections.sort(studentAssessment, Comparator.comparing(StudentAssessment::getPen)
                    .thenComparing(StudentAssessment::getAssessmentCode)
                    .thenComparing(StudentAssessment::getSessionDate));
        }
        return studentAssessment;
    }
}
