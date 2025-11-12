package cl.sicc.siccapi.healthcareprofessional.dto;

import lombok.Data;

@Data
public class HealthcareProfessionalDto {
    private Long id;
    private String rut;
    private String name;
    private String specialty;
}

