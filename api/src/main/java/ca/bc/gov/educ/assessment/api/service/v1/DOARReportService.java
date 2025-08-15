package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.reports.*;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidParameterException;
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
    private static final String OPEN_ENDED = "OPEN_ENDED";
    private static final String MUL_CHOICE = "MUL_CHOICE";
    private static final String BOTH = "BOTH";
    private static final String LTP12 = "LTP12";
    private static final String LTP10 = "LTP10";
    private static final String LTF12= "LTF12";
    private static final String[] allowedSelectedResponse = new String[]{"I", "E"};

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

        AssessmentEntity assessmentEntity = session.getAssessments().stream().filter(entity -> entity.getAssessmentTypeCode().equalsIgnoreCase(assessmentTypeCode)).findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentEntity.class, "assessmentTypeCode", assessmentTypeCode));
        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndSchoolAtWriteSchoolID(assessmentEntity.getAssessmentID(), schoolID);

        for (AssessmentStudentEntity result : results) {
            var selectedAssessmentForm = assessmentEntity.getAssessmentForms().stream()
                    .filter(assessmentFormEntity -> Objects.equals(assessmentFormEntity.getAssessmentFormID(), result.getAssessmentFormID()))
                    .findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentFormEntity.class, "assessmentFormID", result.getAssessmentFormID().toString()));
            List<String> csvRowData = prepareDOARForCsv(result, selectedAssessmentForm, schoolTombstone.getMincode(), assessmentTypeCode);
            csvRecords.add(csvRowData);
        }
        return csvRecords;
    }

    public String getStudentTotals(String componentType, String code, AssessmentFormEntity selectedAssessmentForm, AssessmentStudentEntity student, String assessmentTypeCode, boolean includeChoiceCalc) {
        var lookup = map.get(DOARColumnLookup.getDOARColumn(componentType, code, assessmentTypeCode));
        if (lookup != null) {
            return map.get(DOARColumnLookup.getDOARColumn(componentType, code, assessmentTypeCode)).calculateTotal(selectedAssessmentForm, student, code, includeChoiceCalc);
        } else {
            throw new InvalidParameterException("Column type does not exist for this report");
        }
    }

    @PostConstruct
    public void init() {
        map = new EnumMap<>(DOARColumnLookup.class);

        map.put(DOARColumnLookup.ENTRY1, (selectedAssessmentForm, student, code, includeChoiceCalc) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, OPEN_ENDED);
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, MUL_CHOICE);
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });
        map.put(DOARColumnLookup.ENTRY2, (selectedAssessmentForm, student, code, includeChoiceCalc) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getClaimCodeQuestionsForSelectedForm(selectedAssessmentForm, code, student, MUL_CHOICE, includeChoiceCalc);
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });
        map.put(DOARColumnLookup.ENTRY3, (selectedAssessmentForm, student, code, includeChoiceCalc) -> {
            List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, student, OPEN_ENDED, false);
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getCognitiveLevelCodeQuestionsForSelectedForm(selectedAssessmentForm, code, student, MUL_CHOICE, includeChoiceCalc);
            return String.valueOf(doarCalculateService.calculateTotal(student, selectedOeAssessmentQuestionsByTypeCode, selectedMcAssessmentQuestionsByTypeCode));
        });
        map.put(DOARColumnLookup.ENTRY4, (selectedAssessmentForm, student, code, includeChoiceCalc) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getClaimCodeQuestionsForSelectedForm(selectedAssessmentForm, code, student, OPEN_ENDED, false);
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });
        map.put(DOARColumnLookup.ENTRY5, (selectedAssessmentForm, student, code, includeChoiceCalc) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getTaskCodeQuestionsForSelectedForm(selectedAssessmentForm, code, MUL_CHOICE);
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });
        map.put(DOARColumnLookup.ENTRY6, (selectedAssessmentForm, student, code, includeChoiceCalc) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getConceptsCodeQuestionsForSelectedForm(selectedAssessmentForm, code, student, MUL_CHOICE, includeChoiceCalc);
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });
        map.put(DOARColumnLookup.ENTRY7, (selectedAssessmentForm, student, code, includeChoiceCalc) -> {
            List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode = getQuestionsWithAssessmentSectionForSelectedForm(selectedAssessmentForm, code, student, MUL_CHOICE, includeChoiceCalc);
            return String.valueOf(doarCalculateService.calculateMCTotal(selectedMcAssessmentQuestionsByTypeCode, student));
        });
    }

    private List<String> prepareDOARForCsv(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String mincode, String assessmentTypeCode) {
        return switch (assessmentTypeCode) {
            case "NME10", "NMF10" -> prepareNMEDOARForCsv(student, selectedAssessmentForm, mincode);
            case "LTE10", "LTE12" -> prepareLTEDOARForCsv(student, selectedAssessmentForm, mincode);
            case LTP12 -> prepareLTP12DOARForCsv(student, selectedAssessmentForm, mincode);
            case LTP10 -> prepareLTP10DOARForCsv(student, selectedAssessmentForm, mincode);
            case LTF12 -> prepareLTF12DOARForCsv(student, selectedAssessmentForm, mincode);
            default -> Collections.emptyList();
        };
    }

    private List<String> prepareLTEDOARForCsv(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String mincode) {
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
                getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals(MUL_CHOICE, "B", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals(OPEN_ENDED, "GO", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals(OPEN_ENDED, "WRA", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals(OPEN_ENDED, "WRB", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals("BOTH", "7", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals("BOTH", "8", selectedAssessmentForm, student, "LTE", true),
                getStudentTotals("BOTH", "9", selectedAssessmentForm, student, "LTE", true)
        ));
    }

    private List<String> prepareLTP12DOARForCsv(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String mincode) {
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
                getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "O", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(MUL_CHOICE, "B", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "GO", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "WRA", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "WRB", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "O1", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "O2", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(OPEN_ENDED, "O3", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(BOTH, "7", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(BOTH, "8", selectedAssessmentForm, student, LTP12, true),
                getStudentTotals(BOTH, "9", selectedAssessmentForm, student, LTP12, true)
        ));
    }

    private List<String> prepareLTP10DOARForCsv(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String mincode) {
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
                getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "O", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(MUL_CHOICE, "B", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "WRS", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "GO", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "WRS", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "WRA", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "WRB", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "O1", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "O2", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(OPEN_ENDED, "O3", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(BOTH, "7", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(BOTH, "8", selectedAssessmentForm, student, LTP10, true),
                getStudentTotals(BOTH, "9", selectedAssessmentForm, student, LTP10, true)
        ));
    }

    private List<String> prepareLTF12DOARForCsv(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String mincode) {
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
                getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "O", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, LTF12, false),
                getStudentTotals(MUL_CHOICE, "I", selectedAssessmentForm, student, LTF12, false),
                getStudentTotals(MUL_CHOICE, "E", selectedAssessmentForm, student, LTF12, false),
                getStudentTotals(OPEN_ENDED, "WRS", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "WRD", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "WRF", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "O1D", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "O1F", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "O1E", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "O2D", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "O2F", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(OPEN_ENDED, "O2E", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(BOTH, "7", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(BOTH, "8", selectedAssessmentForm, student, LTF12, true),
                getStudentTotals(BOTH, "9", selectedAssessmentForm, student, LTF12, true)
        ));
    }

    private List<String> prepareNMEDOARForCsv(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String mincode) {
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
                getStudentTotals(BOTH, "P", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(BOTH, "R", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(BOTH, "F", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(BOTH, "M", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(MUL_CHOICE, "I", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(MUL_CHOICE, "P", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(MUL_CHOICE, "S", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(MUL_CHOICE, "N", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(BOTH, "7", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(BOTH, "8", selectedAssessmentForm, student, "NME", false),
                getStudentTotals(BOTH, "9", selectedAssessmentForm, student, "NME", false)
        ));
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

    private List<AssessmentQuestionEntity> getClaimCodeQuestionsForSelectedForm(AssessmentFormEntity selectedAssessmentForm, String taskCode, AssessmentStudentEntity student, String componentTypeCode, boolean includeChoiceCalc) {
        if(!selectedAssessmentForm.getAssessmentComponentEntities().isEmpty()) {
            var component = selectedAssessmentForm.getAssessmentComponentEntities().stream()
                    .filter(assessmentComponentEntity ->
                            assessmentComponentEntity.getComponentTypeCode().equalsIgnoreCase(componentTypeCode))
                    .findFirst();

            if(component.isPresent() && !component.get().getAssessmentQuestionEntities().isEmpty()) {
                var responseNotSelected = getChoicePathNotSelected(student, component.get());

                if(includeChoiceCalc) {
                    return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                            .flatMap(Collection::stream)
                            .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getClaimCode())
                                    && StringUtils.isNotBlank(responseNotSelected)
                                    && assessmentQuestionEntity.getTaskCode().equalsIgnoreCase(responseNotSelected)
                                    && assessmentQuestionEntity.getClaimCode().equalsIgnoreCase(taskCode))
                            .toList();
                }
                return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                        .flatMap(Collection::stream)
                        .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getClaimCode()) && assessmentQuestionEntity.getClaimCode().equalsIgnoreCase(taskCode))
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private List<AssessmentQuestionEntity> getConceptsCodeQuestionsForSelectedForm(AssessmentFormEntity selectedAssessmentForm, String taskCode, AssessmentStudentEntity student, String componentTypeCode, boolean includeChoiceCalc) {
        if(!selectedAssessmentForm.getAssessmentComponentEntities().isEmpty()) {
            var component = selectedAssessmentForm.getAssessmentComponentEntities().stream()
                    .filter(assessmentComponentEntity ->
                            assessmentComponentEntity.getComponentTypeCode().equalsIgnoreCase(componentTypeCode))
                    .findFirst();

            if(component.isPresent() && !component.get().getAssessmentQuestionEntities().isEmpty()) {
                var responseNotSelected = getChoicePathNotSelected(student, component.get());
                if(includeChoiceCalc) {
                    return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                            .flatMap(Collection::stream)
                            .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getConceptCode())
                                    && StringUtils.isNotBlank(responseNotSelected)
                                    && !assessmentQuestionEntity.getTaskCode().equalsIgnoreCase(responseNotSelected)
                                    && assessmentQuestionEntity.getConceptCode().equalsIgnoreCase(taskCode))
                            .toList();
                }
                return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                        .flatMap(Collection::stream)
                        .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getConceptCode()) && assessmentQuestionEntity.getConceptCode().equalsIgnoreCase(taskCode))
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private List<AssessmentQuestionEntity> getCognitiveLevelCodeQuestionsForSelectedForm(AssessmentFormEntity selectedAssessmentForm, String taskCode, AssessmentStudentEntity student, String componentTypeCode, boolean includeChoiceCalc) {
        if(!selectedAssessmentForm.getAssessmentComponentEntities().isEmpty()) {
            var component = selectedAssessmentForm.getAssessmentComponentEntities().stream()
                    .filter(assessmentComponentEntity ->
                            assessmentComponentEntity.getComponentTypeCode().equalsIgnoreCase(componentTypeCode))
                    .findFirst();

            if(component.isPresent() && !component.get().getAssessmentQuestionEntities().isEmpty()) {
                var responseNotSelected = getChoicePathNotSelected(student, component.get());
                if(includeChoiceCalc) {
                    return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                            .flatMap(Collection::stream)
                            .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getCognitiveLevelCode())
                                    && StringUtils.isNotBlank(responseNotSelected)
                                    && !assessmentQuestionEntity.getTaskCode().equalsIgnoreCase(responseNotSelected)
                                    && assessmentQuestionEntity.getCognitiveLevelCode().equalsIgnoreCase(taskCode))
                            .toList();
                }
                return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                        .flatMap(Collection::stream)
                        .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getCognitiveLevelCode()) && assessmentQuestionEntity.getCognitiveLevelCode().equalsIgnoreCase(taskCode))
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private String getChoicePathNotSelected(AssessmentStudentEntity student, AssessmentComponentEntity component) {
        var studentComponent = student.getAssessmentStudentComponentEntities().stream()
                .filter(assessmentComponentEntity -> Objects.equals(assessmentComponentEntity.getAssessmentComponentID(), component.getAssessmentComponentID()))
                .findFirst();
        var selectedChoice = studentComponent.map(AssessmentStudentComponentEntity::getChoicePath).orElse(null);
        var responseNotSelected = Arrays.stream(allowedSelectedResponse).filter(response -> StringUtils.isNotBlank(selectedChoice) && selectedChoice.equalsIgnoreCase(response)).findFirst();
        return responseNotSelected.isPresent() ? selectedChoice : null;
    }

    private List<AssessmentQuestionEntity> getQuestionsWithAssessmentSectionForSelectedForm(AssessmentFormEntity selectedAssessmentForm, String code, AssessmentStudentEntity student, String componentTypeCode, boolean includeChoiceCalc) {
        if(!selectedAssessmentForm.getAssessmentComponentEntities().isEmpty()) {
            var component = selectedAssessmentForm.getAssessmentComponentEntities().stream()
                    .filter(assessmentComponentEntity ->
                            assessmentComponentEntity.getComponentTypeCode().equalsIgnoreCase(componentTypeCode))
                    .findFirst();

            if(component.isPresent() && !component.get().getAssessmentQuestionEntities().isEmpty()) {
                var responseNotSelected = getChoicePathNotSelected(student, component.get());
                if(includeChoiceCalc) {
                    return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                            .flatMap(Collection::stream)
                            .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getAssessmentSection())
                                    && StringUtils.isNotBlank(responseNotSelected)
                                    && !assessmentQuestionEntity.getTaskCode().equalsIgnoreCase(responseNotSelected)
                                    && assessmentQuestionEntity.getAssessmentSection().startsWith(code))
                            .toList();
                }
                return component.stream().map(AssessmentComponentEntity::getAssessmentQuestionEntities)
                        .flatMap(Collection::stream)
                        .filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getAssessmentSection()) && assessmentQuestionEntity.getAssessmentSection().startsWith(code))
                        .toList();
            }
        }
        return Collections.emptyList();
    }
}
