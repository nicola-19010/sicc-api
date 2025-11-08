package cl.sicc.siccapi.healthcareprofessional.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthcareProfessionalDto {
    private Long id;
    private String rut;
    private String name;
    private String specialty;
}
