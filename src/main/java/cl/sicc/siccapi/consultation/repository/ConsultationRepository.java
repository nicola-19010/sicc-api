package cl.sicc.siccapi.consultation.repository;

import cl.sicc.siccapi.consultation.domain.Consultation;
import cl.sicc.siccapi.patient.domain.Patient;
import cl.sicc.siccapi.healthcareprofessional.domain.HealthcareProfessional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Page<Consultation> findByPatientAndDateBetween(Patient patient, LocalDate from, LocalDate to, Pageable pageable);
    Page<Consultation> findByProfessionalAndDateBetween(HealthcareProfessional professional, LocalDate from, LocalDate to, Pageable pageable);
}
