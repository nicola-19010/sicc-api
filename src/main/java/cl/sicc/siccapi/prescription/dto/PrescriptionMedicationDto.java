package cl.sicc.siccapi.prescription.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PrescriptionMedicationDto {
    private Long medicationId;
    private String medicationName;
    private Integer quantity;
    private String instructions;

    public PrescriptionMedicationDto() {}

}
