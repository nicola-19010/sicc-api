package cl.sicc.siccapi.diagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cl.sicc.siccapi.diagnosis.domain.Diagnosis;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> { }
