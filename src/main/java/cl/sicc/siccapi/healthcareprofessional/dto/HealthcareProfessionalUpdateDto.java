package cl.sicc.siccapi.healthcareprofessional.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class HealthcareProfessionalUpdateDto {
    @NotBlank
    private String name;

    private String specialty;
}

