package cl.sicc.siccapi.consultation.dto;

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

    // Enriched fields for Frontend
    private String patientName;
    private String patientRut;
    private Integer patientAge;
    private String patientSex;
    private String fonasaType;
    private String doctorName;
    private String specialtyName;
    private String diagnosis; // Primary diagnosis description
}
