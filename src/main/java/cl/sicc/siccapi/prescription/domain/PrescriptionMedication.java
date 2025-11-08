package cl.sicc.siccapi.prescription.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionMedication {

    @EmbeddedId
    private PrescriptionMedicationId id;

    @ManyToOne
    @MapsId("prescriptionId")
    private Prescription prescription;

    @ManyToOne
    @MapsId("medicationId")
    private Medication medication;

    @Column(nullable = false)
    private Integer quantity;

    private String instructions;
}

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class PrescriptionMedicationId implements java.io.Serializable {
    private Long prescriptionId;
    private Long medicationId;
}
