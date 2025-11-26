package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EmergingDiagnosisDto {
    private String diagnosis;
    private Long currentMonth;
    private Long previousMonth;
    private Double variation;
}
