package cl.sicc.siccapi.healthcareprofessional.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(indexes = @Index(name = "idx_professional_rut", columnList = "rut"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthcareProfessional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rut;

    @Column(nullable = false)
    private String name;

    private String specialty;
}
