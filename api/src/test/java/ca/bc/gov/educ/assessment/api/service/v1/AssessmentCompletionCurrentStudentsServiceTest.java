package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.AssessmentCompletionCurrentStudentPage;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.ReportGradStudentData;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentCompletionSummaryResult;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentStudentLocalIdResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentCompletionCurrentStudentsServiceTest {
    private static final int DEFAULT_PAGE_SIZE = 2000;

    @Mock
    private RestUtils restUtils;

    @Mock
    private AssessmentStudentRepository assessmentStudentRepository;

    private AssessmentCompletionCurrentStudentsService service;

    private AssessmentCompletionSummaryResult completionSummaryResult;
    private AssessmentStudentLocalIdResult localIdResult;

    AssessmentCompletionCurrentStudentsServiceTest() {
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

        localIdResult = new AssessmentStudentLocalIdResult() {
            @Override
            public String getPen() {
                return "123456789";
            }

            @Override
            public String getLocalID() {
                return "A001";
            }
        };
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AssessmentCompletionCurrentStudentsService(restUtils, assessmentStudentRepository);
    }

    @Test
    void testGetSchoolCurrentStudentsChunk_ShouldFetchGradStudentsCompletionSummaryAndLocalIds() {
        AssessmentCompletionCurrentStudentPage gradPage = AssessmentCompletionCurrentStudentPage.builder()
                .content(List.of(
                        ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("123456789").studentStatus("CUR").build(),
                        ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("987654321").studentStatus("CUR").build()
                ))
                .pageNumber(0)
                .pageSize(DEFAULT_PAGE_SIZE)
                .numberOfElements(2)
                .hasNext(false)
                .build();

        when(restUtils.getGradAssessmentCompletionCurrentStudentsPage("schoolId", "school-id", 0, DEFAULT_PAGE_SIZE)).thenReturn(gradPage);
        when(assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(List.of("123456789", "987654321")))
                .thenReturn(List.of(completionSummaryResult));
        when(assessmentStudentRepository.findStudentLocalIdsByPenIn(List.of("123456789", "987654321")))
                .thenReturn(List.of(localIdResult));

        var result = service.getSchoolCurrentStudentsChunk("school-id", 0);

        assertNotNull(result);
        assertEquals(2, result.getGradStudentsPage().getContent().size());
        assertEquals(1, result.getAssessmentCompletionByPen().size());
        assertEquals("A001", result.getLocalIdByPen().get("123456789"));
        assertEquals(1, result.getAssessmentCompletionByPen().get("123456789").getLte10Completed());
        verify(restUtils).getGradAssessmentCompletionCurrentStudentsPage("schoolId", "school-id", 0, DEFAULT_PAGE_SIZE);
        verify(assessmentStudentRepository).findAssessmentCompletionSummaryByPenIn(List.of("123456789", "987654321"));
        verify(assessmentStudentRepository).findStudentLocalIdsByPenIn(List.of("123456789", "987654321"));
    }

    @Test
    void testGetDistrictCurrentStudentsChunk_WhenNoPens_ShouldSkipEnrichmentQueries() {
        AssessmentCompletionCurrentStudentPage gradPage = AssessmentCompletionCurrentStudentPage.builder()
                .content(List.of(ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("").studentStatus("CUR").build()))
                .pageNumber(1)
                .pageSize(DEFAULT_PAGE_SIZE)
                .numberOfElements(1)
                .hasNext(false)
                .build();

        when(restUtils.getGradAssessmentCompletionCurrentStudentsPage("districtId", "district-id", 1, DEFAULT_PAGE_SIZE)).thenReturn(gradPage);

        var result = service.getDistrictCurrentStudentsChunk("district-id", 1);

        assertNotNull(result);
        assertEquals(1, result.getGradStudentsPage().getContent().size());
        assertEquals(Map.of(), result.getAssessmentCompletionByPen());
        assertEquals(Map.of(), result.getLocalIdByPen());
        verify(restUtils).getGradAssessmentCompletionCurrentStudentsPage("districtId", "district-id", 1, DEFAULT_PAGE_SIZE);
    }

    @Test
    void testGetSchoolCurrentStudentsChunk_UsesSchoolScopeEndpoint() {
        AssessmentCompletionCurrentStudentPage gradPage = AssessmentCompletionCurrentStudentPage.builder()
                .content(List.of(ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("123456789").studentStatus("CUR").build()))
                .pageNumber(0)
                .pageSize(DEFAULT_PAGE_SIZE)
                .numberOfElements(1)
                .hasNext(false)
                .build();
        when(restUtils.getGradAssessmentCompletionCurrentStudentsPage("schoolId", "school-id", 0, DEFAULT_PAGE_SIZE)).thenReturn(gradPage);
        when(assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(List.of("123456789")))
                .thenReturn(List.of(completionSummaryResult));
        when(assessmentStudentRepository.findStudentLocalIdsByPenIn(List.of("123456789")))
                .thenReturn(List.of(localIdResult));

        service.getSchoolCurrentStudentsChunk("school-id", 0);

        verify(restUtils).getGradAssessmentCompletionCurrentStudentsPage("schoolId", "school-id", 0, DEFAULT_PAGE_SIZE);
    }

    @Test
    void testGetDistrictCurrentStudentsChunk_UsesDistrictScopeEndpoint() {
        AssessmentCompletionCurrentStudentPage gradPage = AssessmentCompletionCurrentStudentPage.builder()
                .content(List.of(ReportGradStudentData.builder().graduationStudentRecordId(UUID.randomUUID()).pen("123456789").studentStatus("CUR").build()))
                .pageNumber(0)
                .pageSize(DEFAULT_PAGE_SIZE)
                .numberOfElements(1)
                .hasNext(false)
                .build();
        when(restUtils.getGradAssessmentCompletionCurrentStudentsPage("districtId", "district-id", 0, DEFAULT_PAGE_SIZE)).thenReturn(gradPage);
        when(assessmentStudentRepository.findAssessmentCompletionSummaryByPenIn(List.of("123456789")))
                .thenReturn(List.of(completionSummaryResult));
        when(assessmentStudentRepository.findStudentLocalIdsByPenIn(List.of("123456789")))
                .thenReturn(List.of(localIdResult));

        service.getDistrictCurrentStudentsChunk("district-id", 0);

        verify(restUtils).getGradAssessmentCompletionCurrentStudentsPage("districtId", "district-id", 0, DEFAULT_PAGE_SIZE);
    }
}
