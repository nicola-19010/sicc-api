package cl.sicc.siccapi.patient.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rut;

    @Column(nullable = false)
    private String name;

    private Integer age;

    @Column(length = 1)
    private String sex; // M or F

    private String residentialSector;

    @Enumerated(EnumType.STRING)
    private FonasaTier fonasaTier;

    public enum FonasaTier {
        A, B, C, D
    }
}
