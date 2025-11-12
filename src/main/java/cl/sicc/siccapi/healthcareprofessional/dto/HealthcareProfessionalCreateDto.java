package cl.sicc.siccapi.healthcareprofessional.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class HealthcareProfessionalCreateDto {
    @NotBlank
    private String rut;

    @NotBlank
    private String name;

    private String specialty;
}

