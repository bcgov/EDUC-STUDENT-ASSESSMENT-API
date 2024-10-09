package ca.bc.gov.educ.eas.api.repository.v1;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {

}
