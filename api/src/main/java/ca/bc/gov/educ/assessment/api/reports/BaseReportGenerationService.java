package ca.bc.gov.educ.assessment.api.reports;


import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.IndependentAuthority;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.inSession.SchoolStudentReportNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public abstract class BaseReportGenerationService {

  private final RestUtils restUtils;

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  protected static final String FALSE = "false";
  protected static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

  protected ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

  protected BaseReportGenerationService(RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  protected DownloadableReportResponse generateJasperReport(String reportJSON, JasperReport jasperReport, String schoolReportTypeCode){
    try{
      var params = getJasperParams();
      InputStream targetStream = new ByteArrayInputStream(reportJSON.getBytes());
      String tempDir = System.getProperty("java.io.tmpdir") + "/jasper-reports/";
      params.put("SUBREPORT_DIR", tempDir);
      params.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, targetStream);

      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);
      var downloadableReport = new DownloadableReportResponse();
      downloadableReport.setReportType(schoolReportTypeCode);
      downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(JasperExportManager.exportReportToPdf(jasperPrint)));
      return downloadableReport;
    } catch (JRException e) {
       log.error("Exception occurred while writing PDF report :: " + e.getMessage());
       throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report :: " + e.getMessage());
    }
  }

  protected District validateAndReturnDistrict(SchoolTombstone schoolTombstone){
    var district = restUtils.getDistrictByDistrictID(schoolTombstone.getDistrictId());
    if(district.isEmpty()){
      log.error("District could not be found while writing PDF report :: " + schoolTombstone.getDistrictId());
      throw new EntityNotFoundException(District.class, "District could not be found while writing PDF report :: ", schoolTombstone.getDistrictId());
    }

    return district.get();
  }

  protected IndependentAuthority validateAndReturnAuthority(SchoolTombstone schoolTombstone){
    var authority = restUtils.getAuthorityByAuthorityID(schoolTombstone.getIndependentAuthorityId());
    if(authority.isEmpty()){
      log.error("Authority could not be found while writing PDF report :: " + schoolTombstone.getDistrictId());
      throw new EntityNotFoundException(IndependentAuthority.class, "Authority could not be found while writing PDF report :: ", schoolTombstone.getIndependentAuthorityId());
    }

    return authority.get();
  }

  protected SchoolTombstone validateAndReturnSchool(UUID schoolID){
    var school = restUtils.getSchoolBySchoolID(schoolID.toString());
    if(school.isEmpty()){
      log.error("School could not be found while writing PDF report :: " + schoolID);
      throw new EntityNotFoundException(SchoolTombstone.class, "School could not be found while writing PDF report :: ", schoolID.toString());
    }

    return school.get();
  }
  

  protected Map<String, Object> getJasperParams(){
    Map<String, Object> params = new HashMap<>();
    params.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
    params.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
    params.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
    params.put(JRParameter.REPORT_LOCALE, Locale.US);
    return params;
  }
  
}
