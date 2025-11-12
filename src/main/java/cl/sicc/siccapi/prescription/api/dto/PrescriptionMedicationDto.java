package cl.sicc.siccapi.prescription.api.dto;

import lombok.Data;

@Data
public class PrescriptionMedicationDto {
    private Long medicationId;
    private String medicationName;
    private Integer quantity;
    private String instructions;
}

