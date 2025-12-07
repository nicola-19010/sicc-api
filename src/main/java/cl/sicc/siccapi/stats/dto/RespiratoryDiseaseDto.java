package cl.sicc.siccapi.stats.dto;

public record RespiratoryDiseaseDto(
    String disease,
    Long currentCount,
    Long previousCount,
    Double variation,
    String status
) {}

