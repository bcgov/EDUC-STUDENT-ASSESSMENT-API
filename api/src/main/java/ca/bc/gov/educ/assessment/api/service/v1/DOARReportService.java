package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.reports.*;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidParameterException;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentDOARCalculationRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.DOARCalculate;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class DOARReportService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository;
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

    public DOARReportService(AssessmentSessionRepository assessmentSessionRepository, AssessmentStudentRepository assessmentStudentRepository, AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository, DOARCalculateService doarCalculateService, RestUtils restUtils) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.assessmentStudentDOARCalculationRepository = assessmentStudentDOARCalculationRepository;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAndPopulateDOARSummaryCalculations(TransferOnApprovalSagaData sagaData) {
        var assessmentStudentEntity = assessmentStudentRepository
                .findByAssessmentEntity_AssessmentIDAndStudentID(UUID.fromString(sagaData.getAssessmentID()), UUID.fromString(sagaData.getStudentID()))
                .orElseThrow(() -> new EntityNotFoundException(AssessmentStudentEntity.class, "StudentID", sagaData.getStudentID()));

        var selectedAssessmentForm = assessmentStudentEntity.getAssessmentEntity().getAssessmentForms().stream()
                .filter(assessmentFormEntity -> assessmentStudentEntity.getAssessmentFormID() != null
                        && Objects.equals(assessmentFormEntity.getAssessmentFormID(), assessmentStudentEntity.getAssessmentFormID()))
                .findFirst();
        if(selectedAssessmentForm.isPresent()) {
            var assessmentStudentDOARCalculationEntity = prepareLTEDOARSummaryEntity(assessmentStudentEntity, selectedAssessmentForm.get(), assessmentStudentEntity.getAssessmentEntity().getAssessmentTypeCode());
            assessmentStudentDOARCalculationEntity.setCreateUser(ApplicationProperties.STUDENT_ASSESSMENT_API);
            assessmentStudentDOARCalculationEntity.setUpdateUser(ApplicationProperties.STUDENT_ASSESSMENT_API);
            assessmentStudentDOARCalculationEntity.setCreateDate(LocalDateTime.now());
            assessmentStudentDOARCalculationEntity.setUpdateDate(LocalDateTime.now());
            assessmentStudentDOARCalculationRepository.save(assessmentStudentDOARCalculationEntity);
        }
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

    private AssessmentStudentDOARCalculationEntity prepareLTEDOARSummaryEntity(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm, String assessmentTypeCode) {
        return switch (assessmentTypeCode) {
            case "NME10", "NMF10" -> prepareNMEDOAREntity(student, selectedAssessmentForm);
            case "LTE10", "LTE12" -> prepareLTEDOAREntity(student, selectedAssessmentForm);
            case LTP12 -> prepareLTP12DOAREntity(student, selectedAssessmentForm);
            case LTP10 -> prepareLTP10DOAREntity(student, selectedAssessmentForm);
            case LTF12 -> prepareLTF12DOAREntity(student, selectedAssessmentForm);
            default -> AssessmentStudentDOARCalculationEntity.builder().build();
        };
    }

    private AssessmentStudentDOARCalculationEntity prepareLTEDOAREntity(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm) {
        var taskComprehend = getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, "LTE", true);
        var taskCommunicate = getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, "LTE", true);

        var comprehendPartA = getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, "LTE", true);
        var comprehendPartB = getStudentTotals(MUL_CHOICE, "B", selectedAssessmentForm, student, "LTE", true);

        var commGO = getStudentTotals(OPEN_ENDED, "GO", selectedAssessmentForm, student, "LTE", true);
        var commWRA = getStudentTotals(OPEN_ENDED, "WRA", selectedAssessmentForm, student, "LTE", true);
        var commWRB = getStudentTotals(OPEN_ENDED, "WRB", selectedAssessmentForm, student, "LTE", true);

        var dok1 = getStudentTotals("BOTH", "7", selectedAssessmentForm, student, "LTE", true);
        var dok2 = getStudentTotals("BOTH", "8", selectedAssessmentForm, student, "LTE", true);
        var dok3 = getStudentTotals("BOTH", "9", selectedAssessmentForm, student, "LTE", true);

        return AssessmentStudentDOARCalculationEntity.builder()
                .assessmentStudentID(student.getAssessmentStudentID())
                .assessmentID(student.getAssessmentEntity().getAssessmentID())
                .taskComprehend(new BigDecimal(taskComprehend))
                .taskCommunicate(new BigDecimal(taskCommunicate))
                .comprehendPartA(new BigDecimal(comprehendPartA))
                .comprehendPartB(new BigDecimal(comprehendPartB))
                .communicateGraphicOrg(new BigDecimal(commGO))
                .communicateUnderstanding(new BigDecimal(commWRA))
                .communicatePersonalConn(new BigDecimal(commWRB))
                .dok1(new BigDecimal(dok1))
                .dok2(new BigDecimal(dok2))
                .dok3(new BigDecimal(dok3))
                .build();
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

    private AssessmentStudentDOARCalculationEntity prepareLTP12DOAREntity(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm) {
        var taskComprehend = getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, LTP12, true);
        var taskCommunicate = getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, LTP12, true);
        var taskOral = getStudentTotals(OPEN_ENDED, "O", selectedAssessmentForm, student, LTP12, true);

        var comprehendPartA = getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, LTP12, true);
        var comprehendPartB = getStudentTotals(MUL_CHOICE, "B", selectedAssessmentForm, student, LTP12, true);

        var commGO = getStudentTotals(OPEN_ENDED, "GO", selectedAssessmentForm, student, LTP12, true);
        var commWRA = getStudentTotals(OPEN_ENDED, "WRA", selectedAssessmentForm, student, LTP12, true);
        var commWRB = getStudentTotals(OPEN_ENDED, "WRB", selectedAssessmentForm, student, LTP12, true);

        var communicateOralPart1 = getStudentTotals(OPEN_ENDED, "O1", selectedAssessmentForm, student, LTP12, true);
        var communicateOralPart2 = getStudentTotals(OPEN_ENDED, "O2", selectedAssessmentForm, student, LTP12, true);
        var communicateOralPart3 = getStudentTotals(OPEN_ENDED, "O3", selectedAssessmentForm, student, LTP12, true);

        var dok1 = getStudentTotals("BOTH", "7", selectedAssessmentForm, student, LTP12, true);
        var dok2 = getStudentTotals("BOTH", "8", selectedAssessmentForm, student, LTP12, true);
        var dok3 = getStudentTotals("BOTH", "9", selectedAssessmentForm, student, LTP12, true);

        return AssessmentStudentDOARCalculationEntity.builder()
                .assessmentStudentID(student.getAssessmentStudentID())
                .assessmentID(student.getAssessmentEntity().getAssessmentID())
                .taskComprehend(new BigDecimal(taskComprehend))
                .taskCommunicate(new BigDecimal(taskCommunicate))
                .taskOral(new BigDecimal(taskOral))
                .comprehendPartA(new BigDecimal(comprehendPartA))
                .comprehendPartB(new BigDecimal(comprehendPartB))
                .communicateGraphicOrg(new BigDecimal(commGO))
                .communicateUnderstanding(new BigDecimal(commWRA))
                .communicatePersonalConn(new BigDecimal(commWRB))
                .communicateOralPart1(new BigDecimal(communicateOralPart1))
                .communicateOralPart2(new BigDecimal(communicateOralPart2))
                .communicateOralPart3(new BigDecimal(communicateOralPart3))
                .dok1(new BigDecimal(dok1))
                .dok2(new BigDecimal(dok2))
                .dok3(new BigDecimal(dok3))
                .build();
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

    private AssessmentStudentDOARCalculationEntity prepareLTP10DOAREntity(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm) {
        var taskComprehend = getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, LTP10, true);
        var taskCommunicate = getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, LTP10, true);
        var taskOral = getStudentTotals(OPEN_ENDED, "O", selectedAssessmentForm, student, LTP10 , true);

        var comprehendPartA = getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, LTP10, true);
        var comprehendPartB = getStudentTotals(MUL_CHOICE, "B", selectedAssessmentForm, student, LTP10, true);

        var commGO = getStudentTotals(OPEN_ENDED, "GO", selectedAssessmentForm, student, LTP10, true);
        var commWRA = getStudentTotals(OPEN_ENDED, "WRA", selectedAssessmentForm, student, LTP10, true);
        var commWRB = getStudentTotals(OPEN_ENDED, "WRB", selectedAssessmentForm, student, LTP10, true);
        var comprehendPartAShort = getStudentTotals(OPEN_ENDED, "WRS", selectedAssessmentForm, student, LTP10, true);

        var communicateOralPart1 = getStudentTotals(OPEN_ENDED, "O1", selectedAssessmentForm, student, LTP10, true);
        var communicateOralPart2 = getStudentTotals(OPEN_ENDED, "O2", selectedAssessmentForm, student, LTP10, true);
        var communicateOralPart3 = getStudentTotals(OPEN_ENDED, "O3", selectedAssessmentForm, student, LTP10, true);

        var dok1 = getStudentTotals("BOTH", "7", selectedAssessmentForm, student, LTP10, true);
        var dok2 = getStudentTotals("BOTH", "8", selectedAssessmentForm, student, LTP10, true);
        var dok3 = getStudentTotals("BOTH", "9", selectedAssessmentForm, student, LTP10, true);

        return AssessmentStudentDOARCalculationEntity.builder()
                .assessmentStudentID(student.getAssessmentStudentID())
                .assessmentID(student.getAssessmentEntity().getAssessmentID())
                .taskComprehend(new BigDecimal(taskComprehend))
                .taskCommunicate(new BigDecimal(taskCommunicate))
                .taskOral(new BigDecimal(taskOral))
                .comprehendPartA(new BigDecimal(comprehendPartA))
                .comprehendPartB(new BigDecimal(comprehendPartB))
                .communicateGraphicOrg(new BigDecimal(commGO))
                .communicateUnderstanding(new BigDecimal(commWRA))
                .communicatePersonalConn(new BigDecimal(commWRB))
                .comprehendPartAShort(new BigDecimal(comprehendPartAShort))
                .communicateOralPart1(new BigDecimal(communicateOralPart1))
                .communicateOralPart2(new BigDecimal(communicateOralPart2))
                .communicateOralPart3(new BigDecimal(communicateOralPart3))
                .dok1(new BigDecimal(dok1))
                .dok2(new BigDecimal(dok2))
                .dok3(new BigDecimal(dok3))
                .build();
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

    private AssessmentStudentDOARCalculationEntity prepareLTF12DOAREntity(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm) {
        var taskComprehend = getStudentTotals(MUL_CHOICE, "C", selectedAssessmentForm, student, LTF12, true);
        var taskCommunicate = getStudentTotals(OPEN_ENDED, "W", selectedAssessmentForm, student, LTF12, true);
        var taskOral = getStudentTotals(OPEN_ENDED, "O", selectedAssessmentForm, student, LTF12 , true);

        var comprehendPartATask = getStudentTotals(MUL_CHOICE, "A", selectedAssessmentForm, student, LTF12, false);
        var comprehendPartBInfo = getStudentTotals(MUL_CHOICE, "I", selectedAssessmentForm, student, LTF12, false);
        var comprehendPartBExp =  getStudentTotals(MUL_CHOICE, "E", selectedAssessmentForm, student, LTF12, false);

        var comprehendPartAShort = getStudentTotals(OPEN_ENDED, "WRS", selectedAssessmentForm, student, LTF12, true);
        var dissertationBackground = getStudentTotals(OPEN_ENDED, "WRD", selectedAssessmentForm, student, LTF12, true);
        var dissertationForm = getStudentTotals(OPEN_ENDED, "WRF", selectedAssessmentForm, student, LTF12, true);

        var communicateOralPart1Background = getStudentTotals(OPEN_ENDED, "O1D", selectedAssessmentForm, student, LTF12, true);
        var comprehendOralPart1Form = getStudentTotals(OPEN_ENDED, "O1F", selectedAssessmentForm, student, LTF12, true);
        var comprehendOralPart1Expression = getStudentTotals(OPEN_ENDED, "O1E", selectedAssessmentForm, student, LTF12, true);

        var communicateOralPart2Background = getStudentTotals(OPEN_ENDED, "O2D", selectedAssessmentForm, student, LTF12, true);
        var comprehendOralPart2Form = getStudentTotals(OPEN_ENDED, "O2F", selectedAssessmentForm, student, LTF12, true);
        var comprehendOralPart2Expression = getStudentTotals(OPEN_ENDED, "O2E", selectedAssessmentForm, student, LTF12, true);

        var dok1 = getStudentTotals("BOTH", "7", selectedAssessmentForm, student, LTF12, true);
        var dok2 = getStudentTotals("BOTH", "8", selectedAssessmentForm, student, LTF12, true);
        var dok3 = getStudentTotals("BOTH", "9", selectedAssessmentForm, student, LTF12, true);

        return AssessmentStudentDOARCalculationEntity.builder()
                .assessmentStudentID(student.getAssessmentStudentID())
                .assessmentID(student.getAssessmentEntity().getAssessmentID())
                .taskComprehend(new BigDecimal(taskComprehend))
                .taskCommunicate(new BigDecimal(taskCommunicate))
                .taskOral(new BigDecimal(taskOral))
                .comprehendPartATask(new BigDecimal(comprehendPartATask))
                .comprehendPartBInfo(new BigDecimal(comprehendPartBInfo))
                .comprehendPartBExp(new BigDecimal(comprehendPartBExp))
                .comprehendPartAShort(new BigDecimal(comprehendPartAShort))
                .dissertationBackground(new BigDecimal(dissertationBackground))
                .dissertationForm(new BigDecimal(dissertationForm))
                .communicateOralPart1Background(new BigDecimal(communicateOralPart1Background))
                .comprehendOralPart1Form(new BigDecimal(comprehendOralPart1Form))
                .comprehendOralPart1Expression(new BigDecimal(comprehendOralPart1Expression))
                .communicateOralPart2Background(new BigDecimal(communicateOralPart2Background))
                .comprehendOralPart2Form(new BigDecimal(comprehendOralPart2Form))
                .comprehendOralPart2Expression(new BigDecimal(comprehendOralPart2Expression))
                .dok1(new BigDecimal(dok1))
                .dok2(new BigDecimal(dok2))
                .dok3(new BigDecimal(dok3))
                .build();
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

    private AssessmentStudentDOARCalculationEntity prepareNMEDOAREntity(AssessmentStudentEntity student, AssessmentFormEntity selectedAssessmentForm) {
        var taskPlan = getStudentTotals(BOTH, "P", selectedAssessmentForm, student, "NME", false);
        var taskEstimate = getStudentTotals(BOTH, "R", selectedAssessmentForm, student, "NME", false);
        var taskFair = getStudentTotals(BOTH, "F", selectedAssessmentForm, student, "NME", false);
        var taskModel = getStudentTotals(BOTH, "M", selectedAssessmentForm, student, "NME", false);

        var numeracyInterpret = getStudentTotals(MUL_CHOICE, "I", selectedAssessmentForm, student, "NME", false);
        var numeracyApply = getStudentTotals(MUL_CHOICE, "P", selectedAssessmentForm, student, "NME", false);
        var numeracySolve = getStudentTotals(MUL_CHOICE, "S", selectedAssessmentForm, student, "NME", false);
        var numeracyAnalyze = getStudentTotals(MUL_CHOICE, "N", selectedAssessmentForm, student, "NME", false);

        var dok1 = getStudentTotals("BOTH", "7", selectedAssessmentForm, student, "NME", false);
        var dok2 = getStudentTotals("BOTH", "8", selectedAssessmentForm, student, "NME", false);
        var dok3 = getStudentTotals("BOTH", "9", selectedAssessmentForm, student, "NME", false);

        return AssessmentStudentDOARCalculationEntity.builder()
                .assessmentStudentID(student.getAssessmentStudentID())
                .assessmentID(student.getAssessmentEntity().getAssessmentID())
                .taskPlan(new BigDecimal(taskPlan))
                .taskEstimate(new BigDecimal(taskEstimate))
                .taskFair(new BigDecimal(taskFair))
                .taskModel(new BigDecimal(taskModel))
                .numeracyInterpret(new BigDecimal(numeracyInterpret))
                .numeracyApply(new BigDecimal(numeracyApply))
                .numeracySolve(new BigDecimal(numeracySolve))
                .numeracyAnalyze(new BigDecimal(numeracyAnalyze))
                .dok1(new BigDecimal(dok1))
                .dok2(new BigDecimal(dok2))
                .dok3(new BigDecimal(dok3))
                .build();
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
