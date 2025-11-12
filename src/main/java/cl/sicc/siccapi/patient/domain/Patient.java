package cl.sicc.siccapi.patient.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.*;
import java.time.LocalDate;
import java.time.Period;

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

    @Column(nullable = false)
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate birthDate;

    @Column(length = 1)
    private String sex; // M or F

    private String residentialSector;

    @Enumerated(EnumType.STRING)
    private FonasaTier fonasaTier;

    @Transient
    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public enum FonasaTier {
        A, B, C, D
    }
}
