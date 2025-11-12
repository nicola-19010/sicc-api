package cl.sicc.siccapi.prescription.infrastructure;

import cl.sicc.siccapi.prescription.domain.PharmaceuticalForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmaceuticalFormRepository extends JpaRepository<PharmaceuticalForm, Long> {
}

