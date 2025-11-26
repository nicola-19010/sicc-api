package cl.sicc.siccapi.stats.dto;

public record MonthlyCountDto(
    String month,
    long total
) {}
