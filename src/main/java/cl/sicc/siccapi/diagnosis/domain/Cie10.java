package cl.sicc.siccapi.diagnosis.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cie10 {
    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false)
    private String name;
}
