package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.PaginatedResponse;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.ReportGradStudentData;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentCompletionSummaryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssessmentCompletionCurrentStudentsService {
  private static final int DEFAULT_PAGE_SIZE = 1000;
  private static final String ACTIVE_GRAD_STUDENT_STATUS = "CUR";
  private static final String PUBLIC_SCHOOL_CATEGORY = "PUBLIC";
  private static final String VALUE_TYPE_STRING = "STRING";

  private final RestUtils restUtils;
  private final AssessmentStudentRepository assessmentStudentRepository;
  private final ObjectMapper objectMapper;

  public AssessmentCompletionCurrentStudentsChunk getCurrentStudentsChunk(final String searchCriteriaListJson, final int pageNumber) {
    final PaginatedResponse<ReportGradStudentData> gradStudentsPage =
      restUtils.getGradStudentReportPage(searchCriteriaListJson, pageNumber, DEFAULT_PAGE_SIZE);

    final List<String> pens = gradStudentsPage.getContent().stream()
      .map(ReportGradStudentData::getPen)
      .filter(StringUtils::isNotBlank)
      .distinct()
      .toList();

    final Map<String, AssessmentCompletionSummaryResult> assessmentCompletionByPen = new LinkedHashMap<>();
    if (!pens.isEmpty()) {
      assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(pens)
        .forEach(result -> assessmentCompletionByPen.put(result.getPen(), result));
    }

    return AssessmentCompletionCurrentStudentsChunk.builder()
      .gradStudentsPage(gradStudentsPage)
      .assessmentCompletionByPen(assessmentCompletionByPen)
      .build();
  }

  public AssessmentCompletionCurrentStudentsChunk getSchoolCurrentStudentsChunk(final String schoolId, final int pageNumber) {
    return getCurrentStudentsChunk(buildSchoolSearchCriteriaListJson(schoolId), pageNumber);
  }

  public AssessmentCompletionCurrentStudentsChunk getDistrictCurrentStudentsChunk(final String districtId, final int pageNumber) {
    return getCurrentStudentsChunk(buildDistrictSearchCriteriaListJson(districtId), pageNumber);
  }

  private String buildSchoolSearchCriteriaListJson(final String schoolId) {
    return writeSearchCriteriaJson(List.of(
      searchCriterion("schoolOfRecordId", "eq", schoolId, "UUID", "AND"),
      searchCriterion("studentStatus", "eq", ACTIVE_GRAD_STUDENT_STATUS, VALUE_TYPE_STRING, "AND")
    ));
  }

  private String buildDistrictSearchCriteriaListJson(final String districtId) {
    return writeSearchCriteriaJson(List.of(
      searchCriterion("districtId", "eq", districtId, "UUID", "AND"),
      searchCriterion("schoolCategoryCode", "eq", PUBLIC_SCHOOL_CATEGORY, VALUE_TYPE_STRING, "AND"),
      searchCriterion("studentStatus", "eq", ACTIVE_GRAD_STUDENT_STATUS, VALUE_TYPE_STRING, "AND")
    ));
  }

  private String writeSearchCriteriaJson(final List<Map<String, Object>> criteriaList) {
    final List<Map<String, Object>> wrapperList = new ArrayList<>();
    final Map<String, Object> wrapper = new LinkedHashMap<>();
    wrapper.put("searchCriteriaList", criteriaList);
    wrapperList.add(wrapper);
    try {
      return objectMapper.writeValueAsString(wrapperList);
    } catch (JsonProcessingException e) {
      throw new StudentAssessmentAPIRuntimeException(e);
    }
  }

  private Map<String, Object> searchCriterion(final String key, final String operation, final String value, final String valueType, final String condition) {
    final Map<String, Object> criterion = new LinkedHashMap<>();
    criterion.put("key", key);
    criterion.put("operation", operation);
    criterion.put("value", value);
    criterion.put("valueType", valueType);
    criterion.put("condition", condition);
    return criterion;
  }

  @Getter
  @Builder
  public static class AssessmentCompletionCurrentStudentsChunk {
    private final PaginatedResponse<ReportGradStudentData> gradStudentsPage;
    private final Map<String, AssessmentCompletionSummaryResult> assessmentCompletionByPen;
  }
}
