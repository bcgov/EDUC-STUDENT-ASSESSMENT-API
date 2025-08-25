package ca.bc.gov.educ.assessment.api.struct.v1.reports.doar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class ProficiencyLevel {
    private String level;
    private String numberCounted;
    private String studentsWithProficiency1;
    private String studentsWithProficiency2;
    private String studentsWithProficiency3;
    private String studentsWithProficiency4;
    private String notCounted;
}