package cl.sicc.siccapi.consultation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cl.sicc.siccapi.consultation.domain.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> { }
