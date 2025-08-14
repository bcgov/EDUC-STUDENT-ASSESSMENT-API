package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.EventStatus;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentMergeDirectionCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StudentMergeServiceTest {

    @Mock
    private RestUtils restUtils;

    @Mock
    private AssessmentStudentService assessmentStudentService;

    @Mock
    private AssessmentStudentRepository assessmentStudentRepository;

    @Mock
    private AssessmentEventRepository assessmentEventRepository;

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

        var result = studentMergeService.getMergedStudentsForDateRange(createDateStart, createDateEnd);
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

    @Test
    void testUpdateAssessmentStudents_WithCreateMerge_ShouldUpdateStudentsToMerged() throws Exception {
        UUID studentId = UUID.randomUUID();
        StudentMerge studentMerge = StudentMerge.builder().studentID(studentId.toString()).build();
        AssessmentStudentEntity assessmentStudent = AssessmentStudentEntity.builder()
                .studentID(studentId)
                .build();
        List<AssessmentStudentEntity> assessmentStudents = Collections.singletonList(assessmentStudent);

        when(assessmentStudentService.getStudentByStudentId(studentId)).thenReturn(assessmentStudents);

        Method method = StudentMergeService.class.getDeclaredMethod("updateAssessmentStudents", StudentMerge.class, EventType.class);
        method.setAccessible(true);

        method.invoke(studentMergeService, studentMerge, EventType.CREATE_MERGE);

        verify(assessmentStudentService, times(1)).getStudentByStudentId(studentId);
        verify(assessmentStudentRepository, times(1)).save(assessmentStudent);
        assertEquals(StudentStatusCodes.MERGED.toString(), assessmentStudent.getStudentStatusCode());
    }

    @Test
    void testUpdateAssessmentStudents_WithDeleteMerge_ShouldUpdateStudentsToActive() throws Exception {
        UUID studentId = UUID.randomUUID();
        StudentMerge studentMerge = StudentMerge.builder().studentID(studentId.toString()).build();
        AssessmentStudentEntity assessmentStudent = AssessmentStudentEntity.builder()
                .studentID(studentId)
                .build();
        List<AssessmentStudentEntity> assessmentStudents = Collections.singletonList(assessmentStudent);

        when(assessmentStudentService.getStudentByStudentId(studentId)).thenReturn(assessmentStudents);

        Method method = StudentMergeService.class.getDeclaredMethod("updateAssessmentStudents", StudentMerge.class, EventType.class);
        method.setAccessible(true);

        method.invoke(studentMergeService, studentMerge, EventType.DELETE_MERGE);

        verify(assessmentStudentService, times(1)).getStudentByStudentId(studentId);
        verify(assessmentStudentRepository, times(1)).save(assessmentStudent);
        assertEquals(StudentStatusCodes.ACTIVE.toString(), assessmentStudent.getStudentStatusCode());
    }

    @Test
    void testUpdateAssessmentStudents_WithOtherMergeType_ShouldNotUpdateStudents() throws Exception {
        UUID studentId = UUID.randomUUID();
        StudentMerge studentMerge = StudentMerge.builder().studentID(studentId.toString()).build();
        EventType otherEventType = EventType.GET_STUDENT;

        Method method = StudentMergeService.class.getDeclaredMethod("updateAssessmentStudents", StudentMerge.class, EventType.class);
        method.setAccessible(true);

        method.invoke(studentMergeService, studentMerge, otherEventType);

        verify(assessmentStudentService, never()).getStudentByStudentId(any(UUID.class));
        verify(assessmentStudentRepository, never()).save(any(AssessmentStudentEntity.class));
    }

    @Test
    void testProcessMergeEvent_WithCreateMergeAndDirectionTo_ShouldUpdateStudentsAndEvent() throws JsonProcessingException {
        UUID studentId = UUID.randomUUID();
        StudentMerge studentMergeTo = StudentMerge.builder()
                .studentID(studentId.toString())
                .studentMergeDirectionCode(StudentMergeDirectionCodes.TO.toString())
                .build();
        StudentMerge studentMergeFrom = StudentMerge.builder()
                .studentID(UUID.randomUUID().toString())
                .studentMergeDirectionCode(StudentMergeDirectionCodes.FROM.toString())
                .build();
        List<StudentMerge> studentMerges = List.of(studentMergeTo, studentMergeFrom);

        AssessmentEventEntity event = AssessmentEventEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType(EventType.CREATE_MERGE.toString())
                .eventPayload(new ObjectMapper().writeValueAsString(studentMerges))
                .build();

        AssessmentStudentEntity assessmentStudent = AssessmentStudentEntity.builder()
                .studentID(studentId)
                .build();
        List<AssessmentStudentEntity> assessmentStudents = Collections.singletonList(assessmentStudent);

        when(assessmentStudentService.getStudentByStudentId(studentId)).thenReturn(assessmentStudents);
        when(assessmentEventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentMergeService.processMergeEvent(event);

        verify(assessmentStudentService, times(1)).getStudentByStudentId(studentId);
        verify(assessmentStudentRepository, times(1)).save(assessmentStudent);
        assertEquals(StudentStatusCodes.MERGED.toString(), assessmentStudent.getStudentStatusCode());

        verify(assessmentEventRepository, times(1)).findByEventId(event.getEventId());
        verify(assessmentEventRepository, times(1)).save(event);
        assertEquals(EventStatus.PROCESSED.toString(), event.getEventStatus());
    }

    @Test
    void testProcessMergeEvent_WithDeleteMergeAndDirectionTo_ShouldUpdateStudentsAndEvent() throws JsonProcessingException {
        UUID studentId = UUID.randomUUID();
        StudentMerge studentMergeTo = StudentMerge.builder()
                .studentID(studentId.toString())
                .studentMergeDirectionCode(StudentMergeDirectionCodes.TO.toString())
                .build();
        List<StudentMerge> studentMerges = List.of(studentMergeTo);

        AssessmentEventEntity event = AssessmentEventEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType(EventType.DELETE_MERGE.toString())
                .eventPayload(new ObjectMapper().writeValueAsString(studentMerges))
                .build();

        AssessmentStudentEntity assessmentStudent = AssessmentStudentEntity.builder()
                .studentID(studentId)
                .build();
        List<AssessmentStudentEntity> assessmentStudents = Collections.singletonList(assessmentStudent);

        when(assessmentStudentService.getStudentByStudentId(studentId)).thenReturn(assessmentStudents);
        when(assessmentEventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentMergeService.processMergeEvent(event);

        verify(assessmentStudentService, times(1)).getStudentByStudentId(studentId);
        verify(assessmentStudentRepository, times(1)).save(assessmentStudent);
        assertEquals(StudentStatusCodes.ACTIVE.toString(), assessmentStudent.getStudentStatusCode());

        verify(assessmentEventRepository, times(1)).findByEventId(event.getEventId());
        verify(assessmentEventRepository, times(1)).save(event);
        assertEquals(EventStatus.PROCESSED.toString(), event.getEventStatus());
    }

    @Test
    void testProcessMergeEvent_WithNoDirectionTo_ShouldNotUpdateStudentsButUpdateEvent() throws JsonProcessingException {
        StudentMerge studentMergeFrom = StudentMerge.builder()
                .studentID(UUID.randomUUID().toString())
                .studentMergeDirectionCode(StudentMergeDirectionCodes.FROM.toString())
                .build();
        List<StudentMerge> studentMerges = List.of(studentMergeFrom);

        AssessmentEventEntity event = AssessmentEventEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType(EventType.CREATE_MERGE.toString())
                .eventPayload(new ObjectMapper().writeValueAsString(studentMerges))
                .build();

        when(assessmentEventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentMergeService.processMergeEvent(event);

        verify(assessmentStudentService, never()).getStudentByStudentId(any(UUID.class));
        verify(assessmentStudentRepository, never()).save(any(AssessmentStudentEntity.class));

        verify(assessmentEventRepository, times(1)).findByEventId(event.getEventId());
        verify(assessmentEventRepository, times(1)).save(event);
        assertEquals(EventStatus.PROCESSED.toString(), event.getEventStatus());
    }

    @Test
    void testProcessMergeEvent_WhenEventNotFound_ShouldNotUpdateEvent() throws JsonProcessingException {
        StudentMerge studentMergeFrom = StudentMerge.builder()
                .studentID(UUID.randomUUID().toString())
                .studentMergeDirectionCode(StudentMergeDirectionCodes.FROM.toString())
                .build();
        List<StudentMerge> studentMerges = List.of(studentMergeFrom);

        AssessmentEventEntity event = AssessmentEventEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType(EventType.CREATE_MERGE.toString())
                .eventPayload(new ObjectMapper().writeValueAsString(studentMerges))
                .build();

        when(assessmentEventRepository.findByEventId(event.getEventId())).thenReturn(Optional.empty());

        studentMergeService.processMergeEvent(event);

        verify(assessmentStudentService, never()).getStudentByStudentId(any(UUID.class));
        verify(assessmentStudentRepository, never()).save(any(AssessmentStudentEntity.class));

        verify(assessmentEventRepository, times(1)).findByEventId(event.getEventId());
        verify(assessmentEventRepository, never()).save(any(AssessmentEventEntity.class));
    }
}
