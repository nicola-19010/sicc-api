package cl.sicc.siccapi.stats.dto;

public record ConsultationTypeStatsDto(
    String type,
    Long count,
    Double percentage
) {}

