package cl.sicc.siccapi.consultation.api.dto;

import lombok.Data;

@Data
public class DiagnosisDto {
    private Long id;
    private String cie10Code;
    private String description;
}

