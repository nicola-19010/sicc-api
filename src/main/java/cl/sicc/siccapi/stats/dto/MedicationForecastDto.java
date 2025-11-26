package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MedicationForecastDto {
    private String medication;
    private Long currentStock;
    private Long predictedDemand;
    private Long recommendedStock;
    private Double growthRate;
}
