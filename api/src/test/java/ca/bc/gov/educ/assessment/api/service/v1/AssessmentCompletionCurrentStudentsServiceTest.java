package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.PaginatedResponse;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.ReportGradStudentData;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentCompletionSummaryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentCompletionCurrentStudentsServiceTest {
    @Mock
    private RestUtils restUtils;

    @Mock
    private AssessmentStudentRepository assessmentStudentRepository;

    private AssessmentCompletionCurrentStudentsService service;

    private AssessmentCompletionSummaryResult completionSummaryResult;

    @BeforeEach
    void setUp() {
        service = new AssessmentCompletionCurrentStudentsService(restUtils, assessmentStudentRepository, new ObjectMapper());
        completionSummaryResult = new AssessmentCompletionSummaryResult() {
            @Override
            public String getPen() {
                return "123456789";
            }

            @Override
            public Integer getLte10Completed() {
                return 1;
            }

            @Override
            public Integer getNme10Completed() {
                return 0;
            }

            @Override
            public Integer getNmf10Completed() {
                return 1;
            }

            @Override
            public Integer getLte12Completed() {
                return 0;
            }

            @Override
            public Integer getLtf12Completed() {
                return 1;
            }

            @Override
            public Integer getLtp10Completed() {
                return 0;
            }

            @Override
            public Integer getLtp12Completed() {
                return 1;
            }
        };
    }

    @Test
    void testGetCurrentStudentsChunk_ShouldFetchGradStudentsAndCompletionSummary() {
        PaginatedResponse<ReportGradStudentData> gradPage = new PaginatedResponse<>(
                List.of(
                        ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("123456789").studentStatus("CUR").build(),
                        ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("987654321").studentStatus("CUR").build()
                ),
                PageRequest.of(0, 1000),
                2L
        );

        when(restUtils.getGradStudentReportPage("criteria", 0, 1000)).thenReturn(gradPage);
        when(assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(List.of("123456789", "987654321")))
                .thenReturn(List.of(completionSummaryResult));

        var result = service.getCurrentStudentsChunk("criteria", 0);

        assertNotNull(result);
        assertEquals(2, result.getGradStudentsPage().getContent().size());
        assertEquals(1, result.getAssessmentCompletionByPen().size());
        assertEquals(1, result.getAssessmentCompletionByPen().get("123456789").getLte10Completed());
        verify(restUtils).getGradStudentReportPage("criteria", 0, 1000);
        verify(assessmentStudentRepository).findAssessmentCompletionSummaryByPenIn(List.of("123456789", "987654321"));
    }

    @Test
    void testGetCurrentStudentsChunk_WhenNoPens_ShouldSkipCompletionQuery() {
        PaginatedResponse<ReportGradStudentData> gradPage = new PaginatedResponse<>(
                List.of(ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("").studentStatus("CUR").build()),
                PageRequest.of(1, 1000),
                1L
        );

        when(restUtils.getGradStudentReportPage("criteria", 1, 1000)).thenReturn(gradPage);

        var result = service.getCurrentStudentsChunk("criteria", 1);

        assertNotNull(result);
        assertEquals(1, result.getGradStudentsPage().getContent().size());
        assertEquals(Map.of(), result.getAssessmentCompletionByPen());
        verify(restUtils).getGradStudentReportPage("criteria", 1, 1000);
    }

    @Test
    void testGetSchoolCurrentStudentsChunk_ShouldBuildSchoolSearchCriteria() {
        PaginatedResponse<ReportGradStudentData> gradPage = new PaginatedResponse<>(
                List.of(ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("123456789").studentStatus("CUR").build()),
                PageRequest.of(0, 1000),
                1L
        );
        when(restUtils.getGradStudentReportPage(anyString(), anyInt(), anyInt())).thenReturn(gradPage);
        when(assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(List.of("123456789")))
                .thenReturn(List.of(completionSummaryResult));

        service.getSchoolCurrentStudentsChunk("school-id", 0);

        ArgumentCaptor<String> criteriaCaptor = ArgumentCaptor.forClass(String.class);
        verify(restUtils).getGradStudentReportPage(criteriaCaptor.capture(), anyInt(), anyInt());
        assertTrue(criteriaCaptor.getValue().contains("\"schoolOfRecordId\""));
        assertTrue(criteriaCaptor.getValue().contains("\"studentStatus\""));
        assertTrue(criteriaCaptor.getValue().contains("school-id"));
    }

    @Test
    void testGetDistrictCurrentStudentsChunk_ShouldBuildDistrictSearchCriteria() {
        PaginatedResponse<ReportGradStudentData> gradPage = new PaginatedResponse<>(
                List.of(ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("123456789").studentStatus("CUR").build()),
                PageRequest.of(0, 1000),
                1L
        );
        when(restUtils.getGradStudentReportPage(anyString(), anyInt(), anyInt())).thenReturn(gradPage);
        when(assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(List.of("123456789")))
                .thenReturn(List.of(completionSummaryResult));

        service.getDistrictCurrentStudentsChunk("district-id", 0);

        ArgumentCaptor<String> criteriaCaptor = ArgumentCaptor.forClass(String.class);
        verify(restUtils).getGradStudentReportPage(criteriaCaptor.capture(), anyInt(), anyInt());
        assertTrue(criteriaCaptor.getValue().contains("\"districtId\""));
        assertTrue(criteriaCaptor.getValue().contains("\"schoolCategoryCode\""));
        assertTrue(criteriaCaptor.getValue().contains("\"PUBLIC\""));
    }
}
