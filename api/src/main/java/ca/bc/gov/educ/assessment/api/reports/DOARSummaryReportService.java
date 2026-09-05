package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.PreconditionRequiredException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentDOARCalculationEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentDOARCalculationRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentLightRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.doar.*;
import ca.bc.gov.educ.assessment.api.util.TextNormalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
  private static final String PROVINCE = "Province";
  private static final String ALL_PUBLIC = "All Public";
  private static final String SCHOOL_CSF = "École";
  private static final String DISTRICT_CSF = "Conseil scolaire";
  private static final String PUBLIC_CSF = "Écoles publiques";
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

  public boolean isDOARSummaryAvailable(UUID sessionID, UUID schoolID) {
    return assessmentStudentLightRepository.countBySessionIDAndSchoolIDWithResults(sessionID, schoolID) > 0;
  }

  public List<UUID> getDistrictSchoolIDsWithResults(UUID sessionID, UUID districtID) {
    List<UUID> districtSchoolIDs = getDistrictPublicSchoolTombstones(districtID).stream()
            .map(school -> UUID.fromString(school.getSchoolId()))
            .toList();
    if (districtSchoolIDs.isEmpty()) {
      return Collections.emptyList();
    }
    return assessmentStudentLightRepository.findSchoolIDsWithResultsBySessionID(sessionID, districtSchoolIDs);
  }

  public List<SchoolTombstone> getDistrictPublicSchoolTombstones(UUID districtID) {
    var schoolTombstones = restUtils.getAllSchoolTombstones();
    return schoolTombstones.stream()
            .filter(school -> StringUtils.isNotBlank(school.getSchoolId()))
            .filter(school -> districtID.toString().equalsIgnoreCase(school.getDistrictId()))
            .filter(school -> StringUtils.isBlank(school.getIndependentAuthorityId()))
            .filter(school -> !INDEPENDENTS_AND_OFFSHORE.contains(school.getSchoolCategoryCode()))
            .toList();
  }

  public boolean isDistrictDOARSummaryAvailable(UUID sessionID, UUID districtID) {
    return !getDistrictSchoolIDsWithResults(sessionID, districtID).isEmpty();
  }

  public DownloadableReportResponse generateDOARSummaryReport(UUID assessmentSessionID, UUID schoolID){
    try {
      var session = assessmentSessionRepository.findById(assessmentSessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", assessmentSessionID.toString()));
      var students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeAndProficiencyScoreIsNotNullOrProvincialSpecialCaseCode(assessmentSessionID, StudentStatusCodes.ACTIVE.getCode(), ProvincialSpecialCaseCodes.NOTCOMPLETED.getCode());
      var school = validateAndReturnSchool(schoolID);

      boolean schoolHasAnyResult = students.stream().anyMatch(student -> Objects.equals(student.getSchoolAtWriteSchoolID(), schoolID));
      if(!schoolHasAnyResult){
        throw new PreconditionRequiredException(AssessmentSessionEntity.class, "Results not available in this session:: ", session.getSessionID().toString());
      }
      DOARSummaryNode doarSummaryNode = new DOARSummaryNode();
      doarSummaryNode.setReports(new ArrayList<>());

      var studentsByAssessment = organizeStudentsInEachAssessment(students);
      addSchoolPagesToNode(session, studentsByAssessment, school, doarSummaryNode);
      var normalized = TextNormalizer.normalizeObject(doarSummaryNode);
      var payload = objectWriter.writeValueAsString(normalized);
      return generateJasperReport(payload, doarSummaryReport, AssessmentReportTypeCode.DOAR_SUMMARY.getCode());
    }
    catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
    }
  }

  public void streamDistrictDOARSummaryReport(UUID assessmentSessionID, UUID districtID, HttpServletResponse response) throws IOException {
    var doarSummaryNode = buildDistrictDOARSummaryNode(assessmentSessionID, districtID);
    var normalized = TextNormalizer.normalizeObject(doarSummaryNode);
    var payload = objectWriter.writeValueAsString(normalized);
    var district = restUtils.getDistrictByDistrictID(districtID.toString()).orElseThrow(() -> new EntityNotFoundException(District.class, "districtID", districtID.toString()));
    String filename = "%s-DOAR Summary.pdf".formatted(district.getDistrictNumber());
    generateJasperReportStream(payload, doarSummaryReport, filename, response);
  }

  private DOARSummaryNode buildDistrictDOARSummaryNode(UUID assessmentSessionID, UUID districtID){
    var session = assessmentSessionRepository.findById(assessmentSessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", assessmentSessionID.toString()));
    restUtils.getDistrictByDistrictID(districtID.toString()).orElseThrow(() -> new EntityNotFoundException(District.class, "districtID", districtID.toString()));
    var students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeAndProficiencyScoreIsNotNullOrProvincialSpecialCaseCode(assessmentSessionID, StudentStatusCodes.ACTIVE.getCode(), ProvincialSpecialCaseCodes.NOTCOMPLETED.getCode());

    Set<UUID> schoolIDsWithResults = students.stream()
            .map(AssessmentStudentLightEntity::getSchoolAtWriteSchoolID)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    var districtSchoolsWithResults = getDistrictPublicSchoolTombstones(districtID).stream()
            .filter(school -> schoolIDsWithResults.contains(UUID.fromString(school.getSchoolId())))
            .sorted(Comparator.comparing(SchoolTombstone::getMincode))
            .toList();

    if(districtSchoolsWithResults.isEmpty()){
      throw new PreconditionRequiredException(AssessmentSessionEntity.class, "Results not available in this session:: ", session.getSessionID().toString());
    }

    Set<UUID> districtLevelSchoolIds = getSchoolsByLevel(DISTRICT, districtID.toString()).stream()
            .map(school -> UUID.fromString(school.getSchoolId()))
            .collect(Collectors.toSet());
    Set<UUID> publicLevelSchoolIds = getSchoolsByLevel(PUBLIC, null).stream()
            .map(school -> UUID.fromString(school.getSchoolId()))
            .collect(Collectors.toSet());
    Set<UUID> provinceLevelSchoolIds = getSchoolsByLevel(PROVINCE, null).stream()
            .map(school -> UUID.fromString(school.getSchoolId()))
            .collect(Collectors.toSet());

    var studentsByAssessment = organizeStudentsInEachAssessment(students);
    Map<String, DistrictAssessmentData> dataByAssessment = new HashMap<>();
    for (var entry : studentsByAssessment.entrySet()) {
      dataByAssessment.put(entry.getKey(), prepareDistrictAssessmentData(entry.getKey(), entry.getValue(), districtLevelSchoolIds, publicLevelSchoolIds, provinceLevelSchoolIds));
    }

    boolean canonicalIsCsf = districtSchoolsWithResults.getFirst().getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode());
    Map<String, SharedAssessmentSections> sharedSectionsByAssessment = new HashMap<>();
    dataByAssessment.forEach((assessmentType, data) -> sharedSectionsByAssessment.put(assessmentType, buildSharedAssessmentSections(assessmentType, canonicalIsCsf, data)));

    DOARSummaryNode doarSummaryNode = new DOARSummaryNode();
    doarSummaryNode.setReports(new ArrayList<>());
    districtSchoolsWithResults.forEach(school -> addDistrictSchoolPagesToNode(session, dataByAssessment, sharedSectionsByAssessment, canonicalIsCsf, school, doarSummaryNode));
    return doarSummaryNode;
  }

  private DistrictAssessmentData prepareDistrictAssessmentData(String assessmentType, List<AssessmentStudentLightEntity> assessmentStudents, Set<UUID> districtLevelSchoolIds, Set<UUID> publicLevelSchoolIds, Set<UUID> provinceLevelSchoolIds) {
    Map<UUID, List<AssessmentStudentLightEntity>> studentsBySchool = new HashMap<>();
    List<AssessmentStudentLightEntity> districtLevelStudents = new ArrayList<>();
    List<AssessmentStudentLightEntity> publicLevelStudents = new ArrayList<>();
    List<AssessmentStudentLightEntity> provinceLevelStudents = new ArrayList<>();

    for (AssessmentStudentLightEntity student : assessmentStudents) {
      UUID schoolId = student.getSchoolAtWriteSchoolID();
      if (schoolId == null) {
        continue;
      }
      studentsBySchool.computeIfAbsent(schoolId, id -> new ArrayList<>()).add(student);
      if (districtLevelSchoolIds.contains(schoolId)) {
        districtLevelStudents.add(student);
      }
      if (publicLevelSchoolIds.contains(schoolId)) {
        publicLevelStudents.add(student);
      }
      if (provinceLevelSchoolIds.contains(schoolId)) {
        provinceLevelStudents.add(student);
      }
    }

    UUID assessmentID = assessmentStudents.getFirst().getAssessmentEntity().getAssessmentID();
    Map<UUID, AssessmentStudentDOARCalculationEntity> calcByStudentID = assessmentStudentDOARCalculationRepository
            .findAllByAssessmentID(assessmentID).stream()
            .collect(Collectors.toMap(AssessmentStudentDOARCalculationEntity::getAssessmentStudentID, Function.identity()));

    return new DistrictAssessmentData(assessmentType, studentsBySchool, calcByStudentID,
            districtLevelStudents, publicLevelStudents, provinceLevelStudents,
            calcForStudents(calcByStudentID, districtLevelStudents),
            calcForStudents(calcByStudentID, publicLevelStudents),
            calcForStudents(calcByStudentID, provinceLevelStudents));
  }

  private List<AssessmentStudentDOARCalculationEntity> calcForStudents(Map<UUID, AssessmentStudentDOARCalculationEntity> calcByStudentID, List<AssessmentStudentLightEntity> students) {
    return students.stream()
            .map(student -> calcByStudentID.get(student.getAssessmentStudentID()))
            .filter(Objects::nonNull)
            .toList();
  }

  private SharedAssessmentSections buildSharedAssessmentSections(String assessmentType, boolean isCsf, DistrictAssessmentData data) {
    boolean isFrench = isFrenchAssessment(assessmentType);
    String districtLabel = isCsf ? DISTRICT_CSF : DISTRICT;
    String publicLabel = isCsf ? PUBLIC_CSF : ALL_PUBLIC;
    String provinceLabel = PROVINCE;

    var districtNumeracy = new NumeracyScore[1];
    var publicNumeracy = new NumeracyScore[1];
    var provinceNumeracy = new NumeracyScore[1];
    var districtComprehend = new ComprehendScore[1];
    var publicComprehend = new ComprehendScore[1];
    var provinceComprehend = new ComprehendScore[1];
    var districtCommunicate = new CommunicateScore[1];
    var publicCommunicate = new CommunicateScore[1];
    var provinceCommunicate = new CommunicateScore[1];
    var districtCommunicateOral = new CommunicateOralScore[1];
    var publicCommunicateOral = new CommunicateOralScore[1];
    var provinceCommunicateOral = new CommunicateOralScore[1];

    switch (assessmentType) {
      case NME10, NMF10 -> {
        districtNumeracy[0] = createNumeracySectionByLevel(districtLabel, data.districtLevelCalc(), isFrench);
        publicNumeracy[0] = createNumeracySectionByLevel(publicLabel, data.publicLevelCalc(), isFrench);
        provinceNumeracy[0] = createNumeracySectionByLevel(provinceLabel, data.provinceLevelCalc(), isFrench);
      }
      case LTE10, LTE12 -> {
        districtComprehend[0] = createComprehendSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench);
        publicComprehend[0] = createComprehendSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench);
        provinceComprehend[0] = createComprehendSectionByLevel(assessmentType, provinceLabel, data.provinceLevelCalc(), isFrench);
        districtCommunicate[0] = createCommunicateSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench);
        publicCommunicate[0] = createCommunicateSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench);
        provinceCommunicate[0] = createCommunicateSectionByLevel(assessmentType, provinceLabel, data.provinceLevelCalc(), isFrench);
      }
      case LTP10, LTP12, LTF12 -> {
        districtComprehend[0] = createComprehendSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench);
        publicComprehend[0] = createComprehendSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench);
        provinceComprehend[0] = createComprehendSectionByLevel(assessmentType, provinceLabel, data.provinceLevelCalc(), isFrench);
        districtCommunicate[0] = createCommunicateSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench);
        publicCommunicate[0] = createCommunicateSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench);
        provinceCommunicate[0] = createCommunicateSectionByLevel(assessmentType, provinceLabel, data.provinceLevelCalc(), isFrench);
        districtCommunicateOral[0] = createCommunicateOralSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench);
        publicCommunicateOral[0] = createCommunicateOralSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench);
        provinceCommunicateOral[0] = createCommunicateOralSectionByLevel(assessmentType, provinceLabel, data.provinceLevelCalc(), isFrench);
      }
      default -> log.info("Invalid assessment type");
    }

    return new SharedAssessmentSections(
            createProficiencyLevelSection(districtLabel, data.districtLevelStudents(), isFrench),
            createProficiencyLevelSection(publicLabel, data.publicLevelStudents(), isFrench),
            createProficiencyLevelSection(provinceLabel, data.provinceLevelStudents(), isFrench),
            createTaskSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench),
            createTaskSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench),
            createTaskSectionByLevel(assessmentType, provinceLabel, data.provinceLevelCalc(), isFrench),
            createCognitiveSectionByLevel(districtLabel, data.districtLevelCalc(), isFrench),
            createCognitiveSectionByLevel(publicLabel, data.publicLevelCalc(), isFrench),
            createCognitiveSectionByLevel(provinceLabel, data.provinceLevelCalc(), isFrench),
            districtNumeracy[0], publicNumeracy[0], provinceNumeracy[0],
            districtComprehend[0], publicComprehend[0], provinceComprehend[0],
            districtCommunicate[0], publicCommunicate[0], provinceCommunicate[0],
            districtCommunicateOral[0], publicCommunicateOral[0], provinceCommunicateOral[0]);
  }

  private void addDistrictSchoolPagesToNode(AssessmentSessionEntity session, Map<String, DistrictAssessmentData> dataByAssessment, Map<String, SharedAssessmentSections> sharedSectionsByAssessment, boolean canonicalIsCsf, SchoolTombstone school, DOARSummaryNode doarSummaryNode) {
    UUID schoolId = UUID.fromString(school.getSchoolId());
    boolean isCsf = school.getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode());
    boolean labelsMatch = isCsf == canonicalIsCsf;
    String schoolLabel = isCsf ? SCHOOL_CSF : SCHOOL;
    String districtLabel = isCsf ? DISTRICT_CSF : DISTRICT;
    String publicLabel = isCsf ? PUBLIC_CSF : ALL_PUBLIC;

    dataByAssessment.entrySet().stream()
      .sorted(Comparator.comparingInt(e -> AssessmentTypeCodes.sortOrderFor(e.getKey())))
      .forEach(entry -> {
        var assessmentType = entry.getKey();
        var data = entry.getValue();
        var shared = sharedSectionsByAssessment.get(assessmentType);
        var schoolStudents = data.studentsBySchool().get(schoolId);
        if(schoolStudents == null || schoolStudents.isEmpty()) {
          return;
        }
        boolean isFrench = isFrenchAssessment(assessmentType);
        var schoolCalc = calcForStudents(data.calcByStudentID(), schoolStudents);

        DOARSummaryPage doarSummaryPage = new DOARSummaryPage();
        setReportTombstoneValues(session, doarSummaryPage, assessmentType, school);
        doarSummaryPage.setProficiencySection(new ArrayList<>());
        doarSummaryPage.setTaskScore(new ArrayList<>());
        doarSummaryPage.setComprehendScore(new ArrayList<>());
        doarSummaryPage.setCommunicateScore(new ArrayList<>());
        doarSummaryPage.setCommunicateOralScore(new ArrayList<>());
        doarSummaryPage.setNumeracyScore(new ArrayList<>());
        doarSummaryPage.setCognitiveLevelScore(new ArrayList<>());

        doarSummaryPage.getProficiencySection().addAll(List.of(
                createProficiencyLevelSection(schoolLabel, schoolStudents, isFrench),
                useSharedOrCompute(labelsMatch, shared.districtProficiency(), () -> createProficiencyLevelSection(districtLabel, data.districtLevelStudents(), isFrench)),
                useSharedOrCompute(labelsMatch, shared.publicProficiency(), () -> createProficiencyLevelSection(publicLabel, data.publicLevelStudents(), isFrench)),
                shared.provinceProficiency()));

        doarSummaryPage.getTaskScore().addAll(List.of(
                createTaskSectionByLevel(assessmentType, schoolLabel, schoolCalc, isFrench),
                useSharedOrCompute(labelsMatch, shared.districtTask(), () -> createTaskSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench)),
                useSharedOrCompute(labelsMatch, shared.publicTask(), () -> createTaskSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench)),
                shared.provinceTask()));

        doarSummaryPage.getCognitiveLevelScore().addAll(List.of(
                createCognitiveSectionByLevel(schoolLabel, schoolCalc, isFrench),
                useSharedOrCompute(labelsMatch, shared.districtCognitive(), () -> createCognitiveSectionByLevel(districtLabel, data.districtLevelCalc(), isFrench)),
                useSharedOrCompute(labelsMatch, shared.publicCognitive(), () -> createCognitiveSectionByLevel(publicLabel, data.publicLevelCalc(), isFrench)),
                shared.provinceCognitive()));

        switch (assessmentType) {
          case NME10, NMF10 ->  {
            doarSummaryPage.getNumeracyScore().addAll(List.of(
                    createNumeracySectionByLevel(schoolLabel, schoolCalc, isFrench),
                    useSharedOrCompute(labelsMatch, shared.districtNumeracy(), () -> createNumeracySectionByLevel(districtLabel, data.districtLevelCalc(), isFrench)),
                    useSharedOrCompute(labelsMatch, shared.publicNumeracy(), () -> createNumeracySectionByLevel(publicLabel, data.publicLevelCalc(), isFrench)),
                    shared.provinceNumeracy()));
          }
          case LTE10, LTE12 -> {
            doarSummaryPage.getComprehendScore().addAll(List.of(
                    createComprehendSectionByLevel(assessmentType, schoolLabel, schoolCalc, isFrench),
                    useSharedOrCompute(labelsMatch, shared.districtComprehend(), () -> createComprehendSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench)),
                    useSharedOrCompute(labelsMatch, shared.publicComprehend(), () -> createComprehendSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench)),
                    shared.provinceComprehend()));

            doarSummaryPage.getCommunicateScore().addAll(List.of(
                    createCommunicateSectionByLevel(assessmentType, schoolLabel, schoolCalc, isFrench),
                    useSharedOrCompute(labelsMatch, shared.districtCommunicate(), () -> createCommunicateSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench)),
                    useSharedOrCompute(labelsMatch, shared.publicCommunicate(), () -> createCommunicateSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench)),
                    shared.provinceCommunicate()));
          }
          case LTP10, LTP12, LTF12 -> {
            doarSummaryPage.getComprehendScore().addAll(List.of(
                    createComprehendSectionByLevel(assessmentType, schoolLabel, schoolCalc, isFrench),
                    useSharedOrCompute(labelsMatch, shared.districtComprehend(), () -> createComprehendSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench)),
                    useSharedOrCompute(labelsMatch, shared.publicComprehend(), () -> createComprehendSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench)),
                    shared.provinceComprehend()));

            doarSummaryPage.getCommunicateScore().addAll(List.of(
                    createCommunicateSectionByLevel(assessmentType, schoolLabel, schoolCalc, isFrench),
                    useSharedOrCompute(labelsMatch, shared.districtCommunicate(), () -> createCommunicateSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench)),
                    useSharedOrCompute(labelsMatch, shared.publicCommunicate(), () -> createCommunicateSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench)),
                    shared.provinceCommunicate()));

            doarSummaryPage.getCommunicateOralScore().addAll(List.of(
                    createCommunicateOralSectionByLevel(assessmentType, schoolLabel, schoolCalc, isFrench),
                    useSharedOrCompute(labelsMatch, shared.districtCommunicateOral(), () -> createCommunicateOralSectionByLevel(assessmentType, districtLabel, data.districtLevelCalc(), isFrench)),
                    useSharedOrCompute(labelsMatch, shared.publicCommunicateOral(), () -> createCommunicateOralSectionByLevel(assessmentType, publicLabel, data.publicLevelCalc(), isFrench)),
                    shared.provinceCommunicateOral()));
          }
          default -> log.info("Invalid assessment type");
        }

        doarSummaryNode.getReports().add(doarSummaryPage);
    });
  }

  private <T> T useSharedOrCompute(boolean reuseShared, T sharedSection, java.util.function.Supplier<T> computeSection) {
    return reuseShared ? sharedSection : computeSection.get();
  }

  private boolean isFrenchAssessment(String assessmentType) {
    return assessmentType.equalsIgnoreCase(NMF10) || assessmentType.equalsIgnoreCase(LTP10) || assessmentType.equalsIgnoreCase(LTP12) || assessmentType.equalsIgnoreCase(LTF12);
  }

  private record DistrictAssessmentData(
          String assessmentType,
          Map<UUID, List<AssessmentStudentLightEntity>> studentsBySchool,
          Map<UUID, AssessmentStudentDOARCalculationEntity> calcByStudentID,
          List<AssessmentStudentLightEntity> districtLevelStudents,
          List<AssessmentStudentLightEntity> publicLevelStudents,
          List<AssessmentStudentLightEntity> provinceLevelStudents,
          List<AssessmentStudentDOARCalculationEntity> districtLevelCalc,
          List<AssessmentStudentDOARCalculationEntity> publicLevelCalc,
          List<AssessmentStudentDOARCalculationEntity> provinceLevelCalc) {
  }

  private record SharedAssessmentSections(
          ProficiencyLevel districtProficiency,
          ProficiencyLevel publicProficiency,
          ProficiencyLevel provinceProficiency,
          TaskScore districtTask,
          TaskScore publicTask,
          TaskScore provinceTask,
          CognitiveLevelScore districtCognitive,
          CognitiveLevelScore publicCognitive,
          CognitiveLevelScore provinceCognitive,
          NumeracyScore districtNumeracy,
          NumeracyScore publicNumeracy,
          NumeracyScore provinceNumeracy,
          ComprehendScore districtComprehend,
          ComprehendScore publicComprehend,
          ComprehendScore provinceComprehend,
          CommunicateScore districtCommunicate,
          CommunicateScore publicCommunicate,
          CommunicateScore provinceCommunicate,
          CommunicateOralScore districtCommunicateOral,
          CommunicateOralScore publicCommunicateOral,
          CommunicateOralScore provinceCommunicateOral) {
  }

  private void addSchoolPagesToNode(AssessmentSessionEntity session, HashMap<String, List<AssessmentStudentLightEntity>> studentsByAssessment, SchoolTombstone school, DOARSummaryNode doarSummaryNode) {
    boolean isIndependent = school.getIndependentAuthorityId() != null;
    studentsByAssessment.entrySet().stream()
      .sorted(Comparator.comparingInt(e -> AssessmentTypeCodes.sortOrderFor(e.getKey())))
      .forEach(entry -> {
        var studentList = entry.getValue();
        var assessmentType = entry.getKey();
        boolean schoolHasResult = studentList.stream().anyMatch(student -> Objects.equals(student.getSchoolAtWriteSchoolID(), UUID.fromString(school.getSchoolId())));
        if(!studentList.isEmpty() && schoolHasResult) {
          DOARSummaryPage doarSummaryPage = new DOARSummaryPage();
          setReportTombstoneValues(session, doarSummaryPage, assessmentType, school);
          doarSummaryPage.setProficiencySection(new ArrayList<>());
          doarSummaryPage.setTaskScore(new ArrayList<>());
          doarSummaryPage.setComprehendScore(new ArrayList<>());
          doarSummaryPage.setCommunicateScore(new ArrayList<>());
          doarSummaryPage.setCommunicateOralScore(new ArrayList<>());
          doarSummaryPage.setNumeracyScore(new ArrayList<>());
          doarSummaryPage.setCognitiveLevelScore(new ArrayList<>());

          setProficiencyLevels(studentList, isIndependent, school, doarSummaryPage, assessmentType);
          setAssessmentRawScores(studentList, isIndependent, school, doarSummaryPage, assessmentType);

          doarSummaryNode.getReports().add(doarSummaryPage);
        }
    });
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

    if(school.getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode())) {
      reportNode.setReportGeneratedDate("Rapport généré le : " + LocalDate.now().format(formatter));
    } else {
      reportNode.setReportGeneratedDate("Report Generated: " + LocalDate.now().format(formatter));
    }

    reportNode.setSessionDetail(assessmentSession.getCourseYear() + "/" + assessmentSession.getCourseMonth() + " Session");
    reportNode.setSchoolMincodeAndName(school.getMincode() + " - " + school.getDisplayName());
    reportNode.setAssessmentType(assessmentType);
    reportNode.setReportId(UUID.randomUUID().toString());
    reportNode.setReportTitle(getReportTitle(assessmentType));
    reportNode.setCSF(school.getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode()));
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
            filterDOARCalcByStudentIds(doarCalcMap, studentIdsByLevel.get(PROVINCE));

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
              publicLevelDOARCalc, provinceLevelDOARCalc, doarSummaryPage, assessmentType, school);
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

    Set<UUID> provinceSchoolIds = getSchoolsByLevel(PROVINCE, null).stream()
            .map(s -> UUID.fromString(s.getSchoolId()))
            .collect(Collectors.toSet());

    Set<UUID> schoolLevel = new HashSet<>();
    Set<UUID> districtLevel = new HashSet<>();
    Set<UUID> publicLevel = new HashSet<>();
    Set<UUID> independentLevel = new HashSet<>();
    Set<UUID> provinceLevel = new HashSet<>();

    for (AssessmentStudentLightEntity student : students) {
      UUID studentSchoolId = student.getSchoolAtWriteSchoolID();
      UUID studentId = student.getAssessmentStudentID();

      if (provinceSchoolIds.contains(studentSchoolId)) {
        provinceLevel.add(studentId);
      }

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
    result.put(PROVINCE, provinceLevel);

    return result;
  }

  private void populateRawScoresForIndependentSchools(List<AssessmentStudentDOARCalculationEntity> schoolLevel, List<AssessmentStudentDOARCalculationEntity> provinceLevel, List<AssessmentStudentDOARCalculationEntity> indpLevel, DOARSummaryPage doarSummaryPage, String assessmentType) {
    boolean isFrenchAssessment = assessmentType.equalsIgnoreCase(NMF10) || assessmentType.equalsIgnoreCase(LTP10) || assessmentType.equalsIgnoreCase(LTP12) || assessmentType.equalsIgnoreCase(LTF12);

    List<TaskScore> taskScores = List.of(createTaskSectionByLevel(assessmentType, SCHOOL, schoolLevel, isFrenchAssessment),
            createTaskSectionByLevel(assessmentType, ALL_INDP, indpLevel, isFrenchAssessment), createTaskSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
    doarSummaryPage.getTaskScore().addAll(taskScores);

    List<CognitiveLevelScore> cognScore = List.of(createCognitiveSectionByLevel(SCHOOL, schoolLevel, isFrenchAssessment),
            createCognitiveSectionByLevel(ALL_INDP, indpLevel, isFrenchAssessment), createCognitiveSectionByLevel(PROVINCE, provinceLevel, isFrenchAssessment));
    doarSummaryPage.getCognitiveLevelScore().addAll(cognScore);

    switch (assessmentType) {
      case NME10, NMF10 ->  {
        List<NumeracyScore> numScores = List.of(createNumeracySectionByLevel(SCHOOL, schoolLevel, isFrenchAssessment),
                createNumeracySectionByLevel(ALL_INDP, indpLevel, isFrenchAssessment), createNumeracySectionByLevel(PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getNumeracyScore().addAll(numScores);
      }
      case LTE10, LTE12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, SCHOOL, schoolLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, ALL_INDP, indpLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, SCHOOL, schoolLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, ALL_INDP, indpLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);
      }
      case LTP10, LTP12, LTF12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, SCHOOL, schoolLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, ALL_INDP, indpLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, SCHOOL, schoolLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, ALL_INDP, indpLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);

        List<CommunicateOralScore> communicateOralScores = List.of(createCommunicateOralSectionByLevel(assessmentType, SCHOOL, schoolLevel, isFrenchAssessment),
                createCommunicateOralSectionByLevel(assessmentType, ALL_INDP, indpLevel, isFrenchAssessment),
                createCommunicateOralSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getCommunicateOralScore().addAll(communicateOralScores);
      }
      default -> log.info("Invalid assessment type");
    }
  }

  private void populateRawScoresForPublicSchools(List<AssessmentStudentDOARCalculationEntity> schoolLevel, List<AssessmentStudentDOARCalculationEntity> districtLevel, List<AssessmentStudentDOARCalculationEntity> publicLevel, List<AssessmentStudentDOARCalculationEntity> provinceLevel, DOARSummaryPage doarSummaryPage, String assessmentType, SchoolTombstone school) {
    boolean isCsf = school.getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode());
    boolean isFrenchAssessment = assessmentType.equalsIgnoreCase(NMF10) || assessmentType.equalsIgnoreCase(LTP10) || assessmentType.equalsIgnoreCase(LTP12) || assessmentType.equalsIgnoreCase(LTF12);
    String schoolLabel = isCsf ? SCHOOL_CSF : SCHOOL;
    String districtLabel = isCsf ? DISTRICT_CSF : DISTRICT;
    String publicLabel = isCsf ? PUBLIC_CSF : ALL_PUBLIC;
    List<TaskScore> taskScores = List.of(createTaskSectionByLevel(assessmentType, schoolLabel, schoolLevel, isFrenchAssessment),
            createTaskSectionByLevel(assessmentType, districtLabel, districtLevel, isFrenchAssessment),
            createTaskSectionByLevel(assessmentType, publicLabel, publicLevel, isFrenchAssessment), createTaskSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
    doarSummaryPage.getTaskScore().addAll(taskScores);

    List<CognitiveLevelScore> cognScore = List.of(createCognitiveSectionByLevel(schoolLabel, schoolLevel, isFrenchAssessment),
            createCognitiveSectionByLevel(districtLabel, districtLevel, isFrenchAssessment),
            createCognitiveSectionByLevel(publicLabel, publicLevel, isFrenchAssessment), createCognitiveSectionByLevel(PROVINCE, provinceLevel, isFrenchAssessment));
    doarSummaryPage.getCognitiveLevelScore().addAll(cognScore);

    switch (assessmentType) {
      case NME10, NMF10 ->  {
        List<NumeracyScore> numScores = List.of(createNumeracySectionByLevel(schoolLabel, schoolLevel, isFrenchAssessment),
                createNumeracySectionByLevel(districtLabel, districtLevel, isFrenchAssessment),
                createNumeracySectionByLevel(publicLabel, publicLevel, isFrenchAssessment), createNumeracySectionByLevel(PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getNumeracyScore().addAll(numScores);
      }
      case LTE10, LTE12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, schoolLabel, schoolLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, districtLabel, districtLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, publicLabel, publicLevel, isFrenchAssessment), createComprehendSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, schoolLabel, schoolLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, districtLabel, districtLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, publicLabel, publicLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);
      }
      case LTP10, LTP12, LTF12 -> {
        List<ComprehendScore> comprehendScores = List.of(createComprehendSectionByLevel(assessmentType, schoolLabel, schoolLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, districtLabel, districtLevel, isFrenchAssessment),
                createComprehendSectionByLevel(assessmentType, publicLabel, publicLevel, isFrenchAssessment), createComprehendSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getComprehendScore().addAll(comprehendScores);

        List<CommunicateScore> communicateScores = List.of(createCommunicateSectionByLevel(assessmentType, schoolLabel, schoolLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, districtLabel, districtLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, publicLabel, publicLevel, isFrenchAssessment),
                createCommunicateSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getCommunicateScore().addAll(communicateScores);

        List<CommunicateOralScore> communicateOralScores = List.of(createCommunicateOralSectionByLevel(assessmentType, schoolLabel, schoolLevel, isFrenchAssessment),
                createCommunicateOralSectionByLevel(assessmentType, districtLabel, districtLevel, isFrenchAssessment),
                createCommunicateOralSectionByLevel(assessmentType, publicLabel, publicLevel, isFrenchAssessment),
                createCommunicateOralSectionByLevel(assessmentType, PROVINCE, provinceLevel, isFrenchAssessment));
        doarSummaryPage.getCommunicateOralScore().addAll(communicateOralScores);
      }
      default -> log.info("Invalid assessment type");
    }
  }

  private TaskScore createTaskSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc, boolean isFrenchAssessment) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new TaskScore();
    }
    TaskScore score = new TaskScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case NME10, NMF10 ->  {
        BigDecimal taskPlanSumAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskPlan).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskEstimateSumAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskEstimate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskFairSumAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskFair).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskModelSumAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskModel).reduce(BigDecimal.ZERO, BigDecimal::add);

        String taskPlanSum = String.valueOf(taskPlanSumAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String taskEstimateSum = String.valueOf(taskEstimateSumAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String taskFairSum = String.valueOf(taskFairSumAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String taskModelSum = String.valueOf(taskModelSumAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setTaskPlan(isFrenchAssessment ? replacePeriodsWithCommas(taskPlanSum) : taskPlanSum);
        score.setTaskEstimate(isFrenchAssessment ? replacePeriodsWithCommas(taskEstimateSum) : taskEstimateSum);
        score.setTaskFair(isFrenchAssessment ? replacePeriodsWithCommas(taskFairSum) : taskFairSum);
        score.setTaskModel(isFrenchAssessment ? replacePeriodsWithCommas(taskModelSum) : taskModelSum);

        yield score;
      }
      case LTE10, LTE12 -> {
        BigDecimal taskCompreAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskCommAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);

        String taskCompre = String.valueOf(taskCompreAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String taskComm = String.valueOf(taskCommAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setTaskComprehend(isFrenchAssessment ? replacePeriodsWithCommas(taskCompre) : taskCompre);
        score.setTaskCommunicate(isFrenchAssessment ? replacePeriodsWithCommas(taskComm) : taskComm);

        yield score;
      }
      case LTP10, LTP12, LTF12 -> {
        BigDecimal taskCompreAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskComprehend).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskCommAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskCommunicate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskCommOralAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getTaskOral).reduce(BigDecimal.ZERO, BigDecimal::add);

        String taskCompre = String.valueOf(taskCompreAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String taskComm = String.valueOf(taskCommAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String taskCommOral = String.valueOf(taskCommOralAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setTaskComprehend(isFrenchAssessment ? replacePeriodsWithCommas(taskCompre) : taskCompre);
        score.setTaskCommunicate(isFrenchAssessment ? replacePeriodsWithCommas(taskComm) : taskComm);
        score.setTaskOral(isFrenchAssessment ? replacePeriodsWithCommas(taskCommOral) : taskCommOral);

        yield score;
      }
     default -> score;
    };
  }

  private ComprehendScore createComprehendSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc, boolean isFrenchAssessment) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new ComprehendScore();
    }
    ComprehendScore score = new ComprehendScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case LTE10, LTE12, LTP10, LTP12 -> {
        BigDecimal taskCompreAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartA).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taskCommAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartB).reduce(BigDecimal.ZERO, BigDecimal::add);

        String taskCompre = String.valueOf(taskCompreAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String taskComm = String.valueOf(taskCommAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setComprehendPartA(isFrenchAssessment ? replacePeriodsWithCommas(taskCompre) : taskCompre);
        score.setComprehendPartB(isFrenchAssessment ? replacePeriodsWithCommas(taskComm) : taskComm);
        yield score;
      }
      case LTF12 -> {
        BigDecimal taskCompreAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartATask).reduce(BigDecimal.ZERO, BigDecimal::add);

        var filteredStudentsForInfo = listOfDOARCalc.stream().filter(calc ->
                calc.getSelectedResponseChoicePath() != null && calc.getSelectedResponseChoicePath().equalsIgnoreCase("I")).toList();
        BigDecimal infoCompreAdd = filteredStudentsForInfo.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartBInfo).reduce(BigDecimal.ZERO, BigDecimal::add);

        var filteredStudentsForExp = listOfDOARCalc.stream().filter(calc ->
                calc.getSelectedResponseChoicePath() != null && calc.getSelectedResponseChoicePath().equalsIgnoreCase("E")).toList();
        BigDecimal expCompreAdd = filteredStudentsForExp.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartBExp).reduce(BigDecimal.ZERO, BigDecimal::add);

        String taskCompre = String.valueOf(taskCompreAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String infoCompre = filteredStudentsForInfo.isEmpty() ? "0.00"
                :String.valueOf(infoCompreAdd.divide(new BigDecimal(filteredStudentsForInfo.size()), 2, RoundingMode.DOWN));
        String expCompre = filteredStudentsForExp.isEmpty() ? "0.00"
                :String.valueOf(expCompreAdd.divide(new BigDecimal(filteredStudentsForExp.size()), 2, RoundingMode.DOWN));

        score.setComprehendPartATask(isFrenchAssessment ? replacePeriodsWithCommas(taskCompre) : taskCompre);
        score.setComprehendPartBInfo(isFrenchAssessment ? replacePeriodsWithCommas(infoCompre) : infoCompre);
        score.setComprehendPartBExp(isFrenchAssessment ? replacePeriodsWithCommas(expCompre) : expCompre);
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateScore createCommunicateSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc, boolean isFrenchAssessment) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateScore();
    }
    CommunicateScore score = new CommunicateScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case LTE10, LTE12, LTP12 -> {
        BigDecimal commGOAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRAAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRBAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        String commGO = String.valueOf(commGOAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String commWRA = String.valueOf(commWRAAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String commWRB = String.valueOf(commWRBAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setCommunicateGraphicOrg(isFrenchAssessment ? replacePeriodsWithCommas(commGO) : commGO);
        score.setCommunicateUnderstanding(isFrenchAssessment ? replacePeriodsWithCommas(commWRA) : commWRA);
        score.setCommunicatePersonalConn(isFrenchAssessment ? replacePeriodsWithCommas(commWRB) : commWRB);
        yield score;
      }
      case LTP10 -> {
        BigDecimal comprePartAAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commGOAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateGraphicOrg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRAAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateUnderstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commWRBAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicatePersonalConn).reduce(BigDecimal.ZERO, BigDecimal::add);

        String comprePartA = String.valueOf(comprePartAAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String commGO = String.valueOf(commGOAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String commWRA = String.valueOf(commWRAAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String commWRB = String.valueOf(commWRBAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setCommunicateGraphicOrg(isFrenchAssessment ? replacePeriodsWithCommas(commGO) : commGO);
        score.setCommunicateUnderstanding(isFrenchAssessment ? replacePeriodsWithCommas(commWRA): commWRA);
        score.setCommunicatePersonalConn(isFrenchAssessment ? replacePeriodsWithCommas(commWRB): commWRB);
        score.setComprehendPartAShort(isFrenchAssessment ? replacePeriodsWithCommas(comprePartA): comprePartA);
        yield score;
      }
      case LTF12 -> {
        BigDecimal backgroundAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDissertationBackground).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal formAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDissertationForm).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comprePartAAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getComprehendPartAShort).reduce(BigDecimal.ZERO, BigDecimal::add);

        String background = String.valueOf(backgroundAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String form = String.valueOf(formAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String comprePartA = String.valueOf(comprePartAAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));


        score.setComprehendPartAShort(isFrenchAssessment ? replacePeriodsWithCommas(comprePartA) : comprePartA);
        score.setDissertationBackground(isFrenchAssessment ? replacePeriodsWithCommas(background) : background);
        score.setDissertationForm(isFrenchAssessment ? replacePeriodsWithCommas(form) : form);
        yield score;
      }
      default -> score;
    };
  }

  private CommunicateOralScore createCommunicateOralSectionByLevel(String assessmentType, String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc, boolean isFrenchAssessment) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CommunicateOralScore();
    }
    CommunicateOralScore score = new CommunicateOralScore();
    score.setLevel(level);

    return switch (assessmentType) {
      case LTP12, LTP10 -> {
        BigDecimal part1Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart3).reduce(BigDecimal.ZERO, BigDecimal::add);

        String part1 = String.valueOf(part1Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String part2 = String.valueOf(part2Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String part3 = String.valueOf(part3Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setCommunicateOralPart1(isFrenchAssessment ? replacePeriodsWithCommas(part1) : part1);
        score.setCommunicateOralPart2(isFrenchAssessment ? replacePeriodsWithCommas(part2) : part2);
        score.setCommunicateOralPart3(isFrenchAssessment ? replacePeriodsWithCommas(part3) : part3);
        yield score;
      }
      case LTF12 -> {
        BigDecimal part1Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part2Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Form).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part3Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart1Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part4Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Expression).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part5Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Background).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal part6Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getCommunicateOralPart2Form).reduce(BigDecimal.ZERO, BigDecimal::add);

        String part1 = String.valueOf(part1Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String part2 = String.valueOf(part2Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String part3 = String.valueOf(part3Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String part4 = String.valueOf(part4Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String part5 = String.valueOf(part5Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
        String part6 = String.valueOf(part6Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

        score.setCommunicateOralPart1Background(isFrenchAssessment ? replacePeriodsWithCommas(part1) : part1);
        score.setCommunicateOralPart1Form(isFrenchAssessment ? replacePeriodsWithCommas(part2) : part2);
        score.setCommunicateOralPart1Expression(isFrenchAssessment ? replacePeriodsWithCommas(part3) : part3);
        score.setCommunicateOralPart2Expression(isFrenchAssessment ? replacePeriodsWithCommas(part4) : part4);
        score.setCommunicateOralPart2Background(isFrenchAssessment ? replacePeriodsWithCommas(part5) : part5);
        score.setCommunicateOralPart2Form(isFrenchAssessment ? replacePeriodsWithCommas(part6) : part6);
        yield score;
      }
      default -> score;
    };
  }

  private CognitiveLevelScore createCognitiveSectionByLevel(String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc, boolean isFrenchAssessment) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new CognitiveLevelScore();
    }
    CognitiveLevelScore score = new CognitiveLevelScore();
    score.setLevel(level);
    BigDecimal dok1Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok1).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok2Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok2).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal dok3Add = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getDok3).reduce(BigDecimal.ZERO, BigDecimal::add);

    String dok1 = String.valueOf(dok1Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
    String dok2 = String.valueOf(dok2Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
    String dok3 = String.valueOf(dok3Add.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

    score.setDok1(isFrenchAssessment ? replacePeriodsWithCommas(dok1) : dok1);
    score.setDok2(isFrenchAssessment ? replacePeriodsWithCommas(dok2) : dok2);
    score.setDok3(isFrenchAssessment ? replacePeriodsWithCommas(dok3) : dok3);
    return score;
  }

  private NumeracyScore createNumeracySectionByLevel(String level, List<AssessmentStudentDOARCalculationEntity> listOfDOARCalc, boolean isFrenchAssessment) {
    int noOfStudents = listOfDOARCalc.size();
    if (noOfStudents == 0) {
      return new NumeracyScore();
    }
    NumeracyScore score = new NumeracyScore();
    score.setLevel(level);
    BigDecimal interpretAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyInterpret).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal applyAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyApply).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal solveAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracySolve).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal analyzeAdd = listOfDOARCalc.stream().map(AssessmentStudentDOARCalculationEntity::getNumeracyAnalyze).reduce(BigDecimal.ZERO, BigDecimal::add);

    String interpret = String.valueOf(interpretAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
    String apply = String.valueOf(applyAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
    String solve = String.valueOf(solveAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));
    String analyze = String.valueOf(analyzeAdd.divide(new BigDecimal(noOfStudents), 2, RoundingMode.DOWN));

    score.setNumeracyInterpret(isFrenchAssessment ? replacePeriodsWithCommas(interpret) : interpret);
    score.setNumeracyApply(isFrenchAssessment ? replacePeriodsWithCommas(apply) : apply);
    score.setNumeracySolve(isFrenchAssessment ? replacePeriodsWithCommas(solve) : solve);
    score.setNumeracyAnalyze(isFrenchAssessment ? replacePeriodsWithCommas(analyze) : analyze);
    return score;
  }

  private void setProficiencyLevels(List<AssessmentStudentLightEntity> students, boolean isIndependent, SchoolTombstone school, DOARSummaryPage doarSummaryPage, String assessmentType) {
    boolean isCsf = school.getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode());
    boolean isFrenchAssessment = assessmentType.equalsIgnoreCase(NMF10) || assessmentType.equalsIgnoreCase(LTP10) || assessmentType.equalsIgnoreCase(LTP12) || assessmentType.equalsIgnoreCase(LTF12);
    Map<String, List<AssessmentStudentLightEntity>> studentsByLevel =
            categorizeStudentsEntitiesByLevel(students, school, isIndependent);

    var provinceLevel = createProficiencyLevelSection(PROVINCE , studentsByLevel.get(PROVINCE), isFrenchAssessment);
    if(isIndependent) {
      doarSummaryPage.getProficiencySection().addAll(List.of(
              createProficiencyLevelSection(SCHOOL, studentsByLevel.get(SCHOOL), isFrenchAssessment),
              createProficiencyLevelSection(ALL_INDP, studentsByLevel.get(INDEPENDENT), isFrenchAssessment),
              provinceLevel
      ));
    } else {
      doarSummaryPage.getProficiencySection().addAll(List.of(
              createProficiencyLevelSection(isCsf ? SCHOOL_CSF : SCHOOL, studentsByLevel.get(SCHOOL), isFrenchAssessment),
              createProficiencyLevelSection(isCsf ? DISTRICT_CSF : DISTRICT, studentsByLevel.get(DISTRICT), isFrenchAssessment),
              createProficiencyLevelSection(isCsf ? PUBLIC_CSF : ALL_PUBLIC, studentsByLevel.get(PUBLIC), isFrenchAssessment),
              provinceLevel
      ));
    }
  }

  private Map<String, List<AssessmentStudentLightEntity>> categorizeStudentsEntitiesByLevel(
          List<AssessmentStudentLightEntity> students, SchoolTombstone school, boolean isIndependent) {

    Map<String, List<AssessmentStudentLightEntity>> result = new HashMap<>();
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

    Set<UUID> provinceSchoolIds = getSchoolsByLevel(PROVINCE, null).stream()
            .map(s -> UUID.fromString(s.getSchoolId()))
            .collect(Collectors.toSet());

    // Single pass through students
    List<AssessmentStudentLightEntity> schoolLevel = new ArrayList<>();
    List<AssessmentStudentLightEntity> districtLevel = new ArrayList<>();
    List<AssessmentStudentLightEntity> publicLevel = new ArrayList<>();
    List<AssessmentStudentLightEntity> independentLevel = new ArrayList<>();
    List<AssessmentStudentLightEntity> provinceLevel = new ArrayList<>();

    for (AssessmentStudentLightEntity student : students) {
      UUID studentSchoolId = student.getSchoolAtWriteSchoolID();

      if (schoolId.equals(studentSchoolId)) {
        schoolLevel.add(student);
      }
      if (districtSchoolIds.contains(studentSchoolId)) {
        districtLevel.add(student);
      }
      if (publicSchoolIds.contains(studentSchoolId)) {
        publicLevel.add(student);
      }
      if (independentSchoolIds.contains(studentSchoolId)) {
        independentLevel.add(student);
      }
      if (provinceSchoolIds.contains(studentSchoolId)) {
        provinceLevel.add(student);
      }
    }

    result.put(SCHOOL, schoolLevel);
    result.put(DISTRICT, districtLevel);
    result.put(PUBLIC, publicLevel);
    result.put(INDEPENDENT, independentLevel);
    result.put(PROVINCE, provinceLevel);

    return result;
  }

  private ProficiencyLevel createProficiencyLevelSection(String level, List<AssessmentStudentLightEntity> students, boolean isFrenchAssessment) {
    int totalCount = students.size();
    ProficiencyLevel schoolProficiencyLevel =  new ProficiencyLevel();
    schoolProficiencyLevel.setLevel(level);
    schoolProficiencyLevel.setNumberCounted(String.valueOf(totalCount));
    schoolProficiencyLevel.setStudentsWithProficiency1(getProficiencyScorePercent(students, 1, totalCount, isFrenchAssessment));
    schoolProficiencyLevel.setStudentsWithProficiency2(getProficiencyScorePercent(students, 2, totalCount, isFrenchAssessment));
    schoolProficiencyLevel.setStudentsWithProficiency3(getProficiencyScorePercent(students, 3, totalCount, isFrenchAssessment));
    schoolProficiencyLevel.setStudentsWithProficiency4(getProficiencyScorePercent(students, 4, totalCount, isFrenchAssessment));
    schoolProficiencyLevel.setNotCounted(getSpecialCasePercent(students, ProvincialSpecialCaseCodes.NOTCOMPLETED.getCode(), totalCount, isFrenchAssessment));

    return schoolProficiencyLevel;
  }

  private String getSpecialCasePercent(List<AssessmentStudentLightEntity> students, String caseCode, int count, boolean isFrenchAssessment) {
    if(count == 0) {
      return String.valueOf(BigDecimal.ZERO);
    }
    var studentsWithProfScore = students.stream().filter(student -> StringUtils.isNotBlank(student.getProvincialSpecialCaseCode()) && student.getProvincialSpecialCaseCode().equalsIgnoreCase(caseCode)).toList();
    if(studentsWithProfScore.isEmpty()) {
      return String.valueOf(BigDecimal.ZERO);
    }

    var ncScore = new BigDecimal(studentsWithProfScore.size()).multiply(new BigDecimal(100)).divide(new BigDecimal(count), 2, RoundingMode.DOWN);
    return isFrenchAssessment ? replacePeriodsWithCommas(String.valueOf(ncScore)) : String.valueOf(ncScore);
  }

  private String replacePeriodsWithCommas(String s){
    if(StringUtils.isNotBlank(s)){
      return s.replace(".", ",");
    }
    return s;
  }

  private String getProficiencyScorePercent(List<AssessmentStudentLightEntity> students, int score, int count, boolean isFrenchAssessment) {
    if(count == 0) {
      return String.valueOf(BigDecimal.ZERO);
    }
    var studentsWithProfScore = students.stream().filter(student -> student.getProficiencyScore() != null && student.getProficiencyScore() == score).toList();
    if(studentsWithProfScore.isEmpty()) {
      return String.valueOf(BigDecimal.ZERO);
    }

    var profScore = new BigDecimal(studentsWithProfScore.size()).multiply(new BigDecimal(100)).divide(new BigDecimal(count), 2, RoundingMode.DOWN);
    return isFrenchAssessment ? replacePeriodsWithCommas(String.valueOf(profScore)) : String.valueOf(profScore);
  }

  private List<SchoolTombstone> getSchoolsByLevel(String level, String districtOrAuthorityID) {
    var schools = restUtils.getAllSchoolTombstones();
    return switch (level) {
      case DISTRICT -> schools.stream().filter(school -> school.getDistrictId().equals(districtOrAuthorityID) && StringUtils.isBlank(school.getIndependentAuthorityId())
              && (school.getSchoolCategoryCode().equalsIgnoreCase(PUBLIC) || school.getSchoolCategoryCode().equalsIgnoreCase(YUKON.getCode()))).toList();
      case INDEPENDENT -> schools.stream().filter(school -> StringUtils.isNotBlank(school.getIndependentAuthorityId()) && (school.getSchoolCategoryCode().equalsIgnoreCase(INDEPEND.getCode())
              ||  school.getSchoolCategoryCode().equalsIgnoreCase(INDP_FNS.getCode()))).toList();
      case PUBLIC -> schools.stream().filter(school -> school.getSchoolCategoryCode().equalsIgnoreCase(PUBLIC)
              || school.getSchoolCategoryCode().equalsIgnoreCase(YUKON.getCode())).toList();
      case PROVINCE -> schools.stream().filter(school -> school.getSchoolCategoryCode().equalsIgnoreCase(PUBLIC)
              || school.getSchoolCategoryCode().equalsIgnoreCase(YUKON.getCode()) ||  school.getSchoolCategoryCode().equalsIgnoreCase(INDEPEND.getCode())
              ||  school.getSchoolCategoryCode().equalsIgnoreCase(INDP_FNS.getCode()) || school.getSchoolCategoryCode().equalsIgnoreCase(OFFSHORE.getCode())).toList();

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
