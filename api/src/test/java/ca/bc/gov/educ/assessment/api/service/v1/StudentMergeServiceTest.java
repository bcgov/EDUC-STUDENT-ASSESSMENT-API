package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StudentMergeServiceTest {

    @Mock
    private RestUtils restUtils;

    @InjectMocks
    private StudentMergeService studentMergeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetMergedStudentsForDateRange_ShouldReturnMergedStudents() {
        String createDateStart = "2024-02-01T00:00:00";
        String createDateEnd = "2024-09-01T00:00:00";
        List<StudentMerge> mockMergedStudents = List.of(
                StudentMerge.builder().studentMergeID(UUID.randomUUID().toString()).studentID(UUID.randomUUID().toString()).mergeStudentID(UUID.randomUUID().toString()).studentMergeDirectionCode("FROM").studentMergeSourceCode("MI").build(),
                StudentMerge.builder().studentMergeID(UUID.randomUUID().toString()).studentID(UUID.randomUUID().toString()).mergeStudentID(UUID.randomUUID().toString()).studentMergeDirectionCode("TO").studentMergeSourceCode("API").build()
        );
        List<Student> mockStudents = new ArrayList<>();
        Set<String> studentIDs = new HashSet<>();
        for (StudentMerge mockMergedStudent : mockMergedStudents) {
            mockStudents.add(Student.builder().studentID(mockMergedStudent.getStudentID()).pen("123456789").build());
            mockStudents.add(Student.builder().studentID(mockMergedStudent.getMergeStudentID()).pen("987654321").build());
            studentIDs.add(mockMergedStudent.getStudentID());
            studentIDs.add(mockMergedStudent.getMergeStudentID());
        }

        when(restUtils.getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)))
                .thenReturn(mockMergedStudents);
        when(restUtils.getStudents(any(), any())).thenReturn(mockStudents);

        val result = studentMergeService.getMergedStudentsForDateRange(createDateStart, createDateEnd);
        assertEquals(mockMergedStudents.size(), result.size());
        verify(restUtils, times(1)).getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)); // Use any(UUID.class) here
    }

    @Test
    void testGetMergedStudentsForDateRange_WhenRestUtilsThrowsException_ShouldThrowAssessmentAPIRuntimeException() {
        String createDateStart = "2024-02-01T00:00:00";
        String createDateEnd = "2024-09-01T00:00:00";

        when(restUtils.getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)))
                .thenThrow(new StudentAssessmentAPIRuntimeException("PEN Services API failed"));

        assertThrows(StudentAssessmentAPIRuntimeException.class, () -> studentMergeService.getMergedStudentsForDateRange(createDateStart, createDateEnd));
        verify(restUtils, times(1)).getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)); // Use any(UUID.class) here
    }
}
