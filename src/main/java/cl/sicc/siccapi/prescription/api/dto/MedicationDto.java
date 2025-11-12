package cl.sicc.siccapi.prescription.api.dto;

import lombok.Data;

@Data
public class MedicationDto {
    private Long id;
    private String name;
    private String dosage;
    private String pharmaceuticalFormName;
}

