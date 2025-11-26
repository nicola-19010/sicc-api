package cl.sicc.siccapi.stats.dto;

public record DiagnosisCountDto(
    String diagnosis,
    long count
) {}
