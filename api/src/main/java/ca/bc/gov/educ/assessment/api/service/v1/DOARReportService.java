package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.reports.DOARColumnLookup;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.DOARCalculate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@Slf4j
public class DOARReportService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final DOARCalculateService doarCalculateService;
    private final RestUtils restUtils;
    private EnumMap<DOARColumnLookup, DOARCalculate> map;
    private static final String SCHOOL_ID = "schoolID";
    private static final String SESSION_ID = "sessionID";

    public DOARReportService(AssessmentSessionRepository assessmentSessionRepository, AssessmentStudentRepository assessmentStudentRepository, DOARCalculateService doarCalculateService, RestUtils restUtils) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.doarCalculateService = doarCalculateService;
        this.restUtils = restUtils;
        init();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<List<String>> generateDetailedDOARBySchoolAndAssessmentType(UUID sessionID, UUID schoolID, String assessmentTypeCode) {
        List<List<String>> csvRecords = new ArrayList<>();
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        var schoolTombstone = this.restUtils.getSchoolBySchoolID(schoolID.toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, schoolID.toString()));

        AssessmentEntity nmeAssessmentEntity = session.getAssessments().stream().filter(assessmentEntity -> assessmentEntity.getAssessmentTypeCode().equalsIgnoreCase(assessmentTypeCode)).findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentEntity.class, "assessmentTypeCode", assessmentTypeCode));
        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndSchoolAtWriteSchoolID(nmeAssessmentEntity.getAssessmentID(), schoolID);

        for (AssessmentStudentEntity result : results) {
            var selectedAssessmentForm = nmeAssessmentEntity.getAssessmentForms().stream()
                    .filter(assessmentFormEntity -> Objects.equals(assessmentFormEntity.getAssessmentFormID(), result.getAssessmentFormID()))
                    .findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentFormEntity.class, "assessmentFormID", result.getAssessmentFormID().toString()));
            List<String> csvRowData = prepareNMEDOARForCsv(result, selectedAssessmentForm, schoolTombstone.getMincode());
            csvRecords.add(csvRowData);
        }
        return csvRecords;
    }

    public List<String> prepareNMEDOARForCsv(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String mincode) {
        return new ArrayList<>(Arrays.asList(
                selectedAssessmentForm.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + selectedAssessmentForm.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth(),
                mincode,
                selectedAssessmentForm.getAssessmentEntity().getAssessmentTypeCode(),
                student.getPen(),
                student.getLocalID(),
                student.getSurname(),
                student.getGivenName(),
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : "",
                student.getProvincialSpecialCaseCode(),
                getStudentTotals("BOTH", "P", selectedAssessmentForm, student),
                getStudentTotals("BOTH", "R", selectedAssessmentForm, student),
                getStudentTotals("BOTH", "F", selectedAssessmentForm, student),
                getStudentTotals("BOTH", "M", selectedAssessmentForm, student),
                getStudentTotals("MUL_CHOICE", "I", selectedAssessmentForm, student),
                getStudentTotals("MUL_CHOICE", "P", selectedAssessmentForm, student),
                getStudentTotals("MUL_CHOICE", "S", selectedAssessmentForm, student),
                getStudentTotals("MUL_CHOICE", "N", selectedAssessmentForm, student),
                getStudentTotals("BOTH", "7", selectedAssessmentForm, student),
                getStudentTotals("BOTH", "7", selectedAssessmentForm, student),
                getStudentTotals("BOTH", "9", selectedAssessmentForm, student)
        ));
    }

    public String getStudentTotals(String componentType, String code, AssessmentFormEntity selectedAssessmentForm, AssessmentStudentEntity student) {
        return map.get(DOARColumnLookup.getDOARColumn(componentType, code)).calculateTotal(selectedAssessmentForm, student, code);
    }

    @PostConstruct
    public void init() {
        map = new EnumMap<>(DOARColumnLookup.class);

        map.put(DOARColumnLookup.ENTRY1, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "OPEN_ENDED");
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });

        map.put(DOARColumnLookup.ENTRY2, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "OPEN_ENDED");
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });

        map.put(DOARColumnLookup.ENTRY3, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "OPEN_ENDED");
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });

        map.put(DOARColumnLookup.ENTRY4, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "OPEN_ENDED");
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });

        map.put(DOARColumnLookup.ENTRY5, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getClaimCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });

        map.put(DOARColumnLookup.ENTRY6, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getClaimCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });

        map.put(DOARColumnLookup.ENTRY7, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getClaimCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });

        map.put(DOARColumnLookup.ENTRY8, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getClaimCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });

        map.put(DOARColumnLookup.ENTRY9, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "OPEN_ENDED");
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });

        map.put(DOARColumnLookup.ENTRY10, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "OPEN_ENDED");
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });

        map.put(DOARColumnLookup.ENTRY11, (selectedAssessmentForm, student, code) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "OPEN_ENDED");
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, "MUL_CHOICE");
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });
    }

    private List<AssessmentQuestionEntity> getTaskCodeQuestionsForSelectedForm(AssessmentFormEntity selectedAssessmentForm, String taskCode, String componentTypeCode) {
        if(!selectedAssessmentForm.getAssessmentComponentEntities().isEmpty()) {
            var component =  selectedAssessmentForm.getAssessmentComponentEntities().stream()
                    .filter(assessmentComponentEntity ->
                            assessmentComponentEntity.getComponentTypeCode().equalsIgnoreCase(componentTypeCode))
                    .findFirst();

            if(component.isPresent() && !component.get().getAssessmentQuestionEntities().isEmpty()) {
                return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                        .flatMap(Collection::stream)
                        .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getTaskCode()) && assessmentQuestionEntity.getTaskCode().equalsIgnoreCase(taskCode))
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private List<AssessmentQuestionEntity> getClaimCodeQuestionsForSelectedForm(AssessmentFormEntity selectedAssessmentForm, String taskCode, String componentTypeCode) {
        if(!selectedAssessmentForm.getAssessmentComponentEntities().isEmpty()) {
            var component = selectedAssessmentForm.getAssessmentComponentEntities().stream()
                    .filter(assessmentComponentEntity ->
                            assessmentComponentEntity.getComponentTypeCode().equalsIgnoreCase(componentTypeCode))
                    .findFirst();

            if(component.isPresent() && !component.get().getAssessmentQuestionEntities().isEmpty()) {
                return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                        .flatMap(Collection::stream)
                        .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getClaimCode()) && assessmentQuestionEntity.getClaimCode().equalsIgnoreCase(taskCode))
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private List<AssessmentQuestionEntity> getCognitiveLevelCodeQuestionsForSelectedForm(AssessmentFormEntity selectedAssessmentForm, String taskCode, String componentTypeCode) {
        if(!selectedAssessmentForm.getAssessmentComponentEntities().isEmpty()) {
            var component = selectedAssessmentForm.getAssessmentComponentEntities().stream()
                    .filter(assessmentComponentEntity ->
                            assessmentComponentEntity.getComponentTypeCode().equalsIgnoreCase(componentTypeCode))
                    .findFirst();

            if(component.isPresent() && !component.get().getAssessmentQuestionEntities().isEmpty()) {
                return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                        .flatMap(Collection::stream)
                        .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getCognitiveLevelCode()) && assessmentQuestionEntity.getCognitiveLevelCode().equalsIgnoreCase(taskCode))
                        .toList();
            }
        }
        return Collections.emptyList();
    }
}
