package cl.sicc.siccapi.prescription.domain;

import cl.sicc.siccapi.consultation.domain.Consultation;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @PastOrPresent(message = "La fecha de la receta no puede ser futura")
    private LocalDate date;

    @ManyToOne(optional = false)
    private Consultation consultation;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionMedication> prescriptionMedications;
}
