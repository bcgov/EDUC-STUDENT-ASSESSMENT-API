package ca.bc.gov.educ.eas.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
//@Entity
@Builder
//@Table(name = "ASSESSMENT_KEY_FILE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentKeyFileEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "ASSESSMENT_KEY_FILE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentKeyFileID;

    @Column(name = "ASSESSMENT_ID", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentID;

    @Column(name = "FILE_NAME", nullable = false, length = 255)
    private String filename;

    @Column(name = "CREATE_USER", updatable = false, length = 100)
    private String createUser;

    @PastOrPresent
    @Column(name = "CREATE_DATE", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_USER", nullable = false, length = 100)
    private String updateUser;

    @PastOrPresent
    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "incomingFileset", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = AssessmentKeyEntity.class)
    Set<AssessmentKeyEntity> assessmentKeyEntities;

    public Set<AssessmentKeyEntity> getAssessmentKeyEntities() {
        if (this.assessmentKeyEntities == null) {
            this.assessmentKeyEntities = new HashSet<>();
        }
        return this.assessmentKeyEntities;
    }

}
