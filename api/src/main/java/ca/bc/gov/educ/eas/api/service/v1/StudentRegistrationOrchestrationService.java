package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StudentRegistrationOrchestrationService {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;
    protected final SagaService sagaService;

    //Inject Downstream Service
    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    public StudentRegistrationOrchestrationService(AssessmentStudentService assessmentStudentService, SagaService sagaService) {
        this.assessmentStudentService = assessmentStudentService;
        this.sagaService = sagaService;
    }

    public void createNewStudentRegistration(AssessmentStudent assessmentStudent) {
        assessmentStudentService.createStudent(mapper.toModel(assessmentStudent));
    }

    public void publishStudentRegistration(AssessmentStudent assessmentStudent, EasSagaEntity sagaEntity) {
        //Placeholder
    }

}
