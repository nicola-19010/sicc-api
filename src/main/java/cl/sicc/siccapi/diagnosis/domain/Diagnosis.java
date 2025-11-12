package cl.sicc.siccapi.diagnosis.domain;

import cl.sicc.siccapi.consultation.domain.Consultation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(indexes = {
        @Index(name = "idx_diagnosis_cie10", columnList = "cie10_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Consultation consultation;

    @ManyToOne(optional = false)
    private Cie10 cie10;

    private String description;
}
