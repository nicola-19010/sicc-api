package cl.sicc.siccapi.diagnosis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisDto {
    private Long id;

    @NotNull(message = "Consultation ID is required")
    private Long consultationId;

    @NotNull(message = "CIE-10 ID is required")
    private Long cie10Id;

    private String cie10Code;
    private String cie10Description;
    private String description;
}