package cl.sicc.siccapi.stats.dto;

import java.util.List;

public record RespiratoryTrendDto(
    Long totalCases,
    Double variationPercentage,
    List<MonthlyRespiratoryDto> monthlyTrend,
    List<RespiratoryDiseaseDto> byDisease
) {}

