package cl.sicc.siccapi.patient.api.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientDto {
    private Long id;
    private String rut;
    private String name;
    private LocalDate birthDate;
    private Integer age;
    private String sex;
    private String residentialSector;
    private String fonasaTier;
}

