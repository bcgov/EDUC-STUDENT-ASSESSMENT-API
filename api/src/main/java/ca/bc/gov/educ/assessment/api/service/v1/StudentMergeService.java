package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.EventStatus;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentMergeDirectionCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.mappers.v1.StudentMergeMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMergeResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class StudentMergeService {

    private static final StudentMergeMapper mapper = StudentMergeMapper.mapper;
    private final RestUtils restUtils;
    private final AssessmentStudentService assessmentStudentService;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentEventRepository assessmentEventRepository;

    @Autowired
    public StudentMergeService(AssessmentStudentService assessmentStudentService, AssessmentEventRepository assessmentEventRepository, AssessmentStudentRepository assessmentStudentRepository, RestUtils restUtils) {
        this.restUtils = restUtils;
        this.assessmentStudentService = assessmentStudentService;
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.assessmentEventRepository = assessmentEventRepository;
    }

    public List<StudentMergeResult> getMergedStudentsForDateRange(String createDateStart, String createDateEnd) {
        UUID correlationID = UUID.randomUUID();
        List<StudentMergeResult> mergeStudentResults = new ArrayList<>();
        log.info("Fetching student merge records created between {} and {} with correlation ID: {}", createDateStart, createDateEnd, correlationID);
        List<StudentMerge> results = restUtils.getMergedStudentsForDateRange(correlationID, createDateStart, createDateEnd);
        if (!CollectionUtils.isEmpty(results)) {
            Set<String> studentIDs = results.stream()
                    .flatMap(studentMerge -> Stream.of(studentMerge.getStudentID(), studentMerge.getMergeStudentID()))
                    .collect(Collectors.toSet());
            List<Student> students = Optional.ofNullable(restUtils.getStudents(correlationID, studentIDs)).orElseGet(Collections::emptyList);
            results.stream()
                    .forEach(studentMerge -> {
                        StudentMergeResult studentMergeResult = mapper.toStructure(studentMerge);
                        Optional<Student> mergedPEN = students.stream()
                                .filter(student -> student.getStudentID().equals(studentMerge.getStudentID()))
                                .findFirst();
                        Optional<Student> currentPEN = students.stream()
                                .filter(student -> student.getStudentID().equals(studentMerge.getMergeStudentID()))
                                .findFirst();

                        currentPEN.ifPresent(pen -> studentMergeResult.setCurrentPEN(pen.getPen()));
                        mergedPEN.ifPresent(pen -> studentMergeResult.setMergedPEN(pen.getPen()));
                        mergeStudentResults.add(studentMergeResult);
                    });
        }
        return mergeStudentResults;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processMergeEvent(AssessmentEventEntity event) throws JsonProcessingException {
        final EventType mergeType = EventType.valueOf(event.getEventType());
        final List<StudentMerge> studentMerges = new ObjectMapper().readValue(event.getEventPayload(), new TypeReference<>() {});

        studentMerges.stream().filter(this::mergeToPredicate).findFirst().ifPresent(studentMerge -> {
            this.updateAssessmentStudents(studentMerge, mergeType);
        });

        this.assessmentEventRepository.findByEventId(event.getEventId()).ifPresent(existingEvent -> {
            existingEvent.setEventStatus(EventStatus.PROCESSED.toString());
            existingEvent.setUpdateDate(LocalDateTime.now());
            this.assessmentEventRepository.save(existingEvent);
        });
    }

    private void updateAssessmentStudents(StudentMerge studentMerge, EventType mergeType) {
        if (mergeType.equals(EventType.CREATE_MERGE) || mergeType.equals(EventType.DELETE_MERGE)) {
            final UUID studentID = UUID.fromString(studentMerge.getStudentID());
            final StudentStatusCodes statusCode = mergeType.equals(EventType.CREATE_MERGE)
                ? StudentStatusCodes.MERGED
                : StudentStatusCodes.ACTIVE;
            List<AssessmentStudentEntity> assessmentStudents = this.assessmentStudentService.getStudentByStudentId(studentID);

            assessmentStudents.stream().forEach(assessmentStudent -> {
                assessmentStudent.setStudentStatus(statusCode.toString());
                this.assessmentStudentRepository.save(assessmentStudent);
            });
           }
    }

    private boolean mergeToPredicate(final StudentMerge studentMerge) {
        return StringUtils.equals(studentMerge.getStudentMergeDirectionCode(), StudentMergeDirectionCodes.TO.toString());
    }

}
