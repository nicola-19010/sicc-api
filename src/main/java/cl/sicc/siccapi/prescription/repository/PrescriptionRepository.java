package cl.sicc.siccapi.prescription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cl.sicc.siccapi.prescription.domain.Prescription;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> { }
