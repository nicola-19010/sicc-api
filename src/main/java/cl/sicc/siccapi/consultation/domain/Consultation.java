package cl.sicc.siccapi.consultation.domain;

import cl.sicc.siccapi.patient.domain.Patient;
import cl.sicc.siccapi.healthcareprofessional.domain.HealthcareProfessional;
import cl.sicc.siccapi.diagnosis.domain.Diagnosis;
import cl.sicc.siccapi.prescription.domain.Prescription;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_consultation_patient_date", columnList = "patient_id, date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @PastOrPresent(message = "La fecha de la consulta no puede ser futura")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private HealthcareProfessional professional;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diagnosis> diagnoses;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prescription> prescriptions;

    public enum Type {
        GENERAL, EMERGENCY, FOLLOW_UP, OTHER
    }
}
