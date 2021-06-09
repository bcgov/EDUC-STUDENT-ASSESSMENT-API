package ca.bc.gov.educ.api.studentassessment.service;

import ca.bc.gov.educ.api.studentassessment.model.dto.Assessment;
import ca.bc.gov.educ.api.studentassessment.model.dto.School;
import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentEntity;
import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentId;
import ca.bc.gov.educ.api.studentassessment.repository.StudentAssessmentRepository;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentAssessmentServiceTest {

    @Autowired
    StudentAssessmentService studentAssessmentService;

    @Autowired
    private StudentAssessmentApiConstants constants;

    @MockBean
    private StudentAssessmentRepository studentAssessmentRepo;

    @MockBean
    WebClient webClient;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    @Test
    public void testGetStudentAssessmentList() {
        // ID
        StudentAssessmentId studentAssessmentId = new StudentAssessmentId();
        studentAssessmentId.setPen("123456789");
        studentAssessmentId.setAssessmentCode("assmt");
        studentAssessmentId.setSessionDate("2020-05");

        StudentAssessmentEntity studentAssessmentEntity = new StudentAssessmentEntity();
        studentAssessmentEntity.setAssessmentKey(studentAssessmentId);
        studentAssessmentEntity.setSpecialCase("special");
        studentAssessmentEntity.setMincodeAssessment("12345678");

        Assessment assessment = new Assessment();
        assessment.setAssessmentCode("assmt");
        assessment.setAssessmentName("assmt test");
        assessment.setLanguage("en");

        School school = new School();
        school.setMinCode("12345678");
        school.setSchoolName("Test School");

        when(studentAssessmentRepo.findByPen(studentAssessmentId.getPen())).thenReturn(Arrays.asList(studentAssessmentEntity));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getAssessmentByAssessmentCodeUrl(), assessment.getAssessmentCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Assessment.class)).thenReturn(Mono.just(assessment));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolNameByMincodeUrl(), studentAssessmentEntity.getMincodeAssessment()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(School.class)).thenReturn(Mono.just(school));

        var result = studentAssessmentService.getStudentAssessmentList(studentAssessmentId.getPen(), "accessToken", true);
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        StudentAssessment responseStudentAssessment = result.get(0);
        assertThat(responseStudentAssessment.getAssessmentCode()).isEqualTo(assessment.getAssessmentCode());
        assertThat(responseStudentAssessment.getSpecialCase()).isEqualTo(studentAssessmentEntity.getSpecialCase());
        assertThat(responseStudentAssessment.getMincodeAssessmentName()).isEqualTo(school.getSchoolName());
    }
}
