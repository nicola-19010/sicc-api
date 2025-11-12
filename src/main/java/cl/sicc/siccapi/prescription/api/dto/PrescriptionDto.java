package cl.sicc.siccapi.prescription.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PrescriptionDto {
    private Long id;
    private LocalDate date;
    private Long consultationId;
    private List<PrescriptionMedicationDto> medications;
}

