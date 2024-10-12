package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class AssessmentStudentService {

    private final AssessmentStudentRepository assessmentStudentRepository;

    @Autowired
    public AssessmentStudentService(final AssessmentStudentRepository assessmentStudentRepository) {
        this.assessmentStudentRepository = assessmentStudentRepository;
    }

    public AssessmentStudentEntity getStudentByID(UUID assessmentStudentID) {
        return assessmentStudentRepository.findById(assessmentStudentID).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudent.class, "assessmentStudentID", assessmentStudentID.toString())
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentStudentEntity updateStudent(AssessmentStudentEntity assessmentStudentEntity) {
        AssessmentStudentEntity currentAssessmentStudentEntity = assessmentStudentRepository.findById(assessmentStudentEntity.getAssessmentStudentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessmentStudentEntity.getAssessmentStudentID().toString())
        );

        BeanUtils.copyProperties(assessmentStudentEntity, currentAssessmentStudentEntity, "assessmentEntity", "createUser", "createDate");
        TransformUtil.uppercaseFields(currentAssessmentStudentEntity);
        return assessmentStudentRepository.save(currentAssessmentStudentEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentStudentEntity createStudent(AssessmentStudentEntity assessmentStudentEntity) {
        return assessmentStudentRepository.save(assessmentStudentEntity);
    }


}
