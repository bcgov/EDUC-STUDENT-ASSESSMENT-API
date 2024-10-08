package ca.bc.gov.educ.eas.api.repository.v1;

import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {

}
