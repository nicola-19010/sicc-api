package cl.sicc.siccapi.prescription.repository;

import cl.sicc.siccapi.consultation.domain.Consultation;
import cl.sicc.siccapi.prescription.domain.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Page<Prescription> findByConsultationAndDateBetween(Consultation consultation, LocalDate from, LocalDate to, Pageable pageable);
}
