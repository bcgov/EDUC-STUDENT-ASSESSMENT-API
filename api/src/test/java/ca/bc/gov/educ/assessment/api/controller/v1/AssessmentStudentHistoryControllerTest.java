package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.filter.FilterOperation;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.Search;
import ca.bc.gov.educ.assessment.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.assessment.api.struct.v1.ValueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentStudentHistoryControllerTest extends BaseAssessmentAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  AssessmentStudentRepository studentRepository;

  @Autowired
  AssessmentSessionRepository assessmentSessionRepository;

  @Autowired
  AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

  @Autowired
  AssessmentRepository assessmentRepository;

  @Autowired
  RestUtils restUtils;
  
  @Autowired
  private AssessmentFormRepository assessmentFormRepository;

  private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;

  @BeforeEach
  void setUp() {
    assessmentFormRepository.deleteAll();
    this.assessmentStudentHistoryRepository.deleteAll();
    this.studentRepository.deleteAll();
    this.assessmentRepository.deleteAll();
    this.assessmentSessionRepository.deleteAll();
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
    var savedStudent = studentRepository.save(student);
    var studentHist = createMockStudentHistoryEntity(savedStudent);
    assessmentStudentHistoryRepository.save(studentHist);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    var savedStudent2 = studentRepository.save(student2);
    var studentHist2 = createMockStudentHistoryEntity(savedStudent2);
    assessmentStudentHistoryRepository.save(studentHist2);

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
                    get(URL.BASE_URL_STUDENT_HISTORY + URL.PAGINATED)
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
    var savedStudent = studentRepository.save(student);
    var studentHist = createMockStudentHistoryEntity(savedStudent);
    assessmentStudentHistoryRepository.save(studentHist);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    var savedStudent2 = studentRepository.save(student2);
    var studentHist2 = createMockStudentHistoryEntity(savedStudent2);
    assessmentStudentHistoryRepository.save(studentHist2);

    AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
    var savedStudent3 = studentRepository.save(student3);
    var studentHist3 = createMockStudentHistoryEntity(savedStudent3);
    assessmentStudentHistoryRepository.save(studentHist3);

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
                    get(URL.BASE_URL_STUDENT_HISTORY + URL.PAGINATED)
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
    var savedStudent = studentRepository.save(student);
    var studentHist = createMockStudentHistoryEntity(savedStudent);
    assessmentStudentHistoryRepository.save(studentHist);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    var savedStudent2 = studentRepository.save(student2);
    var studentHist2 = createMockStudentHistoryEntity(savedStudent2);
    assessmentStudentHistoryRepository.save(studentHist2);

    AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
    var savedStudent3 = studentRepository.save(student3);
    var studentHist3 = createMockStudentHistoryEntity(savedStudent3);
    assessmentStudentHistoryRepository.save(studentHist3);

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
                    get(URL.BASE_URL_STUDENT_HISTORY + URL.PAGINATED)
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

    AssessmentStudentEntity student = createMockStudentEntity(assessment);
    var savedStudent = studentRepository.save(student);
    var studentHist = createMockStudentHistoryEntity(savedStudent);
    assessmentStudentHistoryRepository.save(studentHist);

    AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
    var savedStudent2 = studentRepository.save(student2);
    var studentHist2 = createMockStudentHistoryEntity(savedStudent2);
    assessmentStudentHistoryRepository.save(studentHist2);

    AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
    student3.setProvincialSpecialCaseCode(ProvincialSpecialCaseCodes.EXEMPT.getCode());
    var savedStudent3 = studentRepository.save(student3);
    var studentHist3 = createMockStudentHistoryEntity(savedStudent3);
    assessmentStudentHistoryRepository.save(studentHist3);
    
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
                    get(URL.BASE_URL_STUDENT_HISTORY + URL.PAGINATED)
                            .with(mockAuthority)
                            .param("searchCriteriaList", criteriaJSON)
                            .contentType(APPLICATION_JSON))
            .andDo(print())
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

}
