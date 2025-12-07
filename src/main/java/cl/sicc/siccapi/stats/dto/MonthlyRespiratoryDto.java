package cl.sicc.siccapi.stats.dto;

public record MonthlyRespiratoryDto(
    String month,
    Long bronquitis,
    Long resfriado,
    Long neumonia,
    Long faringitis,
    Long sinusitis,
    Long total
) {}

