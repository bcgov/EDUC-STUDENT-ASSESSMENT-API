package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class XAMFileService {
    private final AssessmentStudentRepository assessmentStudentRepository;
    //private final S3Client s3Client;

    @Value("${s3.bucket.name}")
    private String bucketName;

    public File generateXamFile(UUID sessionID, SchoolTombstone school) {
        List<AssessmentStudentEntity> students = assessmentStudentRepository.findByAssessmentEntity_SessionEntity_SessionIDAndSchoolID(sessionID, UUID.fromString(school.getSchoolId()));

        StringBuilder sb = new StringBuilder();

        for (AssessmentStudentEntity student : students) {
            String record =
                padRight("E07", 3) + // TX_ID
                padRight("", 1) + // VENDOR_ID - TODO being added to assessment student entity
                padRight("", 1) + // VERI_FLAG (BLANK)
                padRight("", 5) + // FILLER1 (BLANK)
                padRight(school.getMincode(), 8) + // MINCODE
                padRight(student.getLocalID(), 12) + // STUD_LOCAL_ID
                padRight(student.getPen(), 10) + // STUD_NO (PEN)
                padRight(student.getAssessmentEntity() != null ? student.getAssessmentEntity().getAssessmentTypeCode() : "", 5) + // CRSE_CODE
                padRight("", 3) + // CRSE_LEVEL (BLANK)
                padRight(student.getAssessmentEntity() != null && student.getAssessmentEntity().getAssessmentSessionEntity() != null ? student.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() : "", 4) + // CRSE_YEAR
                padRight(student.getAssessmentEntity() != null && student.getAssessmentEntity().getAssessmentSessionEntity() != null ? student.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth() : "", 2) + // CRSE_MONTH
                padRight("", 2) + // INTERIM_LETTER_GRADE (BLANK)
                padRight("", 3) + // INTERIM_SCHOOL_PERCENT (BLANK)
                padRight("", 3) + // FINAL_SCHOOL_PERCENT (BLANK)
                padRight("", 3) + // EXAM_PERCENT (BLANK)
                padRight(String.valueOf(student.getProficiencyScore()), 3) + // FINAL_PERCENT - PROFICIENCY_SCORE formatted as 001-004 TODO CHECK THE FORMAT
                padRight("", 2) + // FINAL_LETTER_GRADE (BLANK)
                padRight("Y", 1) + // E-EXAM FLAG - Always Y according to spec
                padRight(student.getProvincialSpecialCaseCode(), 1) + // PROV_SPEC_CASE
                padRight(student.getLocalCourseID(), 20) + // LOCAL_CRSE_ID
                padRight("A", 1) + // CRSE_STATUS - Always A according to spec
                padRight(student.getSurname(), 25) + // STUD_SURNAME
                padRight("", 2) + // NUM_CREDITS (BLANK)
                padRight("", 1) + // CRSE_TYPE (BLANK)
                padRight("", 1) + // TO_WRITE_FLAG (BLANK)
                "\n";
            sb.append(record);
        }

        String fileName = school.getMincode() + "_" + sessionID + ".xam";
        File file = new File("./" + fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        } catch (Exception e) {
            log.error("Failed to write XAM file", e);
            throw new RuntimeException("Failed to write XAM file", e);
        }

        return file;
    }

    public void uploadToS3(File file, String key) {
        try {
//            s3Client.putObject(PutObjectRequest.builder()
//                    .bucket(bucketName)
//                    .key(key)
//                    .build(), Path.of(file.getPath()));
            log.info("Uploaded file to S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * For Orchestrator
     * Generates a XAM file for the given school and session, returns the file path.
     */
    public String generateXamFileAndReturnPath(UUID sessionID, SchoolTombstone school) {
        File file = generateXamFile(sessionID, school);
        return file.getAbsolutePath();
    }

    /**
     * For Orchestrator
     * Uploads the file at the given path to S3 for the given school and session.
     */
    public void uploadFilePathToS3(String filePath, UUID sessionID, SchoolTombstone school) {
        File file = new File(filePath);
        String key = "xam-files/" + school.getMincode() + "_" + sessionID + ".xam";
        uploadToS3(file, key);
    }

    private String padRight(String value, int length) {
        String str = value == null ? "" : value;
        if (str.length() > length) {
            return str.substring(0, length);
        }
        return String.format("%-" + length + "s", str);
    }
}
