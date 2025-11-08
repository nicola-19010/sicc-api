package cl.sicc.siccapi.healthcareprofessional.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cl.sicc.siccapi.healthcareprofessional.domain.Healthcareprofessional;

public interface HealthcareprofessionalRepository extends JpaRepository<Healthcareprofessional, Long> { }
