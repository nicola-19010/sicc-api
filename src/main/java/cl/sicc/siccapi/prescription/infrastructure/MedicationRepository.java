package cl.sicc.siccapi.prescription.infrastructure;

import cl.sicc.siccapi.prescription.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
}

