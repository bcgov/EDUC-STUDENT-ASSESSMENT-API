package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.mappers.v1.StudentMergeMapper;
import ca.bc.gov.educ.eas.api.rest.RestUtils;
import ca.bc.gov.educ.eas.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMergeResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class StudentMergeService {

    private static final StudentMergeMapper mapper = StudentMergeMapper.mapper;
    private final RestUtils restUtils;

    @Autowired
    public StudentMergeService(RestUtils restUtils) {
        this.restUtils = restUtils;
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
}
