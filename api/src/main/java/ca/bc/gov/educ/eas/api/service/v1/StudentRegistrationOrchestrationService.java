package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class StudentRegistrationOrchestrationService {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;
    protected final SagaService sagaService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    public StudentRegistrationOrchestrationService(AssessmentStudentService assessmentStudentService, SagaService sagaService) {
        this.assessmentStudentService = assessmentStudentService;
        this.sagaService = sagaService;
    }

    public void createNewStudentRegistration(AssessmentStudent assessmentStudent) {
        Optional<AssessmentStudentEntity> studentEntity = assessmentStudentService.getStudentByAssessmentIDAndStudentID(UUID.fromString(assessmentStudent.getAssessmentID()), UUID.fromString(assessmentStudent.getStudentID()));
        if (studentEntity.isEmpty()) {
            assessmentStudentService.createStudent(mapper.toModel(assessmentStudent));
        } else {
            log.info("Student already exists in assessment {} ", assessmentStudent);
        }
    }

    public void publishStudentRegistration(AssessmentStudent assessmentStudent, EasSagaEntity sagaEntity) {
        //Placeholder
    }

}
