package cl.sicc.siccapi.stats.dto;

public record WeeklyTrendDto(
    String day,
    long consultations,
    int dayOfWeek
) {}
