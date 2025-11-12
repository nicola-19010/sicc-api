package cl.sicc.siccapi.consultation.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ConsultationDto {
    private Long id;
    private LocalDate date;
    private String type;
    private Long patientId;
    private Long professionalId;
    private List<DiagnosisDto> diagnoses;
}

