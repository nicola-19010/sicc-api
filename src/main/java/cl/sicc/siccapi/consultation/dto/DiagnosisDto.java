package cl.sicc.siccapi.consultation.dto;

import lombok.Data;

@Data
public class DiagnosisDto {
    private Long id;
    private String cie10Code;
    private String description;
}

