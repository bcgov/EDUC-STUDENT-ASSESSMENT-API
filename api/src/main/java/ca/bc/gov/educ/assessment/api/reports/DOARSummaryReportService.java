package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentDOARCalculationEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentDOARCalculationRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentLightRepository;
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
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service class for generating School Students by Assessment Report
 */
@Service
@Slf4j
public class DOARSummaryReportService extends BaseReportGenerationService {

  private final AssessmentSessionRepository assessmentSessionRepository;
  private final AssessmentStudentLightRepository assessmentStudentLightRepository;
  private final AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository;
  private final RestUtils restUtils;
  private JasperReport doarSummaryReport;
  private static final String INDEPENDENT= "Independent";
  private static final String DISTRICT= "District";
  private static final String SCHOOL= "School";
  private static final String PUBLIC= "Public";
  private static final String ALL_INDP= "All Independent";
  private static final String NME10= "NME10";
  private static final String NMF10= "NMF10";
  private static final String LTE10= "LTE10";
  private static final String LTE12= "LTE12";
  private static final String LTP10= "LTP10";
  private static final String LTP12= "LTP12";
  private static final String LTF12= "LTF12";

  public DOARSummaryReportService(AssessmentSessionRepository assessmentSessionRepository, AssessmentStudentLightRepository assessmentStudentLightRepository, AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository, RestUtils restUtils) {
    super(restUtils);
    this.assessmentSessionRepository = assessmentSessionRepository;
    this.assessmentStudentLightRepository = assessmentStudentLightRepository;
    this.assessmentStudentDOARCalculationRepository = assessmentStudentDOARCalculationRepository;
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
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/doarSummary.jrxml");
      doarSummaryReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentAssessmentAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateDOARSummaryReport(UUID assessmentSessionID, UUID schoolID){
    try {
      log.debug("generateDOARSummaryReport");
      var session = assessmentSessionRepository.findById(assessmentSessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", assessmentSessionID.toString()));
      var students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCode(assessmentSessionID, StudentStatusCodes.ACTIVE.getCode());

      log.debug("no. of students found {}", students.size());
      var school = validateAndReturnSchool(schoolID);
      boolean isIndependent = school.getIndependentAuthorityId() != null;
      DOARSummaryNode doarSummaryNode = new DOARSummaryNode();
      doarSummaryNode.setSummaryPages(new ArrayList<>());

      var studentsByAssessment = organizeStudentsInEachAssessment(students);
      log.debug("organizeStudentsInEachAssessment");
      studentsByAssessment.forEach((assessmentType, studentList) -> {
        DOARSummaryPage doarSummaryPage =  new DOARSummaryPage();
        setReportTombstoneValues(session, doarSummaryPage, assessmentType, school);
        doarSummaryPage.setProficiencySection(new ArrayList<>());
        doarSummaryPage.setTaskScore(new ArrayList<>());
        doarSummaryPage.setComprehendScore(new ArrayList<>());
        doarSummaryPage.setCommunicateScore(new ArrayList<>());
        doarSummaryPage.setCommunicateOralScore(new ArrayList<>());
        doarSummaryPage.setNumeracyScore(new ArrayList<>());
        doarSummaryPage.setCognitiveLevelScore(new ArrayList<>());

        log.debug("setProficiencyLevels start for assessment {}", assessmentType);
        setProficiencyLevels(studentList, isIndependent, school, doarSummaryPage);
        log.debug("setProficiencyLevels end for assessment {}", assessmentType);

        log.debug("setAssessmentRawScores start for assessment {}", assessmentType);
        setAssessmentRawScores(studentList, isIndependent, school, doarSummaryPage, assessmentType);
        log.debug("setAssessmentRawScores start for assessment {}", assessmentType);

        doarSummaryNode.getSummaryPages().add(doarSummaryPage);
      });
      log.debug("generating report");
      return generateJasperReport(objectWriter.writeValueAsString(doarSummaryNode), doarSummaryReport, AssessmentReportTypeCode.DOAR_SUMMARY.getCode());
    }
    catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
    }
  }
  
  private HashMap<String, List<AssessmentStudentLightEntity>> organizeStudentsInEachAssessment(List<AssessmentStudentLightEntity> students) {
    HashMap<String, List<AssessmentStudentLightEntity>> studentsHash = new HashMap<>();
    
    students.forEach(student -> {
      if(studentsHash.containsKey(student.getAssessmentEntity().getAssessmentTypeCode())) {
        studentsHash.get(student.getAssessmentEntity().getAssessmentTypeCode()).add(student);
      }else{
        List<AssessmentStudentLightEntity> studentList = new ArrayList<>();
        studentList.add(student);
        studentsHash.put(student.getAssessmentEntity().getAssessmentTypeCode(), studentList);
      }
    });
    
    return studentsHash;
  }

  protected void setReportTombstoneValues(AssessmentSessionEntity assessmentSession, DOARSummaryPage reportNode, String assessmentType, SchoolTombstone school){
    if(school.getIndependentAuthorityId() != null) {
      var authority = validateAndReturnAuthority(school);
      reportNode.setDistrictNumberAndName(authority.getAuthorityNumber() + " - " + authority.getDisplayName());
    }else{
      var district = validateAndReturnDistrict(school);
      reportNode.setDistrictNumberAndName(district.getDistrictNumber() + " - " + district.getDisplayName());
    }

    reportNode.setReportGeneratedDate("Report Generated: " + LocalDate.now().format(formatter));
    reportNode.setSession(assessmentSession.getCourseYear() + "/" + assessmentSession.getCourseMonth() + " Session");
    reportNode.setSchoolMincodeAndName(school.getMincode() + " - " + school.getDisplayName());
    reportNode.setAssessmentType(assessmentType);
    reportNode.setReportId(UUID.randomUUID().toString());
    reportNode.setReportTitle(getReportTitle(assessmentType));
  }

  private void setAssessmentRawScores(List<AssessmentStudentLightEntity> students, boolean isIndependent, SchoolTombstone school, DOARSummaryPage doarSummaryPage, String assessmentType) {
    UUID assessmentID = students.getFirst().getAssessmentEntity().getAssessmentID();
    Map<String, Set<UUID>> studentIdsByLevel = categorizeStudentsByLevel(students, school, isIndependent);

    List<UUID> allStudentIds = students.stream()
            .map(AssessmentStudentLightEntity::getAssessmentStudentID)
            .toList();
    var allDOARCalc = assessmentStudentDOARCalculationRepository
            .findAllByAssessmentIDAndAssessmentStudentIDIn(assessmentID, allStudentIds);

    Map<UUID, AssessmentStudentDOARCalculationEntity> doarCalcMap = allDOARCalc.stream()
            .collect(Collectors.toMap(AssessmentStudentDOARCalculationEntity::getAssessmentStudentID, Function.identity()));

    List<AssessmentStudentDOARCalculationEntity> schoolLevelDOARCalc =
            filterDOARCalcByStudentIds(doarCalcMap, studentIdsByLevel.get(SCHOOL));
    List<AssessmentStudentDOARCalculationEntity> provinceLevelDOARCalc =
            filterDOARCalcByStudentIds(doarCalcMap, studentIdsByLevel.get("Province"));

    if(isIndependent) {
      List<AssessmentStudentDOARCalculationEntity> indpLevelDOARCalc =
              filterDOARCalcByStudentIds(doarCalcMap, studentIdsByLevel.get(INDEPENDENT));
      populateRawScoresForIndependentSchools(schoolLevelDOARCalc, provinceLevelDOARCalc,
              indpLevelDOARCalc, doarSummaryPage, assessmentType);
    } else {
      List<AssessmentStudentDOARCalculationEntity> districtLevelDOARCalc =
              filterDOARCalcByStudentIds(doarCalcMap, studentIdsByLevel.get(DISTRICT));
      List<AssessmentStudentDOARCalculationEntity> publicLevelDOARCalc =
              filterDOARCalcByStudentIds(doarCalcMap, studentIdsByLevel.get(PUBLIC));
      populateRawScoresForPublicSchools(schoolLevelDOARCalc, districtLevelDOARCalc,
              publicLevelDOARCalc, provinceLevelDOARCalc, doarSummaryPage, assessmentType);
    }
  }

  private List<AssessmentStudentDOARCalculationEntity> filterDOARCalcByStudentIds(
          Map<UUID, AssessmentStudentDOARCalculationEntity> doarCalcMap, Set<UUID> studentIds) {
    return studentIds.stream()
            .map(doarCalcMap::get)
            .filter(Objects::nonNull)
            .toList();
  }

  private Map<String, Set<UUID>> categorizeStudentsByLevel(
          List<AssessmentStudentLightEntity> students, SchoolTombstone school, boolean isIndependent) {

    Map<String, Set<UUID>> result = new HashMap<>();
    UUID schoolId = UUID.fromString(school.getSchoolId());

    Set<UUID> districtSchoolIds = isIndependent ? Collections.emptySet() :
            getSchoolsByLevel(DISTRICT, school.getDistrictId()).stream()
                    .map(s -> UUID.fromString(s.getSchoolId()))
                    .collect(Collectors.toSet());

    Set<UUID> publicSchoolIds = isIndependent ? Collections.emptySet() :
            getSchoolsByLevel(PUBLIC, null).stream()
                    .map(s -> UUID.fromString(s.getSchoolId()))
                    .collect(Collectors.toSet());

    Set<UUID> independentSchoolIds = isIndependent ?
            getSchoolsByLevel(INDEPENDENT, school.getIndependentAuthorityId()).stream()
                    .map(s -> UUID.fromString(s.getSchoolId()))
                    .collect(Collectors.toSet()) : Collections.emptySet();

    Set<UUID> schoolLevel = new HashSet<>();
    Set<UUID> districtLevel = new HashSet<>();
    Set<UUID> publicLevel = new HashSet<>();
    Set<UUID> independentLevel = new HashSet<>();
    Set<UUID> provinceLevel = new HashSet<>();

    for (AssessmentStudentLightEntity student : students) {
      UUID studentSchoolId = student.getSchoolAtWriteSchoolID();
      UUID studentId = student.getAssessmentStudentID();

      provinceLevel.add(studentId);

      if (schoolId.equals(studentSchoolId)) {
        schoolLevel.add(studentId);
      }
      if (districtSchoolIds.contains(studentSchoolId)) {
        districtLevel.add(studentId);
      }
      if (publicSchoolIds.contains(studentSchoolId)) {
        publicLevel.add(studentId);
      }
      if (independentSchoolIds.contains(studentSchoolId)) {
        independentLevel.add(studentId);
      }
    }

    result.put(SCHOOL, schoolLevel);
    result.put(DISTRICT, districtLevel);
    result.put(PUBLIC, publicLevel);
    result.put(INDEPENDENT, independentLevel);
    result.put("Province", provinceLevel);

    return result;
  }

  private void populateRawScoresForIndependentSchools(List<AssessmentStudentDOARCalculationEntity> schoolLevel, List<AssessmentStudentDOARCalculationEntity> provinceLevel, List<AssessmentStudentDOARCalculationEntity> indpLevel, DOARSummaryPage doarSummaryPage, String assessmentType) {
    List<TaskScore> taskScores = List.of(createTaskSectionByLevel(assessmentType, SCHOOL, schoolLevel), createTaskSectionByLevel(assessmentType, ALL_INDP, indpLevel), createTaskSectionByLevel(assessmentType, "Province", provinceLevel));
    doarSummaryPage.getTaskScore().addAll(taskScores);

    List<CognitiveLevelScore> cognScore = List.of(createCognitiveSectionByLevel(SCHOOL, schoolLevel), createCognitiveSectionByLevel(ALL_INDP, indpLevel), createCognitiveSectionByLevel("Province", provinceLevel));
    doarSummaryPage.getCognitiveLevelScore().addAll(cognScore);

    switch (assessmentType) {
      case NME10, NMF10 ->  {
        List<NumeracyScore> numScores = List.of(createNumeracySectionByLevel(SCHOOL, schoolLevel), createNumeracySectionByLevel(ALL_INDP, indpLevel), createNumeracySectionByLevel("Province", provinceLevel));
        doarSummaryPage.getNumeracyScore().addAll(numScores);
      }
      case LTE10, LTE12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, SCHOOL, schoolLevel), createComprehendSectionByLevel(assessmentType, ALL_INDP, indpLevel), createComprehendSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, SCHOOL, schoolLevel), createCommunicateSectionByLevel(assessmentType, ALL_INDP, indpLevel), createCommunicateSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);
      }
      case LTP10, LTP12, LTF12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, SCHOOL, schoolLevel), createComprehendSectionByLevel(assessmentType, ALL_INDP, indpLevel), createComprehendSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, SCHOOL, schoolLevel), createCommunicateSectionByLevel(assessmentType, ALL_INDP, indpLevel), createCommunicateSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);

        List<CommunicateOralScore> communicateOralScores = List.of(createCommunicateOralSectionByLevel(assessmentType, SCHOOL, schoolLevel), createCommunicateOralSectionByLevel(assessmentType, ALL_INDP, indpLevel) ,createCommunicateOralSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getCommunicateOralScore().addAll(communicateOralScores);
      }
      default -> log.info("Invalid assessment type");
    }
  }

  private void populateRawScoresForPublicSchools(List<AssessmentStudentDOARCalculationEntity> schoolLevel, List<AssessmentStudentDOARCalculationEntity> districtLevel, List<AssessmentStudentDOARCalculationEntity> publicLevel, List<AssessmentStudentDOARCalculationEntity> provinceLevel, DOARSummaryPage doarSummaryPage, String assessmentType) {
    List<TaskScore> taskScores = List.of(createTaskSectionByLevel(assessmentType, SCHOOL, schoolLevel), createTaskSectionByLevel(assessmentType, DISTRICT, districtLevel), createTaskSectionByLevel(assessmentType, "All Public", publicLevel), createTaskSectionByLevel(assessmentType, "Province", provinceLevel));
    doarSummaryPage.getTaskScore().addAll(taskScores);

    List<CognitiveLevelScore> cognScore = List.of(createCognitiveSectionByLevel(SCHOOL, schoolLevel), createCognitiveSectionByLevel(DISTRICT, districtLevel), createCognitiveSectionByLevel("All Public", publicLevel), createCognitiveSectionByLevel("Province", provinceLevel));
    doarSummaryPage.getCognitiveLevelScore().addAll(cognScore);

    switch (assessmentType) {
      case NME10, NMF10 ->  {
        List<NumeracyScore> numScores = List.of(createNumeracySectionByLevel(SCHOOL, schoolLevel), createNumeracySectionByLevel(DISTRICT, districtLevel), createNumeracySectionByLevel("All Public", publicLevel), createNumeracySectionByLevel("Province", provinceLevel));
        doarSummaryPage.getNumeracyScore().addAll(numScores);
      }
      case LTE10, LTE12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, SCHOOL, schoolLevel), createComprehendSectionByLevel(assessmentType, DISTRICT, districtLevel), createComprehendSectionByLevel(assessmentType, "All Public", publicLevel), createComprehendSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, SCHOOL, schoolLevel), createCommunicateSectionByLevel(assessmentType, DISTRICT, districtLevel), createCommunicateSectionByLevel(assessmentType, "All Public", publicLevel), createCommunicateSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);
      }
      case LTP10, LTP12, LTF12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, SCHOOL, schoolLevel), createComprehendSectionByLevel(assessmentType, DISTRICT, districtLevel), createComprehendSectionByLevel(assessmentType, "All Public", publicLevel), createComprehendSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, SCHOOL, schoolLevel), createCommunicateSectionByLevel(assessmentType, DISTRICT, districtLevel), createCommunicateSectionByLevel(assessmentType, "All Public", publicLevel), createCommunicateSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);

        List<CommunicateOralScore> communicateOralScores = List.of(createCommunicateOralSectionByLevel(assessmentType, SCHOOL, schoolLevel), createCommunicateOralSectionByLevel(assessmentType, DISTRICT, districtLevel), createCommunicateOralSectionByLevel(assessmentType, "All Public", publicLevel) ,createCommunicateOralSectionByLevel(assessmentType, "Province", provinceLevel));
        doarSummaryPage.getCommunicateOralScore().addAll(communicateOralScores);
      }
      default -> log.info("Invalid assessment type");
    }
  }

  private TaskScore createTaskSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new TaskScore();
    }
    TaskScore score = new TaskScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case NME10, NMF10 ->  {
        BigDecimal taskPlanSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskPlan).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskEstimateSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskEstimate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskFairSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskFair).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskModelSum = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskModel).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskPlan(String.valueOf(taskPlanSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setTaskEstimate(String.valueOf(taskEstimateSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setTaskFair(String.valueOf(taskFairSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setTaskModel(String.valueOf(taskModelSum.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));

        yield score;
      }
      case LTE10, LTE12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskComprehend(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setTaskCommunicate(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));

        yield score;
      }
      case LTP10, LTP12, LTF12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskCommOral = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskOral).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setTaskComprehend(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setTaskCommunicate(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setTaskOral(String.valueOf(taskCommOral.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));

        yield score;
      }
     default -> score;
    };
  }

  private ComprehendScore createComprehendSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new ComprehendScore();
    }
    ComprehendScore score = new ComprehendScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case LTE10, LTE12, LTP10, LTP12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartA).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskComm = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartB).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartA(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setComprehendPartB(String.valueOf(taskComm.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal taskCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartATask).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal infoCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartBInfo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expCompre = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartBExp).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartATask(String.valueOf(taskCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setComprehendPartBInfo(String.valueOf(infoCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setComprehendPartBExp(String.valueOf(expCompre.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateScore createCommunicateSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateScore();
    }
    CommunicateScore score = new CommunicateScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case LTE10, LTE12, LTP12 -> {
        BigDecimal commGO = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRB = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateGraphicOrg(String.valueOf(commGO.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateUnderstanding(String.valueOf(commWRA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicatePersonalConn(String.valueOf(commWRB.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        yield score;
      }
      case LTP10 -> {
        BigDecimal comprePartA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commGO = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRB = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateGraphicOrg(String.valueOf(commGO.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateUnderstanding(String.valueOf(commWRA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicatePersonalConn(String.valueOf(commWRB.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setComprehendPartAShort(String.valueOf(comprePartA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal background = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDissertationBackground).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal form = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDissertationForm).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comprePartA = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setComprehendPartAShort(String.valueOf(comprePartA.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setDissertationBackground(String.valueOf(background.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setDissertationForm(String.valueOf(form.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateOralScore createCommunicateOralSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateOralScore();
    }
    CommunicateOralScore score = new CommunicateOralScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case LTP12, LTP10 -> {
        BigDecimal part1 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart3).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateOralPart1(String.valueOf(part1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateOralPart2(String.valueOf(part2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateOralPart3(String.valueOf(part3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        yield score;
      }
      case LTF12 -> {
        BigDecimal part1 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Form).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part4 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part5 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part6 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Form).reduce(BigDecimal.ZERO, BigDecimal::add);

        score.setCommunicateOralPart1Background(String.valueOf(part1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateOralPart1Form(String.valueOf(part2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateOralPart1Expression(String.valueOf(part3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateOralPart2Expression(String.valueOf(part4.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateOralPart2Background(String.valueOf(part5.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        score.setCommunicateOralPart2Form(String.valueOf(part6.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
        yield score;
      }
      default -> score;
    };
  }

  private CognitiveLevelScore createCognitiveSectionByLevel(String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CognitiveLevelScore();
    }
    CognitiveLevelScore score = new CognitiveLevelScore();
    score.setLevel(level);
    BigDecimal dok1 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok1).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok2 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok2).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok3 = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok3).reduce(BigDecimal.ZERO, BigDecimal::add);

    score.setDok1(String.valueOf(dok1.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
    score.setDok2(String.valueOf(dok2.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
    score.setDok3(String.valueOf(dok3.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
    return score;
  }

  private NumeracyScore createNumeracySectionByLevel(String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new NumeracyScore();
    }
    NumeracyScore score = new NumeracyScore();
    score.setLevel(level);
    BigDecimal interpret = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyInterpret).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal apply = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyApply).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal solve = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracySolve).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal analyze = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyAnalyze).reduce(BigDecimal.ZERO, BigDecimal::add);

    score.setNumeracyInterpret(String.valueOf(interpret.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
    score.setNumeracyApply(String.valueOf(apply.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
    score.setNumeracySolve(String.valueOf(solve.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
    score.setNumeracyAnalyze(String.valueOf(analyze.divide(new BigDecimal(noOfStudents), 2, RoundingMode.HALF_UP)));
    return score;
  }

  private void setProficiencyLevels(List<AssessmentStudentLightEntity> students, boolean isIndependent, SchoolTombstone school, DOARSummaryPage doarSummaryPage) {
    var schoolLevelStudents = getStudentsInSameSchool(students, school);
    var provinceLevel = createProficiencyLevelSection("Province", students);
    if(isIndependent) {
      var indpLevelStudents = getStudentsByLevel(students, school, INDEPENDENT);
      doarSummaryPage.getProficiencySection().addAll(List.of(createProficiencyLevelSection(SCHOOL, schoolLevelStudents), createProficiencyLevelSection(ALL_INDP, indpLevelStudents), provinceLevel));
    } else {
      var districtLevelStudents = getStudentsByLevel(students, school, DISTRICT);
      var publicLevelStudents =  getStudentsByLevel(students, school, PUBLIC);
      doarSummaryPage.getProficiencySection().addAll(
              List.of(createProficiencyLevelSection(SCHOOL, schoolLevelStudents), createProficiencyLevelSection(DISTRICT, districtLevelStudents), createProficiencyLevelSection("All Public", publicLevelStudents),provinceLevel)
      );
    }
  }

  private List<AssessmentStudentLightEntity> getStudentsInSameSchool(List<AssessmentStudentLightEntity> students, SchoolTombstone school) {
    return students.stream().filter(student -> Objects.equals(student.getSchoolAtWriteSchoolID(), UUID.fromString(school.getSchoolId()))).toList();
  }

  private List<AssessmentStudentLightEntity> getStudentsByLevel(List<AssessmentStudentLightEntity> students, SchoolTombstone school, String level) {
    return students.stream().filter(student ->
                    getSchoolsByLevel(level, school.getDistrictId()).stream().anyMatch(schoolByLevel -> Objects.equals(student.getSchoolAtWriteSchoolID(), UUID.fromString(school.getSchoolId()))))
            .toList();
  }

  private ProficiencyLevel createProficiencyLevelSection(String level, List<AssessmentStudentLightEntity> students) {
    int totalCount = students.size();
    ProficiencyLevel schoolProficiencyLevel =  new ProficiencyLevel();
    schoolProficiencyLevel.setLevel(level);
    schoolProficiencyLevel.setNumberCounted(String.valueOf(totalCount));
    schoolProficiencyLevel.setStudentsWithProficiency1(String.valueOf(getProficiencyScorePercent(students, 1, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency2(String.valueOf(getProficiencyScorePercent(students, 2, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency3(String.valueOf(getProficiencyScorePercent(students, 3, totalCount)));
    schoolProficiencyLevel.setStudentsWithProficiency4(String.valueOf(getProficiencyScorePercent(students, 4, totalCount)));

    return schoolProficiencyLevel;
  }

  private Integer getProficiencyScorePercent(List<AssessmentStudentLightEntity> students, int score, int count) {
    if(count == 0) {
      return 0;
    }
    int sum = students.stream().map(AssessmentStudentLightEntity::getProficiencyScore).filter(proficiencyScore -> proficiencyScore!= null && proficiencyScore == score).reduce(0, Integer::sum);
    return (sum * 100) / count;
  }

  private List<SchoolTombstone> getSchoolsByLevel(String level, String districtOrAuthorityID) {
    var schools = restUtils.getAllSchoolTombstones();
    return switch (level) {
      case DISTRICT, INDEPENDENT -> schools.stream().filter(school -> school.getDistrictId().equals(districtOrAuthorityID)).toList();
      case PUBLIC -> schools.stream().filter(school -> school.getSchoolCategoryCode().equalsIgnoreCase(PUBLIC)).toList();
      default -> Collections.emptyList();
    };
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
