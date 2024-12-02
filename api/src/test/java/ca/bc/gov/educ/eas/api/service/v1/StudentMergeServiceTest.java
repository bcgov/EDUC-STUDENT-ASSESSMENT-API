package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EasAPIRuntimeException;
import ca.bc.gov.educ.eas.api.rest.RestUtils;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

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
        when(restUtils.getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)))
                .thenReturn(mockMergedStudents);

        val result = studentMergeService.getMergedStudentsForDateRange(createDateStart, createDateEnd);

        assertEquals(mockMergedStudents.size(), result.size());
        verify(restUtils, times(1)).getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)); // Use any(UUID.class) here
    }

    @Test
    void testGetMergedStudentsForDateRange_WhenRestUtilsThrowsException_ShouldThrowEasAPIRuntimeException() {
        String createDateStart = "2024-02-01T00:00:00";
        String createDateEnd = "2024-09-01T00:00:00";

        when(restUtils.getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)))
                .thenThrow(new EasAPIRuntimeException("PEN Services API failed"));

        assertThrows(EasAPIRuntimeException.class, () -> studentMergeService.getMergedStudentsForDateRange(createDateStart, createDateEnd));
        verify(restUtils, times(1)).getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)); // Use any(UUID.class) here
    }
}