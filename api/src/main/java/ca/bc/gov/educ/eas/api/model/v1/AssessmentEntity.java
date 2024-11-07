package ca.bc.gov.educ.eas.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Builder
@Table(name = "ASSESSMENT")
public class AssessmentEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "ASSESSMENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = SessionEntity.class)
    @JoinColumn(name = "SESSION_ID", referencedColumnName = "SESSION_ID", updatable = false)
    SessionEntity sessionEntity;

    @Column(name = "ASSESSMENT_TYPE_CODE", nullable = false, length = 10)
    private String assessmentTypeCode;

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
    @OneToMany(mappedBy = "assessmentEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = AssessmentStudentEntity.class)
    Set<AssessmentStudentEntity> students;
}
