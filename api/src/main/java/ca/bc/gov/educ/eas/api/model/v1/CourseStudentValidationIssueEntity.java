package ca.bc.gov.educ.eas.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "COURSE_STUDENT_VALIDATION_ISSUE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseStudentValidationIssueEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
    @Column(name = "COURSE_STUDENT_VALIDATION_ISSUE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    UUID courseStudentValidationIssueID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = CourseStudentEntity.class)
    @JoinColumn(name = "COURSE_STUDENT_ID", referencedColumnName = "COURSE_STUDENT_ID", updatable = false)
    CourseStudentEntity courseStudent;

    @Column(name = "VALIDATION_ISSUE_SEVERITY_CODE", nullable = false)
    private String validationIssueSeverityCode;

    @Column(name = "VALIDATION_ISSUE_CODE", nullable = false)
    private String validationIssueCode;

    @Column(name = "VALIDATION_ISSUE_FIELD_CODE", nullable = false)
    private String validationIssueFieldCode;

    @Column(name = "CREATE_USER", updatable = false , length = 32)
    private String createUser;

    @PastOrPresent
    @Column(name = "CREATE_DATE", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_USER", length = 32)
    private String updateUser;

    @PastOrPresent
    @Column(name = "UPDATE_DATE")
    private LocalDateTime updateDate;

}
