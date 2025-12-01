package cl.sicc.siccapi.prescription.repository;

import cl.sicc.siccapi.consultation.domain.Consultation;
import cl.sicc.siccapi.prescription.domain.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Page<Prescription> findByConsultationAndDateBetween(Consultation consultation, LocalDate from, LocalDate to, Pageable pageable);

    @Query("SELECT m.name, COUNT(pm) FROM PrescriptionMedication pm JOIN pm.medication m GROUP BY m.name ORDER BY COUNT(pm) DESC")
    List<Object[]> findTopMedicamentos(PageRequest pageable);

    // Consultas de verificación de datos
    @Query("SELECT COUNT(m) FROM Medication m")
    Long countTotalMedicamentos();

    @Query("SELECT COUNT(pm) FROM PrescriptionMedication pm")
    Long countTotalPrescriptionMedications();
}
