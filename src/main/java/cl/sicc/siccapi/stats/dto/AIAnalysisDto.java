package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class AIAnalysisDto {
    private String analysisDate;
    private List<String> keyInsights;
    private Map<String, Double> trendPredictions;
    private List<String> riskFactors;
    private List<String> optimizationSuggestions;
    private Map<String, Integer> seasonalPatterns;
    private double accuracyScore;
}
