package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.PreconditionRequiredException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.inSession.SchoolStudentGradAssessmentNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.inSession.SchoolStudentNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.inSession.SchoolStudentReportNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.inSession.SchoolStudentRootNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service class for generating School Students in Session Report
 */
@Service
@Slf4j
public class SchoolStudentsInSessionReportService extends BaseReportGenerationService {

  private final AssessmentSessionRepository assessmentSessionRepository;
  private final AssessmentStudentRepository assessmentStudentRepository;
  private final RestUtils restUtils;
  private final CodeTableService codeTableService;
  private JasperReport schoolStudentInSessionReport;

  public SchoolStudentsInSessionReportService(AssessmentSessionRepository assessmentSessionRepository, AssessmentStudentRepository assessmentStudentRepository, RestUtils restUtils, CodeTableService codeTableService) {
    super(restUtils);
    this.assessmentSessionRepository = assessmentSessionRepository;
    this.assessmentStudentRepository = assessmentStudentRepository;
    this.restUtils = restUtils;
    this.codeTableService = codeTableService;
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
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/schoolStudentsInSession.jrxml");
      schoolStudentInSessionReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentAssessmentAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }
  
  public ResponseEntity<InputStreamResource> generateReportForRandomSetOfSchoolsInSession(UUID assessmentSessionID){
    var schoolsInSession = assessmentStudentRepository.getSchoolIDsOfSchoolsWithMoreThanStudentsInSession(assessmentSessionID);
    var schools = getRandomUUIDs(schoolsInSession, 20);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {

      for (UUID school : schools) {
        DownloadableReportResponse report = generateSchoolStudentsInSessionReport(assessmentSessionID, school);
        var schoolDetail = restUtils.getSchoolBySchoolID(school.toString());

        ZipEntry zipEntry = new ZipEntry("SchoolStudentsInSession - " + schoolDetail.get().getMincode() + ".pdf"); // e.g., "school-report-123.pdf"
        zos.putNextEntry(zipEntry);
        zos.write(report.getDocumentData().getBytes()); // assuming this returns byte[]
        zos.closeEntry();
      }
    } catch (IOException e) {
        throw new StudentAssessmentAPIRuntimeException(e);
    }

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    InputStreamResource resource = new InputStreamResource(bais);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reports.zip\"")
            .header(HttpHeaders.CONTENT_TYPE, "application/zip")
            .body(resource);
  }

  public static List<UUID> getRandomUUIDs(List<UUID> uuids, int count) {
    List<UUID> copy = new ArrayList<>(uuids);
    Collections.shuffle(copy);
    return copy.stream().limit(count).toList();
  }

  public DownloadableReportResponse generateSchoolStudentsInSessionReport(UUID assessmentSessionID, UUID schoolID){
    try {
      var assessmentTypes = codeTableService.getAllAssessmentTypeCodesAsMap();
      var session = assessmentSessionRepository.findById(assessmentSessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", assessmentSessionID.toString()));
      var students = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(assessmentSessionID, schoolID, List.of("ACTIVE"));

      if(students.isEmpty()) {
        throw new PreconditionRequiredException(AssessmentSessionEntity.class, "Results not available in this session:: ", session.getSessionID().toString());
      }

      SchoolStudentRootNode schoolStudentRootNode = new SchoolStudentRootNode();
      SchoolStudentReportNode schoolStudentReportNode = new SchoolStudentReportNode();
      schoolStudentReportNode.setStudents(new ArrayList<>());
      schoolStudentRootNode.setReport(schoolStudentReportNode);
      setReportTombstoneValues(schoolID, session, schoolStudentReportNode);

      var studentList = new HashMap<UUID, SchoolStudentNode>();

      students.forEach(student -> {
        if (!studentList.containsKey(student.getStudentID())) {
          var studentNode = new SchoolStudentNode();
          studentNode.setPen(student.getPen());
          studentNode.setLocalID(student.getLocalID());
          studentNode.setName(student.getSurname() + ", " + student.getGivenName());
          SchoolStudentGradAssessmentNode studentGradAssessmentNode = new SchoolStudentGradAssessmentNode();
          studentGradAssessmentNode.setName(assessmentTypes.get(student.getAssessmentEntity().getAssessmentTypeCode()));
          studentGradAssessmentNode.setProficiencyScore(student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : null);
          studentGradAssessmentNode.setSpecialCase(student.getProficiencyScore() == null && StringUtils.isNotBlank(student.getProvincialSpecialCaseCode()) ? ProvincialSpecialCaseCodes.findByValue(student.getProvincialSpecialCaseCode()).get().getDescription() : null);
          studentNode.setGradAssessments(new ArrayList<>());
          studentNode.getGradAssessments().add(studentGradAssessmentNode);
          studentList.put(student.getStudentID(), studentNode);
        }else{
          var loadedStudent = studentList.get(student.getStudentID());
          SchoolStudentGradAssessmentNode studentGradAssessmentNode = new SchoolStudentGradAssessmentNode();
          studentGradAssessmentNode.setName(assessmentTypes.get(student.getAssessmentEntity().getAssessmentTypeCode()));
          studentGradAssessmentNode.setProficiencyScore(student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : null);
          studentGradAssessmentNode.setSpecialCase(student.getProficiencyScore() == null && StringUtils.isNotBlank(student.getProvincialSpecialCaseCode()) ? ProvincialSpecialCaseCodes.findByValue(student.getProvincialSpecialCaseCode()).get().getDescription() : null);
          loadedStudent.getGradAssessments().add(studentGradAssessmentNode);
        }
      });

      schoolStudentReportNode.setStudents(studentList.values().stream().sorted(Comparator.comparing(SchoolStudentNode::getName)).toList());

      return generateJasperReport(objectWriter.writeValueAsString(schoolStudentRootNode), schoolStudentInSessionReport, AssessmentReportTypeCode.SCHOOL_STUDENTS_IN_SESSION.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
    }
  }

  protected SchoolTombstone setReportTombstoneValues(UUID schoolID, AssessmentSessionEntity assessmentSession, SchoolStudentReportNode reportNode){
    var school = validateAndReturnSchool(schoolID);

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
    reportNode.setCSF(school.getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode()));

    return school;
  }
}
