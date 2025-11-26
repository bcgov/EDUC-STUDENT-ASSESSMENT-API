package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.doar.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.assessment.api.constants.v1.SchoolCategoryCodes.*;

/**
 * Service class for generating DOAR Summary
 */
@Service
@Slf4j
public class DOARProvincialReportService extends BaseReportGenerationService {

  private final AssessmentSessionRepository assessmentSessionRepository;
  private final AssessmentStudentLightRepository assessmentStudentLightRepository;
  private final StagedAssessmentStudentLightRepository stagedAssessmentStudentLightRepository;
  private final AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository;
  private final StagedAssessmentStudentDOARCalculationRepository stagedAssessmentStudentDOARCalculationRepository;
  private final RestUtils restUtils;
  private JasperReport doarSummaryReport;
  private static final String PUBLIC= "Public";
  private static final String NME10= "NME10";
  private static final String NMF10= "NMF10";
  private static final String LTE10= "LTE10";
  private static final String LTE12= "LTE12";
  private static final String LTP10= "LTP10";
  private static final String LTP12= "LTP12";
  private static final String LTF12= "LTF12";

  public DOARProvincialReportService(AssessmentSessionRepository assessmentSessionRepository, AssessmentStudentLightRepository assessmentStudentLightRepository, StagedAssessmentStudentLightRepository stagedAssessmentStudentLightRepository, AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository, StagedAssessmentStudentDOARCalculationRepository stagedAssessmentStudentDOARCalculationRepository, RestUtils restUtils) {
    super(restUtils);
    this.assessmentSessionRepository = assessmentSessionRepository;
    this.assessmentStudentLightRepository = assessmentStudentLightRepository;
    this.stagedAssessmentStudentLightRepository = stagedAssessmentStudentLightRepository;
    this.assessmentStudentDOARCalculationRepository = assessmentStudentDOARCalculationRepository;
    this.stagedAssessmentStudentDOARCalculationRepository = stagedAssessmentStudentDOARCalculationRepository;
    this.restUtils = restUtils;
  }

  @PostConstruct
  public void init() {
    ApplicationProperties.bgTask.execute(this::initialize);
  }

  private void initialize() {
    this.compileJasperReports();
  }

