package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class DemandPredictionDto {
    private int predictedConsultationsTomorrow;
    private int predictedConsultationsWeek;
    private Map<String, Integer> predictionsByDay;
    private List<String> highDemandDays;
    private List<String> recommendations;
    private double confidenceLevel;
}
