package cl.sicc.siccapi.diagnosis.repository;

import cl.sicc.siccapi.diagnosis.domain.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    List<Diagnosis> findByConsultationId(Long consultationId);

    @Query("SELECT d FROM Diagnosis d WHERE d.cie10.code = :code")
    List<Diagnosis> findByCie10Code(@Param("code") String code);
}