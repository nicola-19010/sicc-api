package cl.sicc.siccapi.healthcareprofessional.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cl.sicc.siccapi.healthcareprofessional.domain.HealthcareProfessional;

public interface HealthcareProfessionalRepository extends JpaRepository<HealthcareProfessional, Long> {
}
