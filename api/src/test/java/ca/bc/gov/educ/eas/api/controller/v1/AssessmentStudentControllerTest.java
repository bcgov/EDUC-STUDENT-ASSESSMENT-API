package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.filter.FilterOperation;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.struct.v1.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentStudentControllerTest extends BaseEasAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  AssessmentStudentRepository studentRepository;

  @Autowired
  SessionRepository sessionRepository;

  @Autowired
  AssessmentRepository assessmentRepository;

  @Autowired
  AssessmentStudentMapper mapper;

  @AfterEach
  public void after() {
    this.studentRepository.deleteAll();
    this.assessmentRepository.deleteAll();
    this.sessionRepository.deleteAll();
  }

  @Test
  void testReadStudent_GivenIDExists_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    SessionEntity session = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

    UUID assessmentStudentID = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();

    this.mockMvc.perform(
                    get(URL.BASE_URL_STUDENT + "/" + assessmentStudentID).with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.assessmentStudentID", equalTo(assessmentStudentID.toString())));
  }

  @Test
  void testReadStudent_GivenIDDoesNotExists_ShouldReturn404() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(
                    get(URL.BASE_URL_STUDENT + "/" + UUID.randomUUID()).with(mockAuthority))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  void testReadStudent_GivenIncorrectScope_ShouldReturn403() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    SessionEntity session = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();

    this.mockMvc.perform(
                    get(URL.BASE_URL_STUDENT + "/" + assessmentStudentID).with(mockAuthority))
            .andDo(print())
            .andExpect(status().isForbidden());
  }

  @Test
  void testUpdateStudent_GivenInvalidProvincialCaseCode_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentStudent student = createMockStudent();
    student.setProvincialSpecialCaseCode("INVALID");

    this.mockMvc.perform(
                    put(URL.BASE_URL_STUDENT + "/" + student.getAssessmentStudentID())
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.subErrors[?(@.field == 'provincialSpecialCaseCode')]").exists());
  }

  @Test
  void testUpdateStudent_GivenNullID_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentStudent student = createMockStudent();
    student.setAssessmentStudentID("");

    this.mockMvc.perform(
                    put(URL.BASE_URL_STUDENT + "/" + UUID.randomUUID())
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.subErrors[?(@.field == 'assessmentStudentID')]").exists());
  }

  @Test
  void testUpdateStudent_GivenInvalidPEN_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentStudent student = createMockStudent();
    student.setPen("123456789");

    this.mockMvc.perform(
                    put(URL.BASE_URL_STUDENT + "/" + student.getAssessmentStudentID())
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].field").value("pen"));
  }

  @Test
  void testUpdateStudent_GivenValidPayload_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    SessionEntity session = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudent student = mapper.toStructure(studentRepository.save(createMockStudentEntity(assessment)));

    student.setSessionID(assessment.getSessionEntity().getSessionID().toString());
    student.setAssessmentTypeCode(assessment.getAssessmentTypeCode());
    student.setCreateDate(null);
    student.setUpdateDate(null);
    student.setUpdateUser(null);
    student.setAssessmentID(null);

    this.mockMvc.perform(
                    put(URL.BASE_URL_STUDENT + "/" + student.getAssessmentStudentID())
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.assessmentStudentID", equalTo(student.getAssessmentStudentID())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.updateUser", equalTo("EAS_API")));
  }

  @Test
  void testCreateStudent_GivenIDNotNull_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentStudent student = createMockStudent();

    this.mockMvc.perform(
                    post(URL.BASE_URL_STUDENT)
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.subErrors[?(@.field == 'assessmentStudentID')]").exists());
  }

  @Test
  void testCreateStudent_GivenInvalidAssessmentTypeCode_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentStudent student = createMockStudent();
    student.setAssessmentStudentID(null);
    student.setAssessmentTypeCode("INVALID");

    this.mockMvc.perform(
                    post(URL.BASE_URL_STUDENT)
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].field").value("assessmentTypeCode"));
  }

  @Test
  void testCreateStudent_GivenInvalidCourseStatusCode_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentStudent student = createMockStudent();
    student.setAssessmentStudentID(null);
    student.setCourseStatusCode("INVALID");

    this.mockMvc.perform(
                    post(URL.BASE_URL_STUDENT)
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].field").value("courseStatusCode"));
  }

  @Test
  void testCreateStudent_GivenValidPayload_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    SessionEntity session = sessionRepository.save(createMockSessionEntity());
    assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudent student = createMockStudent();
    student.setAssessmentStudentID(null);
    student.setSessionID(session.getSessionID().toString());
    student.setAssessmentTypeCode(AssessmentTypeCodes.LTF12.getCode());
    student.setCreateDate(null);
    student.setUpdateDate(null);
    student.setUpdateUser(null);

    this.mockMvc.perform(
                    post(URL.BASE_URL_STUDENT)
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.updateUser", equalTo("EAS_API")));
  }

  @Test
  void testFindAll_GivenAssessmentCodeAndSessionMonth_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_EAS_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    SessionEntity session = createMockSessionEntity();
    session.setCourseMonth("08");
    SessionEntity sessionEntity = sessionRepository.save(session);
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity student = createMockStudentEntity(assessment);
    studentRepository.save(student);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    studentRepository.save(student2);

    SearchCriteria criteriaAssessmentTypeCode = SearchCriteria.builder()
            .key("assessmentEntity.assessmentTypeCode")
            .operation(FilterOperation.EQUAL)
            .value(AssessmentTypeCodes.LTP10.getCode())
            .valueType(ValueType.STRING)
            .build();

    SearchCriteria criteriaSessionMonth = SearchCriteria.builder()
            .condition(Condition.AND)
            .key("assessmentEntity.sessionEntity.courseMonth")
            .operation(FilterOperation.EQUAL)
            .value("08")
            .valueType(ValueType.STRING)
            .build();

    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteriaAssessmentTypeCode);
    criteriaList.add(criteriaSessionMonth);
    List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String criteriaJSON = objectMapper.writeValueAsString(searches);

    final MvcResult result = this.mockMvc.perform(
                    get(URL.BASE_URL_STUDENT + URL.PAGINATED)
                            .with(mockAuthority)
                            .param("searchCriteriaList", criteriaJSON)
                            .contentType(APPLICATION_JSON))
            .andDo(print())
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }
}
