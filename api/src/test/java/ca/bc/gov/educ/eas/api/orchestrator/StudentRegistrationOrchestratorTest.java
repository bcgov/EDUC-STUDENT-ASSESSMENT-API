package ca.bc.gov.educ.eas.api.orchestrator;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.SagaEnum;
import ca.bc.gov.educ.eas.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.messaging.MessagePublisher;
import ca.bc.gov.educ.eas.api.model.v1.*;
import ca.bc.gov.educ.eas.api.repository.v1.*;
import ca.bc.gov.educ.eas.api.rest.RestUtils;
import ca.bc.gov.educ.eas.api.service.v1.SagaService;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
public class StudentRegistrationOrchestratorTest extends BaseEasAPITest {

    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    AssessmentStudentRepository studentRepository;
    @Autowired
    SagaRepository sagaRepository;
    @Autowired
    SagaEventRepository sagaEventRepository;
    @Autowired
    MessagePublisher messagePublisher;
    @Autowired
    StudentRegistrationOrchestrator studentRegistrationOrchestrator;
    @Autowired
    SagaService sagaService;
    @MockBean
    protected RestUtils restUtils;
    @Captor
    ArgumentCaptor<byte[]> eventCaptor;

    @BeforeEach
    public void setUp() {
        Mockito.reset(this.messagePublisher);
        Mockito.reset(this.restUtils);
        JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @AfterEach
    public void after() {
        this.studentRepository.deleteAll();
        this.assessmentRepository.deleteAll();
        this.sessionRepository.deleteAll();
    }

    @SneakyThrows
    @Test
    public void testHandleEvent_createAssessmentStudent_CREATED() {
        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudent student = createMockStudent();
        student.setAssessmentStudentID(null);
        student.setAssessmentID(assessment.getAssessmentID().toString());

        EasSagaEntity sagaEntity = this.studentRegistrationOrchestrator.createSaga(JsonUtil.getJsonString(student).get(), student.getUpdateUser());
        this.studentRegistrationOrchestrator.startSaga(sagaEntity);

        Optional<AssessmentStudentEntity> createdStudent = studentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(UUID.fromString(student.getAssessmentID()), UUID.fromString(student.getStudentID()));
        assertThat(createdStudent).isPresent();
        assertThat(createdStudent.get().getAssessmentStudentID()).isNotNull();
        assertThat(createdStudent.get().getStudentID().toString()).isEqualTo(student.getStudentID());

    }



}