  private void compileJasperReports(){
    try {
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/doarProvincialSummary.jrxml");
      doarSummaryReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentAssessmentAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateDOARProvincialReport(UUID assessmentSessionID){
    try {
      DOARSummaryNode doarSummaryNode = new DOARSummaryNode();
      doarSummaryNode.setReports(new ArrayList<>());
      var session = assessmentSessionRepository.findById(assessmentSessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", assessmentSessionID.toString()));

      if(session.getCompletionDate() != null) {
        var students = stagedAssessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeAndProficiencyScoreIsNotNullOrProvincialSpecialCaseCode(assessmentSessionID, StudentStatusCodes.ACTIVE.getCode(), "X");
        setStudentLevelsForStaging(students, doarSummaryNode, session);
      } else {
        var students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeAndProficiencyScoreIsNotNullOrProvincialSpecialCaseCode(assessmentSessionID, StudentStatusCodes.ACTIVE.getCode(), "X");
        setStudentLevels(students, doarSummaryNode, session);
      }

      String payload = objectWriter.writeValueAsString(doarSummaryNode);
      return generateJasperReport(payload, doarSummaryReport, AssessmentReportTypeCode.DOAR_SUMMARY.getCode());
    }
    catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
    }
  }

  private void setStudentLevelsForStaging(List<StagedAssessmentStudentLightEntity> students, DOARSummaryNode doarSummaryNode, AssessmentSessionEntity session) {
    HashMap<String, List<StagedAssessmentStudentLightEntity>> studentsByAssessment = new HashMap<>();

    students.forEach(student -> {
      if(studentsByAssessment.containsKey(student.getAssessmentEntity().getAssessmentTypeCode())) {
        studentsByAssessment.get(student.getAssessmentEntity().getAssessmentTypeCode()).add(student);
      }else{
        List<StagedAssessmentStudentLightEntity> studentList = new ArrayList<>();
        studentList.add(student);
        studentsByAssessment.put(student.getAssessmentEntity().getAssessmentTypeCode(), studentList);
      }
    });

    studentsByAssessment.forEach((assessmentType, studentList) -> {
      if(!studentList.isEmpty()) {
        DOARSummaryPage doarSummaryPage = new DOARSummaryPage();
        setReportTombstoneValues(session, doarSummaryPage, assessmentType);
        doarSummaryPage.setProficiencySection(new ArrayList<>());
        doarSummaryPage.setTaskScore(new ArrayList<>());
        doarSummaryPage.setComprehendScore(new ArrayList<>());
        doarSummaryPage.setCommunicateScore(new ArrayList<>());
        doarSummaryPage.setCommunicateOralScore(new ArrayList<>());
        doarSummaryPage.setNumeracyScore(new ArrayList<>());
        doarSummaryPage.setCognitiveLevelScore(new ArrayList<>());

        setProficiencyLevelsForStaging(studentList, doarSummaryPage);
        setAssessmentRawScoresForStaging(studentList, doarSummaryPage, assessmentType);

        doarSummaryNode.getReports().add(doarSummaryPage);
      }
    });
  }

  private void setStudentLevels(List<AssessmentStudentLightEntity> students, DOARSummaryNode doarSummaryNode, AssessmentSessionEntity session) {
    HashMap<String, List<AssessmentStudentLightEntity>> studentsByAssessment = new HashMap<>();

    students.forEach(student -> {
      if(studentsByAssessment.containsKey(student.getAssessmentEntity().getAssessmentTypeCode())) {
        studentsByAssessment.get(student.getAssessmentEntity().getAssessmentTypeCode()).add(student);
      }else{
        List<AssessmentStudentLightEntity> studentList = new ArrayList<>();
        studentList.add(student);
        studentsByAssessment.put(student.getAssessmentEntity().getAssessmentTypeCode(), studentList);
      }
    });

    studentsByAssessment.forEach((assessmentType, studentList) -> {
      if(!studentList.isEmpty()) {
        DOARSummaryPage doarSummaryPage = new DOARSummaryPage();
        setReportTombstoneValues(session, doarSummaryPage, assessmentType);
        doarSummaryPage.setProficiencySection(new ArrayList<>());
        doarSummaryPage.setTaskScore(new ArrayList<>());
        doarSummaryPage.setComprehendScore(new ArrayList<>());
        doarSummaryPage.setCommunicateScore(new ArrayList<>());
        doarSummaryPage.setCommunicateOralScore(new ArrayList<>());
        doarSummaryPage.setNumeracyScore(new ArrayList<>());
        doarSummaryPage.setCognitiveLevelScore(new ArrayList<>());

        setProficiencyLevels(studentList, doarSummaryPage);
        setAssessmentRawScores(studentList, doarSummaryPage, assessmentType);

        doarSummaryNode.getReports().add(doarSummaryPage);
      }
    });
  }

  protected void setReportTombstoneValues(AssessmentSessionEntity assessmentSession, DOARSummaryPage reportNode, String assessmentType){
    reportNode.setReportGeneratedDate("Report Generated: " + LocalDate.now().format(formatter));
    reportNode.setSessionDetail(assessmentSession.getCourseYear() + "/" + assessmentSession.getCourseMonth() + " Session");
    reportNode.setAssessmentType(assessmentType);
    reportNode.setReportId(UUID.randomUUID().toString());
    reportNode.setReportTitle(getReportTitle(assessmentType));
  }

  private void setAssessmentRawScores(List<AssessmentStudentLightEntity> students, DOARSummaryPage doarSummaryPage, String assessmentType) {
    UUID assessmentID = students.getFirst().getAssessmentEntity().getAssessmentID();
    Map<String, Set<UUID>> studentIdsByLevel = categorizeStudentsByLevel(students);

    List<UUID> allStudentIds = students.stream()
            .map(AssessmentStudentLightEntity::getAssessmentStudentID)
            .toList();
    var allDOARCalc = assessmentStudentDOARCalculationRepository
            .findAllByAssessmentIDAndAssessmentStudentIDIn(assessmentID, allStudentIds);

    Map<UUID, AssessmentStudentDOARCalculationEntity> doarCalcMap = allDOARCalc.stream()
            .collect(Collectors.toMap(AssessmentStudentDOARCalculationEntity::getAssessmentStudentID, Function.identity()));

    List<AssessmentStudentDOARCalculationEntity> provinceLevelDOARCalc =
            filterDOARCalcByStudentIds(doarCalcMap, studentIdsByLevel.get("Province"));

      populateRawScoresForPublicSchools(provinceLevelDOARCalc, doarSummaryPage, assessmentType);
  }

  private void setAssessmentRawScoresForStaging(List<StagedAssessmentStudentLightEntity> students, DOARSummaryPage doarSummaryPage, String assessmentType) {
    UUID assessmentID = students.getFirst().getAssessmentEntity().getAssessmentID();
    Map<String, Set<UUID>> studentIdsByLevel = categorizeStudentsByLevelForStaging(students);

    List<UUID> allStudentIds = students.stream()
            .map(StagedAssessmentStudentLightEntity::getAssessmentStudentID)
            .toList();
    var allDOARCalc = stagedAssessmentStudentDOARCalculationRepository
            .findAllByAssessmentIDAndAssessmentStudentIDIn(assessmentID, allStudentIds);

    Map<UUID, StagedAssessmentStudentDOARCalculationEntity> doarCalcMap = allDOARCalc.stream()
            .collect(Collectors.toMap(StagedAssessmentStudentDOARCalculationEntity::getAssessmentStudentID, Function.identity()));

    List<StagedAssessmentStudentDOARCalculationEntity> provinceLevelDOARCalc = studentIdsByLevel.get("Province").stream()
            .map(doarCalcMap::get)
            .filter(Objects::nonNull)
            .toList();

    populateRawScoresForPublicSchoolsForStaging(provinceLevelDOARCalc, doarSummaryPage, assessmentType);
  }

  private List<AssessmentStudentDOARCalculationEntity> filterDOARCalcByStudentIds(Map<UUID, AssessmentStudentDOARCalculationEntity> doarCalcMap, Set<UUID> studentIds) {
    return studentIds.stream()
            .map(doarCalcMap::get)
            .filter(Objects::nonNull)
            .toList();
  }

  private Map<String, Set<UUID>> categorizeStudentsByLevel(
          List<AssessmentStudentLightEntity> students) {

    Map<String, Set<UUID>> result = new HashMap<>();
    Set<UUID> provinceSchoolIds = getSchoolsInProvince().stream()
            .map(s -> UUID.fromString(s.getSchoolId()))
            .collect(Collectors.toSet());

    Set<UUID> provinceLevel = new HashSet<>();

    for (AssessmentStudentLightEntity student : students) {
      UUID studentSchoolId = student.getSchoolAtWriteSchoolID();
      UUID studentId = student.getAssessmentStudentID();

      if (provinceSchoolIds.contains(studentSchoolId)) {
        provinceLevel.add(studentId);
      }
    }
    result.put("Province", provinceLevel);

    return result;
  }

  private Map<String, Set<UUID>> categorizeStudentsByLevelForStaging(
          List<StagedAssessmentStudentLightEntity> students) {

    Map<String, Set<UUID>> result = new HashMap<>();
    Set<UUID> provinceSchoolIds = getSchoolsInProvince().stream()
            .map(s -> UUID.fromString(s.getSchoolId()))
            .collect(Collectors.toSet());

    Set<UUID> provinceLevel = new HashSet<>();

    for (StagedAssessmentStudentLightEntity student : students) {
      UUID studentSchoolId = student.getSchoolAtWriteSchoolID();
      UUID studentId = student.getAssessmentStudentID();

      if (provinceSchoolIds.contains(studentSchoolId)) {
        provinceLevel.add(studentId);
      }
    }
    result.put("Province", provinceLevel);

    return result;
  }

  private void populateRawScoresForPublicSchools(List<AssessmentStudentDOARCalculationEntity> provinceLevel, DOARSummaryPage doarSummaryPage, String assessmentType) {
    List<TaskScore> taskScores = List.of(createTaskSectionByLevel(assessmentType, provinceLevel));
    doarSummaryPage.getTaskScore().addAll(taskScores);

    List<CognitiveLevelScore> cognScore = List.of(createCognitiveSectionByLevel(provinceLevel));
    doarSummaryPage.getCognitiveLevelScore().addAll(cognScore);

    switch (assessmentType) {
      case NME10, NMF10 ->  {
        List<NumeracyScore> numScores = List.of(createNumeracySectionByLevel(provinceLevel));
        doarSummaryPage.getNumeracyScore().addAll(numScores);
      }
      case LTE10, LTE12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);
      }
      case LTP10, LTP12, LTF12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);

        List<CommunicateOralScore> communicateOralScores = List.of(createCommunicateOralSectionByLevel(assessmentType, provinceLevel));
        doarSummaryPage.getCommunicateOralScore().addAll(communicateOralScores);
      }
      default -> log.info("Invalid assessment type");
    }
  }

  private void populateRawScoresForPublicSchoolsForStaging(List<StagedAssessmentStudentDOARCalculationEntity> provinceLevel, DOARSummaryPage doarSummaryPage, String assessmentType) {
    List<TaskScore> taskScores = List.of(createTaskSectionByLevelForStaging(assessmentType, provinceLevel));
    doarSummaryPage.getTaskScore().addAll(taskScores);

    List<CognitiveLevelScore> cognScore = List.of(createCognitiveSectionByLevelForStaging(provinceLevel));
    doarSummaryPage.getCognitiveLevelScore().addAll(cognScore);

    switch (assessmentType) {
      case NME10, NMF10 ->  {
        List<NumeracyScore> numScores = List.of(createNumeracySectionByLevelForStaging(provinceLevel));
        doarSummaryPage.getNumeracyScore().addAll(numScores);
      }
      case LTE10, LTE12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevelForStaging(assessmentType, provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevelForStaging(assessmentType, provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);
      }
      case LTP10, LTP12, LTF12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevelForStaging(assessmentType, provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevelForStaging(assessmentType, provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);

        List<CommunicateOralScore> communicateOralScores = List.of(createCommunicateOralSectionByLevelForStaging(assessmentType, provinceLevel));
        doarSummaryPage.getCommunicateOralScore().addAll(communicateOralScores);
      }
      default -> log.info("Invalid assessment type");
    }
  }

  private TaskScore createTaskSectionByLevel(String assessmentType, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new TaskScore();
    }
    TaskScore score = new TaskScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case NME10, NMF10 ->  {
        BigDecimal taskPlanSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskPlan).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskEstimateSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskEstimate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskFairSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskFair).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskModelSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskModel).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskPlan(String.valueOf(taskPlanSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskEstimate(String.valueOf(taskEstimateSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskFair(String.valueOf(taskFairSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskModel(String.valueOf(taskModelSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));

        yield score;
      }
      case LTE10, LTE12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskComprehend(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskCommunicate(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));

        yield score;
      }
      case LTP10, LTP12, LTF12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskCommOral = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskOral).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskComprehend(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskCommunicate(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskOral(String.valueOf(taskCommOral.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));

        yield score;
      }
     default -> score;
    };
  }

  private TaskScore createTaskSectionByLevelForStaging(String assessmentType, List<StagedAssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new TaskScore();
    }
    TaskScore score = new TaskScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case NME10, NMF10 ->  {
        BigDecimal taskPlanSum = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskPlan).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskEstimateSum = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskEstimate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskFairSum = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskFair).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskModelSum = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskModel).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskPlan(String.valueOf(taskPlanSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskEstimate(String.valueOf(taskEstimateSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskFair(String.valueOf(taskFairSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskModel(String.valueOf(taskModelSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));

        yield score;
      }
      case LTE10, LTE12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskComprehend(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskCommunicate(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));

        yield score;
      }
      case LTP10, LTP12, LTF12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskCommOral = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getTaskOral).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskComprehend(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskCommunicate(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setTaskOral(String.valueOf(taskCommOral.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));

        yield score;
      }
      default -> score;
    };
  }

  private ComprehendScore createComprehendSectionByLevel(String assessmentType, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new ComprehendScore();
    }
    ComprehendScore score = new ComprehendScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case LTE10, LTE12, LTP10, LTP12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartA).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartB).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartA(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartB(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartATask).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal infoCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartBInfo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartBExp).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartATask(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartBInfo(String.valueOf(infoCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartBExp(String.valueOf(expCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      default -> score;
    };
  }

  private ComprehendScore createComprehendSectionByLevelForStaging(String assessmentType, List<StagedAssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new ComprehendScore();
    }
    ComprehendScore score = new ComprehendScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case LTE10, LTE12, LTP10, LTP12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getComprehendPartA).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getComprehendPartB).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartA(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartB(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getComprehendPartATask).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal infoCompre = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getComprehendPartBInfo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expCompre = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getComprehendPartBExp).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartATask(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartBInfo(String.valueOf(infoCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartBExp(String.valueOf(expCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateScore createCommunicateSectionByLevel(String assessmentType, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateScore();
    }
    CommunicateScore score = new CommunicateScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case LTE10, LTE12, LTP12 -> {
        BigDecimal commGO = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRB = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateGraphicOrg(String.valueOf(commGO.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateUnderstanding(String.valueOf(commWRA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicatePersonalConn(String.valueOf(commWRB.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTP10 -> {
        BigDecimal comprePartA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commGO = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRB = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateGraphicOrg(String.valueOf(commGO.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateUnderstanding(String.valueOf(commWRA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicatePersonalConn(String.valueOf(commWRB.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartAShort(String.valueOf(comprePartA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal background = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDissertationBackground).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal form = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDissertationForm).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comprePartA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartAShort(String.valueOf(comprePartA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setDissertationBackground(String.valueOf(background.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setDissertationForm(String.valueOf(form.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateScore createCommunicateSectionByLevelForStaging(String assessmentType, List<StagedAssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateScore();
    }
    CommunicateScore score = new CommunicateScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case LTE10, LTE12, LTP12 -> {
        BigDecimal commGO = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRA = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRB = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateGraphicOrg(String.valueOf(commGO.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateUnderstanding(String.valueOf(commWRA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicatePersonalConn(String.valueOf(commWRB.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTP10 -> {
        BigDecimal comprePartA = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commGO = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRA = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRB = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateGraphicOrg(String.valueOf(commGO.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateUnderstanding(String.valueOf(commWRA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicatePersonalConn(String.valueOf(commWRB.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setComprehendPartAShort(String.valueOf(comprePartA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal background = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getDissertationBackground).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal form = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getDissertationForm).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comprePartA = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartAShort(String.valueOf(comprePartA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setDissertationBackground(String.valueOf(background.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setDissertationForm(String.valueOf(form.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateOralScore createCommunicateOralSectionByLevel(String assessmentType, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateOralScore();
    }
    CommunicateOralScore score = new CommunicateOralScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case LTP12, LTP10 -> {
        BigDecimal part1 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart3).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateOralPart1(String.valueOf(part1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2(String.valueOf(part2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart3(String.valueOf(part3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal part1 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Form).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part4 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part5 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part6 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Form).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateOralPart1Background(String.valueOf(part1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart1Form(String.valueOf(part2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart1Expression(String.valueOf(part3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2Expression(String.valueOf(part4.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2Background(String.valueOf(part5.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2Form(String.valueOf(part6.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateOralScore createCommunicateOralSectionByLevelForStaging(String assessmentType, List<StagedAssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateOralScore();
    }
    CommunicateOralScore score = new CommunicateOralScore();
    score.setLevel("Province");

    return switch (assessmentType) {
      case LTP12, LTP10 -> {
        BigDecimal part1 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart1).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart2).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart3).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateOralPart1(String.valueOf(part1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2(String.valueOf(part2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart3(String.valueOf(part3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal part1 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Form).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part4 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part5 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part6 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Form).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateOralPart1Background(String.valueOf(part1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart1Form(String.valueOf(part2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart1Expression(String.valueOf(part3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2Expression(String.valueOf(part4.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2Background(String.valueOf(part5.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        score.setCommunicateOralPart2Form(String.valueOf(part6.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
        yield score;
      }
      default -> score;
    };
  }

  private CognitiveLevelScore createCognitiveSectionByLevel(List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CognitiveLevelScore();
    }
    CognitiveLevelScore score = new CognitiveLevelScore();
    score.setLevel("Province");
    BigDecimal dok1 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok1).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok2 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok2).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok3 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok3).reduce(BigDecimal.ZERO, BigDecimal::add);

    score.setDok1(String.valueOf(dok1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setDok2(String.valueOf(dok2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setDok3(String.valueOf(dok3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    return score;
  }

  private CognitiveLevelScore createCognitiveSectionByLevelForStaging(List<StagedAssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CognitiveLevelScore();
    }
    CognitiveLevelScore score = new CognitiveLevelScore();
    score.setLevel("Province");
    BigDecimal dok1 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getDok1).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok2 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getDok2).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok3 = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getDok3).reduce(BigDecimal.ZERO, BigDecimal::add);

    score.setDok1(String.valueOf(dok1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setDok2(String.valueOf(dok2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setDok3(String.valueOf(dok3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    return score;
  }

  private NumeracyScore createNumeracySectionByLevel(List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new NumeracyScore();
    }
    NumeracyScore score = new NumeracyScore();
    score.setLevel("Province");
    BigDecimal interpret = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyInterpret).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal apply = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyApply).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal solve = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracySolve).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal analyze = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyAnalyze).reduce(BigDecimal.ZERO, BigDecimal::add);

    score.setNumeracyInterpret(String.valueOf(interpret.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setNumeracyApply(String.valueOf(apply.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setNumeracySolve(String.valueOf(solve.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setNumeracyAnalyze(String.valueOf(analyze.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    return score;
  }

  private NumeracyScore createNumeracySectionByLevelForStaging(List<StagedAssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new NumeracyScore();
    }
    NumeracyScore score = new NumeracyScore();
    score.setLevel("Province");
    BigDecimal interpret = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getNumeracyInterpret).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal apply = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getNumeracyApply).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal solve = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getNumeracySolve).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal analyze = listOfDOARCalc.stream().map(StagedAssessmentStudentDOARCalculationEntity::getNumeracyAnalyze).reduce(BigDecimal.ZERO, BigDecimal::add);

    score.setNumeracyInterpret(String.valueOf(interpret.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setNumeracyApply(String.valueOf(apply.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setNumeracySolve(String.valueOf(solve.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    score.setNumeracyAnalyze(String.valueOf(analyze.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN)));
    return score;
  }

  private void setProficiencyLevels(List<AssessmentStudentLightEntity> students, DOARSummaryPage doarSummaryPage) {
    Map<String, List<AssessmentStudentLightEntity>> studentsByLevel =
            categorizeStudentsEntitiesByLevel(students);

    var provinceLevel = createProficiencyLevelSection(studentsByLevel.get("Province"));
      doarSummaryPage.getProficiencySection().add(
              provinceLevel
      );
  }

  private void setProficiencyLevelsForStaging(List<StagedAssessmentStudentLightEntity> students, DOARSummaryPage doarSummaryPage) {
    Map<String, List<StagedAssessmentStudentLightEntity>> studentsByLevel =
            categorizeStudentsEntitiesByLevelForStaging(students);

    var provinceLevel = createProficiencyLevelSectionForStaging(studentsByLevel.get("Province"));
    doarSummaryPage.getProficiencySection().add(
            provinceLevel
    );
  }

  private Map<String, List<AssessmentStudentLightEntity>> categorizeStudentsEntitiesByLevel(
          List<AssessmentStudentLightEntity> students) {

    Map<String, List<AssessmentStudentLightEntity>> result = new HashMap<>();;

    Set<UUID> provinceSchoolIds = getSchoolsInProvince().stream()
            .map(s -> UUID.fromString(s.getSchoolId()))
            .collect(Collectors.toSet());

    // Single pass through students
    List<AssessmentStudentLightEntity> provinceLevel = new ArrayList<>();

    for (AssessmentStudentLightEntity student : students) {
      UUID studentSchoolId = student.getSchoolAtWriteSchoolID();
      if (provinceSchoolIds.contains(studentSchoolId)) {
        provinceLevel.add(student);
      }
    }
    result.put("Province", provinceLevel);

    return result;
  }

  private Map<String, List<StagedAssessmentStudentLightEntity>> categorizeStudentsEntitiesByLevelForStaging(
          List<StagedAssessmentStudentLightEntity> students) {

    Map<String, List<StagedAssessmentStudentLightEntity>> result = new HashMap<>();;

    Set<UUID> provinceSchoolIds = getSchoolsInProvince().stream()
            .map(s -> UUID.fromString(s.getSchoolId()))
            .collect(Collectors.toSet());

    // Single pass through students
    List<StagedAssessmentStudentLightEntity> provinceLevel = new ArrayList<>();

    for (StagedAssessmentStudentLightEntity student : students) {
      UUID studentSchoolId = student.getSchoolAtWriteSchoolID();
      if (provinceSchoolIds.contains(studentSchoolId)) {
        provinceLevel.add(student);
      }
    }
    result.put("Province", provinceLevel);

    return result;
  }

  private ProficiencyLevel createProficiencyLevelSection(List<AssessmentStudentLightEntity> students) {
    int totalCount = students.size();
    ProficiencyLevel schoolProficiencyLevel =  new ProficiencyLevel();
    schoolProficiencyLevel.setLevel("Province");
    schoolProficiencyLevel.setNumberCounted(String.valueOf(totalCount));
    schoolProficiencyLevel.setStudentsWithProficiency1(String.valueOf(getProficiencyScorePercent(students, 1, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency2(String.valueOf(getProficiencyScorePercent(students, 2, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency3(String.valueOf(getProficiencyScorePercent(students, 3, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency4(String.valueOf(getProficiencyScorePercent(students, 4, totalCount)));
    schoolProficiencyLevel.setNotCounted(String.valueOf(getSpecialCasePercent(students, "X", totalCount)));

    return schoolProficiencyLevel;
  }

  private ProficiencyLevel createProficiencyLevelSectionForStaging(List<StagedAssessmentStudentLightEntity> students) {
    int totalCount = students.size();
    ProficiencyLevel schoolProficiencyLevel =  new ProficiencyLevel();
    schoolProficiencyLevel.setLevel("Province");
    schoolProficiencyLevel.setNumberCounted(String.valueOf(totalCount));
    schoolProficiencyLevel.setStudentsWithProficiency1(String.valueOf(getProficiencyScorePercentForStaging(students, 1, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency2(String.valueOf(getProficiencyScorePercentForStaging(students, 2, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency3(String.valueOf(getProficiencyScorePercentForStaging(students, 3, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency4(String.valueOf(getProficiencyScorePercentForStaging(students, 4, totalCount)));
    schoolProficiencyLevel.setNotCounted(String.valueOf(getSpecialCasePercentForStaging(students, "X", totalCount)));

    return schoolProficiencyLevel;
  }

  private BigDecimal getSpecialCasePercent(List<AssessmentStudentLightEntity> students, String caseCode, int count) {
    if(count == 0) {
      return BigDecimal.ZERO;
    }
    var studentsWithProfScore = students.stream().filter(student -> StringUtils.isNotBlank(student.getProvincialSpecialCaseCode()) && student.getProvincialSpecialCaseCode().equalsIgnoreCase(caseCode)).toList();
    if(studentsWithProfScore.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(studentsWithProfScore.size()).multiply(new BigDecimal(100)).divide(new BigDecimal(count), 2, RoundingMode.DOWN);
  }

  private BigDecimal getSpecialCasePercentForStaging(List<StagedAssessmentStudentLightEntity> students, String caseCode, int count) {
    if(count == 0) {
      return BigDecimal.ZERO;
    }
    var studentsWithProfScore = students.stream().filter(student -> StringUtils.isNotBlank(student.getProvincialSpecialCaseCode()) && student.getProvincialSpecialCaseCode().equalsIgnoreCase(caseCode)).toList();
    if(studentsWithProfScore.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(studentsWithProfScore.size()).multiply(new BigDecimal(100)).divide(new BigDecimal(count), 2, RoundingMode.DOWN);
  }

  private BigDecimal getProficiencyScorePercent(List<AssessmentStudentLightEntity> students, int score, int count) {
    if(count == 0) {
      return BigDecimal.ZERO;
    }
    var studentsWithProfScore = students.stream().filter(student -> student.getProficiencyScore() != null && student.getProficiencyScore() == score).toList();
    if(studentsWithProfScore.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(studentsWithProfScore.size()).multiply(new BigDecimal(100)).divide(new BigDecimal(count), 2, RoundingMode.DOWN);
  }

  private BigDecimal getProficiencyScorePercentForStaging(List<StagedAssessmentStudentLightEntity> students, int score, int count) {
    if(count == 0) {
      return BigDecimal.ZERO;
    }
    var studentsWithProfScore = students.stream().filter(student -> student.getProficiencyScore() != null && student.getProficiencyScore() == score).toList();
    if(studentsWithProfScore.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(studentsWithProfScore.size()).multiply(new BigDecimal(100)).divide(new BigDecimal(count), 2, RoundingMode.DOWN);
  }

  private List<SchoolTombstone> getSchoolsInProvince() {
    var schools = restUtils.getAllSchoolTombstones();
      return schools.stream().filter(school -> school.getSchoolCategoryCode().equalsIgnoreCase(PUBLIC)
              || school.getSchoolCategoryCode().equalsIgnoreCase(YUKON.getCode()) ||  school.getSchoolCategoryCode().equalsIgnoreCase(INDEPEND.getCode())
              ||  school.getSchoolCategoryCode().equalsIgnoreCase(INDP_FNS.getCode()) || school.getSchoolCategoryCode().equalsIgnoreCase(OFFSHORE.getCode())).toList();
  }

  private String getReportTitle(String assessmentTypeCode) {
    return switch (assessmentTypeCode) {
      case NME10 -> "Grade 10 Numeracy Assessment";
      case NMF10 -> "Évaluation de numératie de la 10ᵉ année";
      case LTE12 -> "Grade 12 Literacy Assessment";
      case LTP10 -> "Évaluation de littératie de la 10e année – Français langue première";
      case LTP12 -> "Évaluation de littératie de la 12e année – Français langue première";
      case LTE10 -> "Grade 10 Literacy Assessment";
      case LTF12 -> "Évaluation de littératie de la 12e année – Français langue seconde – immersion";
      default -> null;
    };
  }
}
