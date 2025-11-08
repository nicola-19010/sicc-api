package cl.sicc.siccapi.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cl.sicc.siccapi.patient.domain.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> { }
