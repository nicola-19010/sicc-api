package cl.sicc.siccapi.patient.api.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Data
public class PatientCreateDto {
    @NotBlank
    private String rut;

    @NotBlank
    private String name;

    @NotNull
    @Past
    private LocalDate birthDate;

    private String sex;

    private String residentialSector;

    private String fonasaTier;
}

