package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.filter.FilterOperation;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.assessment.api.struct.v1.Condition.AND;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentStudentControllerTest extends BaseAssessmentAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  AssessmentStudentRepository studentRepository;

  @Autowired
  AssessmentSessionRepository assessmentSessionRepository;

  @Autowired
  AssessmentRepository assessmentRepository;

  private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;

  @AfterEach
  public void after() {
    this.studentRepository.deleteAll();
    this.assessmentRepository.deleteAll();
    this.assessmentSessionRepository.deleteAll();
  }

  @Test
  void testReadStudent_GivenIDExists_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
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

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();

    this.mockMvc.perform(
                    get(URL.BASE_URL_STUDENT + "/" + assessmentStudentID).with(mockAuthority))
            .andDo(print())
            .andExpect(status().isForbidden());
  }

  @Test
  void testUpdateStudent_GivenInvalidProvincialCaseCode_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity assessmentStudentEntity = studentRepository.save(createMockStudentEntity(assessment));
    AssessmentStudent student = mapper.toStructure(assessmentStudentEntity);
    student.setCreateDate(null);
    student.setUpdateDate(null);
    student.setUpdateUser(null);

    this.mockMvc.perform(
                    put(URL.BASE_URL_STUDENT + "/" + student.getAssessmentStudentID())
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.assessmentStudentID", equalTo(student.getAssessmentStudentID())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.updateUser", equalTo(ApplicationProperties.STUDENT_ASSESSMENT_API)));
  }

  @Test
  void testCreateStudent_GivenIDNotNull_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentStudent student = createMockStudent();
    student.setAssessmentStudentID(null);
    student.setAssessmentID(UUID.randomUUID().toString());

    this.mockMvc.perform(
                    post(URL.BASE_URL_STUDENT)
                            .contentType(APPLICATION_JSON)
                            .content(asJsonString(student))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].field").value("assessmentID"));
  }

  @Test
  void testCreateStudent_GivenInvalidAssessmentID_ShouldReturn400() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
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
            .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].field").value("assessmentID"));
  }

  @Test
  void testCreateStudent_GivenValidPayload_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudent student = createMockStudent();
    student.setAssessmentStudentID(null);
    student.setAssessmentID(assessment.getAssessmentID().toString());
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
            .andExpect(MockMvcResultMatchers.jsonPath("$.updateUser", equalTo(ApplicationProperties.STUDENT_ASSESSMENT_API)));
  }

  @Test
  void testFindAll_GivenAssessmentCodeAndSessionMonth_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = createMockSessionEntity();
    session.setCourseMonth("08");
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity student = createMockStudentEntity(assessment);
    studentRepository.save(student);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    studentRepository.save(student2);

    SearchCriteria criteriaAssessmentTypeCode = SearchCriteria.builder()
            .key("assessmentEntity.assessmentTypeCode")
            .operation(FilterOperation.EQUAL)
            .value(AssessmentTypeCodes.LTP10.getCode())
            .valueType(ValueType.STRING)
            .build();

    SearchCriteria criteriaSessionMonth = SearchCriteria.builder()
            .condition(AND)
            .key("assessmentEntity.assessmentSessionEntity.courseMonth")
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

  @Test
  void testFindAll_GivenAssessmentID_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = createMockSessionEntity();
    session.setCourseMonth("08");
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity student = createMockStudentEntity(assessment);
    studentRepository.save(student);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    studentRepository.save(student2);

    AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
    studentRepository.save(student3);

    SearchCriteria criteriaAssessmentID = SearchCriteria.builder()
            .key("assessmentEntity.assessmentID")
            .operation(FilterOperation.EQUAL)
            .value(assessment2.getAssessmentID().toString())
            .valueType(ValueType.UUID)
            .build();


    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteriaAssessmentID);
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
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void testFindAll_GivenSchoolYear_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = createMockSessionEntity();
    session.setCourseMonth("08");
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity student = createMockStudentEntity(assessment);
    studentRepository.save(student);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    studentRepository.save(student2);

    AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
    studentRepository.save(student3);

    SearchCriteria criteriaAssessmentID = SearchCriteria.builder()
            .key("assessmentEntity.assessmentSessionEntity.schoolYear")
            .operation(FilterOperation.EQUAL)
            .value(String.valueOf(LocalDateTime.now().getYear()))
            .valueType(ValueType.INTEGER)
            .build();


    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteriaAssessmentID);
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
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
  }

  @Test
  void testFindAll_GivenSchoolYearAndSpecialCode_ShouldReturnStudent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = createMockSessionEntity();
    session.setCourseMonth("08");
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
    studentRepository.save(student1);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    studentRepository.save(student2);

    AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
    student3.setProvincialSpecialCaseCode(ProvincialSpecialCaseCodes.EXEMPT.getCode());
    studentRepository.save(student3);
    SearchCriteria criteriaAssessmentID = SearchCriteria.builder()
            .key("assessmentEntity.assessmentSessionEntity.schoolYear")
            .operation(FilterOperation.EQUAL)
            .value(String.valueOf(LocalDateTime.now().getYear()))
            .valueType(ValueType.INTEGER)
            .build();

    SearchCriteria criteriaSpecialCaseCode = SearchCriteria.builder()
            .condition(AND)
            .key("provincialSpecialCaseCode")
            .operation(FilterOperation.IN)
            .value("E,D")
            .valueType(ValueType.STRING)
            .build();


    List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteriaAssessmentID);
    criteriaList.add(criteriaSpecialCaseCode);
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

  @Test
  void testDeleteStudents_GivenValidID_ShouldReturn204() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();
    String payload = asJsonString(Collections.singletonList(assessmentStudentID));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isNoContent());
  }

  @Test
  void testDeleteStudents_GivenValidIDs_ShouldReturn204() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID1 = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();
    UUID assessmentStudentID2 = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();
    String payload = asJsonString(Arrays.asList(assessmentStudentID1, assessmentStudentID2));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isNoContent());
  }

  @Test
  void testDeleteStudents_GivenInvalidID_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    String payload = asJsonString(Collections.singletonList(UUID.randomUUID()));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

  @Test
  void testDeleteStudents_GivenInvalidIDs_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    String payload = asJsonString(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

  @Test
  void testDeleteStudents_GivenOneInvalidID_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();
    String payload = asJsonString(Arrays.asList(assessmentStudentID, UUID.randomUUID()));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

  @Test
  void testDeleteStudents_GivenNoAuthority_ShouldReturn403() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();
    String payload = asJsonString(Collections.singletonList(assessmentStudentID));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isForbidden());
  }

  @Test
  void testDeleteStudents_WhenSessionHasEnded_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = createMockSessionEntity();
    session.setActiveUntilDate(LocalDateTime.now().minusDays(1));
    AssessmentSessionEntity savedSession = assessmentSessionRepository.save(session);
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID = studentRepository.save(createMockStudentEntity(assessment)).getAssessmentStudentID();
    String payload = asJsonString(Collections.singletonList(assessmentStudentID));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

  @Test
  void testDeleteStudents_WhenOneSessionHasEnded_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session1 = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentSessionEntity session2 = createMockSessionEntity();
    session2.setActiveUntilDate(LocalDateTime.now().minusDays(1));
    AssessmentSessionEntity finalSession2 = assessmentSessionRepository.save(session2);
    AssessmentEntity assessment1 = assessmentRepository.save(createMockAssessmentEntity(session1, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(finalSession2, AssessmentTypeCodes.LTF12.getCode()));
    UUID assessmentStudentID1 = studentRepository.save(createMockStudentEntity(assessment1)).getAssessmentStudentID();
    UUID assessmentStudentID2 = studentRepository.save(createMockStudentEntity(assessment2)).getAssessmentStudentID();
    String payload = asJsonString(Arrays.asList(assessmentStudentID1, assessmentStudentID2));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

  @Test
  void testDeleteStudents_WhenStudentHasResult_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student = createMockStudentEntity(assessment);
    student.setProficiencyScore(1);
    UUID assessmentStudentID = studentRepository.save(student).getAssessmentStudentID();
    String payload = asJsonString(Collections.singletonList(assessmentStudentID));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

  @Test
  void testDeleteStudents_WhenStudentsHaveResults_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
    student1.setProficiencyScore(1);
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
    student2.setProficiencyScore(1);
    UUID assessmentStudentID1 = studentRepository.save(student1).getAssessmentStudentID();
    UUID assessmentStudentID2 = studentRepository.save(student2).getAssessmentStudentID();
    String payload = asJsonString(Arrays.asList(assessmentStudentID1, assessmentStudentID2));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

  @Test
  void testDeleteStudents_WhenOneStudentHasResult_ShouldReturn409() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_STUDENT";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
    student1.setProficiencyScore(1);
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
    UUID assessmentStudentID1 = studentRepository.save(student1).getAssessmentStudentID();
    UUID assessmentStudentID2 = studentRepository.save(student2).getAssessmentStudentID();
    String payload = asJsonString(Arrays.asList(assessmentStudentID1, assessmentStudentID2));

    this.mockMvc.perform(post(URL.BASE_URL_STUDENT + "/delete-students")
                    .with(mockAuthority)
                    .contentType(APPLICATION_JSON)
                    .content(payload))
            .andDo(print())
            .andExpect(status().isConflict());
  }

}
