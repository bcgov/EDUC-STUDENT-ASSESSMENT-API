package ca.bc.gov.educ.assessment.api.choreographer;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.StudentForAssessmentUpdate;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ChoreographEventHandlerTest extends BaseAssessmentAPITest {

    @Autowired
    private ChoreographEventHandler choreographEventHandler;
    @Autowired
    private RestUtils restUtils;
    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    AssessmentStudentRepository studentRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    private AssessmentEventRepository assessmentEventRepository;

    @Test
    void handleEvent_UPDATE_SCHOOL_OF_RECORD_shouldUpdateStudentRecord() throws JsonProcessingException {
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        var student = studentRepository.save(createMockStudentEntity(assessment));

        var studentForUpdate = StudentForAssessmentUpdate.builder()
                .schoolOfRecordID(String.valueOf(UUID.randomUUID()))
                .studentID(String.valueOf(student.getStudentID()))
                .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .createDate(LocalDateTime.now().toString())
                .updateDate(LocalDateTime.now().toString())
                .build();

        var assessmentEventEntity = AssessmentEventEntity.builder()
             .eventType("UPDATE_SCHOOL_OF_RECORD")
             .eventOutcome("SCHOOL_OF_RECORD_UPDATED")
             .eventStatus("DB_COMMITTED")
             .eventPayload(JsonUtil.getJsonStringFromObject(studentForUpdate))
             .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
             .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
             .createDate(LocalDateTime.now())
             .updateDate(LocalDateTime.now())
             .build();
        var savedEvent = assessmentEventRepository.save(assessmentEventEntity);

        assessmentEventEntity.setEventId(savedEvent.getEventId());
        choreographEventHandler.handleEvent(assessmentEventEntity);

        var updatedStudent = studentRepository.findByStudentID(student.getStudentID());
        assertThat(updatedStudent).hasSize(1);
        assertThat(updatedStudent.get(0).getSchoolOfRecordSchoolID()).isEqualTo(UUID.fromString(studentForUpdate.getSchoolOfRecordID()));
    }

    @Test
    void handleEvent_UPDATE_shouldNotUpdateStudentRecord() throws JsonProcessingException {
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        var student = studentRepository.save(createMockStudentEntity(assessment));

        var studentForUpdate = StudentForAssessmentUpdate.builder()
                .schoolOfRecordID(String.valueOf(UUID.randomUUID()))
                .studentID(String.valueOf(student.getStudentID()))
                .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .createDate(LocalDateTime.now().toString())
                .updateDate(LocalDateTime.now().toString())
                .build();

        var assessmentEventEntity = AssessmentEventEntity.builder()
                .eventType("UPDATE")
                .eventOutcome("SCHOOL_OF_RECORD_UPDATED")
                .eventStatus("DB_COMMITTED")
                .eventPayload(JsonUtil.getJsonStringFromObject(studentForUpdate))
                .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
        var savedEvent = assessmentEventRepository.save(assessmentEventEntity);

        assessmentEventEntity.setEventId(savedEvent.getEventId());
        choreographEventHandler.handleEvent(assessmentEventEntity);

        var updatedStudent = studentRepository.findByStudentID(student.getStudentID());
        assertThat(updatedStudent).hasSize(1);
        assertThat(updatedStudent.get(0).getSchoolOfRecordSchoolID()).isNotEqualTo(UUID.fromString(studentForUpdate.getSchoolOfRecordID()));

    }
}
