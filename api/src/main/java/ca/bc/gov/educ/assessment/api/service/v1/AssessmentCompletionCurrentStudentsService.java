package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.AssessmentCompletionCurrentStudentPage;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.ReportGradStudentData;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentCompletionSummaryResult;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssessmentCompletionCurrentStudentsService {
  private static final int DEFAULT_PAGE_SIZE = 2000;

  private final RestUtils restUtils;
  private final AssessmentStudentRepository assessmentStudentRepository;

  public AssessmentCompletionCurrentStudentsChunk getSchoolCurrentStudentsChunk(final String schoolId, final int pageNumber) {
    final AssessmentCompletionCurrentStudentPage gradStudentsPage =
      restUtils.getGradAssessmentCompletionCurrentStudentsPage("schoolId", schoolId, pageNumber, DEFAULT_PAGE_SIZE);
    return enrichChunk(gradStudentsPage);
  }

  public AssessmentCompletionCurrentStudentsChunk getDistrictCurrentStudentsChunk(final String districtId, final int pageNumber) {
    final AssessmentCompletionCurrentStudentPage gradStudentsPage =
      restUtils.getGradAssessmentCompletionCurrentStudentsPage("districtId", districtId, pageNumber, DEFAULT_PAGE_SIZE);
    return enrichChunk(gradStudentsPage);
  }

  private AssessmentCompletionCurrentStudentsChunk enrichChunk(final AssessmentCompletionCurrentStudentPage gradStudentsPage) {
    final List<String> pens = gradStudentsPage.getContent().stream()
      .map(ReportGradStudentData::getPen)
      .filter(StringUtils::isNotBlank)
      .distinct()
      .toList();

    final Map<String, AssessmentCompletionSummaryResult> assessmentCompletionByPen = new LinkedHashMap<>();
    final Map<String, String> localIdByPen = new LinkedHashMap<>();
    if (!pens.isEmpty()) {
      assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(pens)
        .forEach(result -> assessmentCompletionByPen.put(result.getPen(), result));
      assessmentStudentRepository.findStudentLocalIdsByPenIn(pens)
        .forEach(result -> localIdByPen.put(result.getPen(), result.getLocalID()));
    }

    return AssessmentCompletionCurrentStudentsChunk.builder()
      .gradStudentsPage(gradStudentsPage)
      .assessmentCompletionByPen(assessmentCompletionByPen)
      .localIdByPen(localIdByPen)
      .build();
  }

  @Getter
  @Builder
  public static class AssessmentCompletionCurrentStudentsChunk {
    private final AssessmentCompletionCurrentStudentPage gradStudentsPage;
    private final Map<String, AssessmentCompletionSummaryResult> assessmentCompletionByPen;
    private final Map<String, String> localIdByPen;
  }
}
