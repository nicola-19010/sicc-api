package cl.sicc.siccapi.prescription.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MedicationDto {
    private Long id;
    private String name;
    private String dosage;
    private Long pharmaceuticalFormId;
    private String pharmaceuticalFormName;

    public MedicationDto() {}

}
