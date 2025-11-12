package cl.sicc.siccapi.healthcareprofessional.repository;

import cl.sicc.siccapi.healthcareprofessional.domain.HealthcareProfessional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HealthcareProfessionalRepository extends JpaRepository<HealthcareProfessional, Long> {
    Optional<HealthcareProfessional> findByRut(String rut);
}
