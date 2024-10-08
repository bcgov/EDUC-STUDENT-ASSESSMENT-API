package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.TransformUtil;
import jakarta.persistence.EntityExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AssessmentStudentService {

  private final AssessmentStudentRepository assessmentStudentRepository;
  private final SessionRepository sessionRepository;

  @Autowired
  public AssessmentStudentService(final AssessmentStudentRepository assessmentStudentRepository, SessionRepository sessionRepository) {
    this.assessmentStudentRepository = assessmentStudentRepository;
    this.sessionRepository = sessionRepository;
  }

  public AssessmentStudentEntity getStudentByID(UUID assessmentStudentID) {
    return this.assessmentStudentRepository.findById(assessmentStudentID).orElseThrow(() ->
      new EntityNotFoundException(AssessmentStudent.class, "assessmentStudentID", assessmentStudentID.toString())
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AssessmentStudentEntity updateStudent(AssessmentStudentEntity assessmentStudentEntity) {
    AssessmentStudentEntity currentAssessmentStudentEntity = this.assessmentStudentRepository.findById(assessmentStudentEntity.getAssessmentStudentID()).orElseThrow(() ->
      new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessmentStudentEntity.getAssessmentStudentID().toString())
    );

    SessionEntity sessionEntity = this.sessionRepository.findById(assessmentStudentEntity.getSessionEntity().getSessionID()).orElseThrow(() ->
            new EntityNotFoundException(SessionEntity.class, "sessionID", assessmentStudentEntity.getSessionEntity().getSessionID().toString())
    );

    Optional<AssessmentStudentEntity> optionalExistingStudent = assessmentStudentRepository.findBySessionEntityAndStudentID(sessionEntity, assessmentStudentEntity.getStudentID());

    //If their already exists this student id in the session that is not the assessment student being updated
    if(optionalExistingStudent.isPresent() && !optionalExistingStudent.get().getAssessmentStudentID().equals(assessmentStudentEntity.getAssessmentStudentID())) {
      throw new EntityExistsException("Assessment student with StudentID::"+assessmentStudentEntity.getStudentID()+" and Session ID::"+sessionEntity.getSessionID()+" already exists");
    } else {
      BeanUtils.copyProperties(assessmentStudentEntity, currentAssessmentStudentEntity, "createUser", "createDate");
      TransformUtil.uppercaseFields(currentAssessmentStudentEntity);
      return assessmentStudentRepository.save(currentAssessmentStudentEntity);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AssessmentStudentEntity createStudent(AssessmentStudentEntity assessmentStudentEntity) {
    SessionEntity sessionEntity = this.sessionRepository.findById(assessmentStudentEntity.getSessionEntity().getSessionID()).orElseThrow(() ->
      new EntityNotFoundException(SessionEntity.class, "sessionID", assessmentStudentEntity.getSessionEntity().getSessionID().toString())
    );

    Optional<AssessmentStudentEntity> optionalExistingStudent = assessmentStudentRepository.findBySessionEntityAndStudentID(sessionEntity, assessmentStudentEntity.getStudentID());

    if(optionalExistingStudent.isEmpty()) {
      TransformUtil.uppercaseFields(assessmentStudentEntity);
      return assessmentStudentRepository.save(assessmentStudentEntity);
    } else {
      throw new EntityExistsException("Assessment student with StudentID::"+assessmentStudentEntity.getStudentID()+" and Session ID::"+sessionEntity.getSessionID()+" already exists");
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteStudent(UUID assessmentStudentID) {
    AssessmentStudentEntity currentAssessmentStudentEntity = this.assessmentStudentRepository.findById(assessmentStudentID).orElseThrow(() ->
            new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessmentStudentID.toString())
    );
    this.assessmentStudentRepository.delete(currentAssessmentStudentEntity);
  }
}
