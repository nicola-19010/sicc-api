package cl.sicc.siccapi.prescription.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class PrescriptionDto {
    private Long id;
    private LocalDate date;
    private Long consultationId;
    private List<PrescriptionMedicationDto> medications;

    public PrescriptionDto() {}

}
