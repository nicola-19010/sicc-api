package cl.sicc.siccapi.stats.dto;

public record PathologyDistributionDto(
    String patologia,
    Long cantidad,
    Double porcentaje
) {
}

